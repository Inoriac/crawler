package com.pixiv.crawler.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.TagInfo;
import com.pixiv.crawler.service.TagService;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TagServiceImpl implements TagService {
    private OkHttpClient client;
    private ObjectMapper mapper;

    public TagServiceImpl(){
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map<String, Double> getTags(File imageFile) throws IOException {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

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
            return tags;
        }
    }

    @Override
    public void processImage(File imageFile, Map<String, TagInfo> tagMap) throws IOException {
        Map<String, Double> tags = getTags(imageFile);

        for(Map.Entry<String, Double> entry : tags.entrySet()) {
            String tag = entry.getKey();
            double prob = entry.getValue();
            if(prob < GlobalConfig.TAG_PROBILITY) continue;

            // 进行 tagMap 中概率值的更新
            tagMap.compute(tag, (k, v)-> {
                if(v == null) return new TagInfo(prob);
                v.update(prob);
                return v;
            });

            // 删除最终结果中平均概率小于阈值的 tag
            tagMap.entrySet().removeIf(e -> e.getValue().getAvgProbability() < GlobalConfig.TAG_FINAL_PROB);
        }
    }

    @Override
    public void processImages(List<File> imageFiles, Map<String, TagInfo> tagMap)  throws IOException{
        for(File imageFile : imageFiles){
            processImage(imageFile, tagMap);
        }
    }

    @Override
    public void saveToJson(Map<String, TagInfo> tagMap) throws IOException {
        // 方便以后扩展，此处的处理是用于控制输出的 json 结构，此处暂时不使用，节省内存开销
//        Map<String, Map<String, Object>> output = new HashMap<>();
//        for(Map.Entry<String, TagInfo> entry : tagMap.entrySet()) {
//            Map<String, Object> info = new HashMap<>();
//            info.put("argProbability", entry.getValue().getAvgProbability());
//            info.put("count", entry.getValue().getCount());
//            output.put(entry.getKey(), info);
//        }
//        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), output);

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
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, tagMap);
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
    public void clearJson() {
        File dir = new File(".");
        File[] backups = dir.listFiles((d, name) -> name.startsWith("tags_backup_") && name.endsWith(".json"));

        if(backups == null || backups.length <= GlobalConfig.TAG_SERVICE_JSON_FILE_MAX_NUMBER) return;

        // 按最后修改时间排序
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

        int filesToDelete = backups.length - GlobalConfig.TAG_SERVICE_JSON_FILE_MAX_NUMBER;
        for (int i = 0; i < filesToDelete; i++){
            if(backups[i].delete()) {
                System.out.println("【TagService】已删除旧备份：" + backups[i].getName());
            } else {
                System.out.println("【TagService】删除失败：" + backups[i].getName());
            }
        }
    }

    @Override
    public double calculateTagSimilarity(List<String> tags, Map<String, TagInfo> tagMap) {
        int tag_number = 0;             // 符合偏好的tag数量
        double total_probability = 0;   // 总概率值

        for(String tag : tags){
            if(!tagMap.containsKey(tag)){
                continue;
            }
            // 获取对应tag的信息
            TagInfo tagInfo = tagMap.get(tag);
            tag_number += tagInfo.getCount();
            total_probability += tagInfo.getAvgProbability() * tagInfo.getCount();
        }

        // 加权平均
        return (total_probability/tag_number);
    }
}
