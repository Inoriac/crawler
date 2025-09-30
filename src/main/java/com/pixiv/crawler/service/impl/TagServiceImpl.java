package com.pixiv.crawler.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.CharacterTagHolder;
import com.pixiv.crawler.model.TagInfo;
import com.pixiv.crawler.model.TagMapHolder;
import com.pixiv.crawler.service.TagService;
import com.pixiv.crawler.util.JsonUtil;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TagServiceImpl implements TagService {
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private static Map<String, List<String>> localMapping;

    public TagServiceImpl(){
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Double> getTags(File imageFile) throws IOException {
        // 根据文件扩展名确定正确的MediaType
        String fileName = imageFile.getName().toLowerCase();
        String mediaType;
        if (fileName.endsWith(".png")) {
            mediaType = "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            mediaType = "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            mediaType = "image/gif";
        } else if (fileName.endsWith(".webp")) {
            mediaType = "image/webp";
        } else {
            // 默认使用jpeg，但记录警告
            mediaType = "image/jpeg";
            System.out.println("【TagService】警告：未知图片格式 " + fileName + "，使用默认jpeg格式");
        }
        
        RequestBody fileBody = RequestBody.create(
                imageFile,
                MediaType.parse(mediaType)
        );

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(), fileBody)
                .build();

        // 添加调试信息
        System.out.println("【TagService】上传图片: " + JsonUtil.unescapeUnicodePublic(imageFile.getName()) +
                          ", 大小: " + imageFile.length() + " bytes, " +
                          "MediaType: " + mediaType);

        // 创建请求
        Request request = new Request.Builder()
                .url(GlobalConfig.TAG_SERVICE_URL_LOCAL)
                .post(body)
                .build();

        // 尝试发送请求
        try (Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            Map<String, Object> res = mapper.readValue(json, Map.class);
            Map<String, Double> tags = (Map<String, Double>) res.get("tags");

            // 映射结果集
            Map<String, Double> mappedTags = new HashMap<>();
            Map<String, Integer> tagNums = new HashMap<>();

            for(Map.Entry<String, Double> entry : tags.entrySet()){
                String originalTag = toLocalMappingKey(entry.getKey()); // key 形式归一化
                Double score = entry.getValue();

                // 查找映射
                List<String> mappedList = findFromLocalMapping(originalTag);
                if(mappedList.isEmpty()) {
                    // 本地未命中，调用API获取
                    mappedList = getSimilarTagByApi(originalTag);
                    Thread.sleep(100);
                }

                if(!mappedList.isEmpty()) {
                    for(String mappedTag : mappedList) {
                        mappedTags.merge(mappedTag, score, Double::sum);
                        // 记录次数
                        if(tagNums.containsKey(mappedTag)) {
                            // 不存在则加入map中
                            tagNums.put(mappedTag, 1);
                        } else {
                            tagNums.merge(mappedTag, 1, Integer::sum);
                        }
                    }
                }
            }

            // 概率修正
            for(Map.Entry<String, Double> entry : mappedTags.entrySet()) {
                String originalTag = entry.getKey();
                Double score = entry.getValue();

                // 计算概率平均值
                mappedTags.put(originalTag, (score/ tagNums.get(originalTag)));
            }

            return mappedTags;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processImage(File imageFile) throws IOException {
        Map<String, Double> tags = getTags(imageFile);
        Map<String, TagInfo> tagMap = TagMapHolder.getInstance().getTagMap();

        for(Map.Entry<String, Double> entry : tags.entrySet()) {
            String tag = entry.getKey();
            double prob = entry.getValue();
            if(prob < GlobalConfig.TAG_PROBABILITY) continue;

            // 进行 tagMap 中概率值的更新
            tagMap.compute(tag, (k, v)-> {
                if(v == null) return new TagInfo(prob);
                v.update(prob);
                return v;
            });
        }
        // 删除最终结果中平均概率小于阈值的 tag
        tagMap.entrySet().removeIf(e -> e.getValue().getAvgProbability() < GlobalConfig.TAG_FINAL_PROB);
    }

    @Override
    public List<String> getPreferCharacterTags() throws IOException{
        // 尝试打开 filename 文件
        File file = new File(GlobalConfig.TAG_SERVICE_JSON_NAME_CHARACTER);
        if(!file.exists()) {
            System.out.println("【TagService】未找到用户偏好角色tag信息");
            return new ArrayList<>();
        }
        return mapper.readValue(file, new TypeReference<List<String>>() {});
    }

    @Override
    public void saveCharacterTagToJson(List<String> characters) {
        File file = new File(GlobalConfig.TAG_SERVICE_JSON_NAME_CHARACTER);

        // 如果旧文件存在，进行备份
        if(file.exists()){
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFile = new File("character_tags_backup_" + timestamp);
            if(file.renameTo(backupFile)){
                System.out.println("【TagService】旧角色偏好已备份：" + backupFile.getName());
            } else {
                System.out.println("【TagService】备份失败，仍继续使用新文件");
            }
        }

        try{
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, characters);
            System.out.println("【TagService】新文件已保存：" + file.getName());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void saveToJson(){
        File file = new File(GlobalConfig.TAG_SERVICE_JSON_NAME);

        // 如果旧文件存在，进行备份
        if(file.exists()){
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFile = new File("tags_backup_" + timestamp);
            if(file.renameTo(backupFile)){
                System.out.println("【TagService】旧文件已备份：" + backupFile.getName());
            } else {
                System.out.println("【TagService】备份失败，仍继续使用新文件");
            }
        }

        try{
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, TagMapHolder.getInstance().getTagMap());
            System.out.println("【TagService】新文件已保存：" + file.getName());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, TagInfo> loadFromJson() throws IOException {
        // 尝试打开 filename 文件
        File file = new File(GlobalConfig.TAG_SERVICE_JSON_NAME);
        Map<String, TagInfo> tagMap = new HashMap<>();
        if(!file.exists()) {
            System.out.println("【TagService】未找到用户偏好tag信息");
            return tagMap;
        }

        Map<String, Map<String, Object>> input = mapper.readValue(file, Map.class);
        for(Map.Entry<String, Map<String, Object>> entry : input.entrySet()){
            double avgProb = ((Number) entry.getValue().get("avgProbability")).doubleValue();
            int count = ((Number) entry.getValue().get("count")).intValue();
            TagInfo info = new TagInfo(avgProb, count);

            tagMap.put(entry.getKey(), info);
        }

        return tagMap;
    }

    @Override
    public double calculateTagSimilarity(List<String> tags) {
        Map<String, TagInfo> tagMap = TagMapHolder.getInstance().getTagMap();
        List<String> characterTags = CharacterTagHolder.getInstance().getCharacterTags();

        // 若没有用户偏好，则默认全取
        if(tagMap.isEmpty()) return 1;

        int tagNumber = 0;             // 符合偏好的tag数量
        double weightedSum = 0;   // 总概率值

        // 计算匹配度
        for(String tag : tags){
            // 没有匹配词条偏好，则进行惩罚
            if(!tagMap.containsKey(tag)) {
                weightedSum -= GlobalConfig.PUNISHMENT;
                continue;
            }
            // 匹配角色偏好，则进行奖励
            if(!characterTags.isEmpty() && characterTags.contains(tag)) {
                weightedSum += GlobalConfig.REWARD;
            }

            // 获取对应tag的信息
            TagInfo tagInfo = tagMap.get(tag);
            tagNumber += tagInfo.getCount();
            weightedSum += tagInfo.getAvgProbability() * tagInfo.getCount();
        }

        if (tagNumber == 0) return 0.0; // 没有匹配的tag

        // 加权平均
        double avg = weightedSum/tagNumber;
        // 平方：强化高分
        return Math.min(0.99, Math.pow(avg, 2));
    }

    @Override
    public List<String> getSimilarTagByApi(String tag) throws IOException{
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(GlobalConfig.HOST, GlobalConfig.PORT));

        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url(GlobalConfig.SEARCH_API_PRE + tag + GlobalConfig.SEARCH_API_SUF)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .addHeader("Cookie", GlobalConfig.COOKIE)
                .addHeader("Referer", "https://www.pixiv.net/")
                .addHeader("Accept", "application/json, text/plain, */*")
                .build();

        // 发送请求并获取响应
        try (Response response = client.newCall(request).execute()) {
            if(!response.isSuccessful()){
                throw new RuntimeException("请求失败，状态码：" + response.code());
            }

            String responseText = response.body().string();
            if(responseText == null || responseText.isEmpty()){
                throw new RuntimeException("响应内容为空");
            }
            System.out.println(responseText);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseText);
            JsonNode candidates = root.get("candidates");

            List<String> tags = new ArrayList<>();
            if(candidates != null && candidates.isArray()) {
                for (JsonNode node : candidates) {
                    // 寻找目标 tag_translation 字段, 获取对应的 tag_name
                    JsonNode translationNode = node.get("tag_translation");
                    JsonNode tagNameNode = node.get("tag_name");
                    // 匹配 tag_name 或者是 translation
                    if(translationNode != null && (tag.equalsIgnoreCase(translationNode.asText()) || tag.equalsIgnoreCase(tagNameNode.asText()))) {
                        tags.add(tagNameNode.asText());
                    }
                }
            }

            return tags;
        }
    }

    @Override
    public List<String> findFromLocalMapping(String tag) {
        loadMapping();
        return localMapping.getOrDefault(tag, Collections.emptyList());
    }

    // 将 tag 中的_换成空格
    private String toLocalMappingKey(String tag){
        if(tag == null) return null;

        return tag.replace("_", " ");
    }

    // 懒加载，第一次调用时加载 mapping
    private void loadMapping(){
        if (localMapping != null) return;   // 已加载 跳过
        synchronized (TagServiceImpl.class) {
            try {
                InputStream inputStream = TagServiceImpl.class.getResourceAsStream(GlobalConfig.LOCAL_MAPPING_URL);
                if(inputStream != null) {
                    localMapping = mapper.readValue(inputStream, new TypeReference<Map<String, List<String>>>() {});
                    System.out.println("【TagService】成功载入mapping.json");
                } else {
                    System.out.println("【TagService】mapping.json路径配置有误");
                }
            } catch (Exception e) {
                System.out.println("【TagService】" + e.getMessage());
                localMapping = Collections.emptyMap();
            }
        }
    }
}
