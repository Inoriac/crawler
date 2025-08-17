package com.pixiv.crawler.service;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.util.JsonUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtistService {
    public List<PixivImage> searchArtworksByArtistId(String artistId, int maxImages) throws Exception{
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", GlobalConfig.PORT));

        String url = "https://www.pixiv.net/ajax/user/" + artistId + "/profile/top?sensitiveFilterMode=userSetting&lang=zh";
        
        // 创建HTTP客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .addHeader("Cookie", GlobalConfig.COOKIE)
                .addHeader("Referer", "https://www.pixiv.net/")
                .addHeader("Accept", "application/json, text/plain, */*")
                .build();

        // 发送请求并获取响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("请求失败，状态码: " + response.code());
            }

            String responseText = response.body().string();
            if (responseText == null || responseText.isEmpty()) {
                throw new RuntimeException("响应内容为空");
            }

            // 解析JSON响应
            return parseArtistWorksFromJson(responseText, maxImages);
        }
    }
    
    /**
     * 从JSON响应中解析画师作品信息
     * 画师作品的JSON结构与推荐作品不同，illusts是一个对象而不是数组
     * @param jsonResponse JSON响应字符串
     * @param maxImages 最大图片数量
     * @return 画师作品列表
     */
    private List<PixivImage> parseArtistWorksFromJson(String jsonResponse, int maxImages) {
        List<PixivImage> artistWorks = new ArrayList<>();

        try {
            System.out.println("【画师作品】开始解析JSON响应，长度: " + jsonResponse.length());
            
            // 调试：输出JSON的前500个字符
            String preview = jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse;
            System.out.println("【画师作品】JSON预览: " + preview);

            // 查找illusts对象的开始位置
            int illustsStart = jsonResponse.indexOf("\"illusts\":{");
            if (illustsStart == -1) {
                System.out.println("【画师作品】未找到illusts对象");
                // 尝试查找其他可能的格式
                int illustsArrayStart = jsonResponse.indexOf("\"illusts\":[");
                if (illustsArrayStart != -1) {
                    System.out.println("【画师作品】找到illusts数组，使用推荐作品的解析方法");
                    return JsonUtil.parseRecommendImagesFromJson(jsonResponse, maxImages);
                }
                return artistWorks;
            }

            // 找到illusts对象的结束位置
            int bracketCount = 0;
            int illustsEnd = illustsStart;
            boolean inObject = false;

            for (int i = illustsStart; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                if (c == '{' && !inObject) {
                    inObject = true;
                    bracketCount = 1;
                } else if (c == '{' && inObject) {
                    bracketCount++;
                } else if (c == '}' && inObject) {
                    bracketCount--;
                    if (bracketCount == 0) {
                        illustsEnd = i + 1;
                        break;
                    }
                }
            }

            if (illustsEnd <= illustsStart) {
                System.out.println("【画师作品】无法确定illusts对象的结束位置");
                return artistWorks;
            }

            // 提取illusts对象内容
            String illustsContent = jsonResponse.substring(illustsStart + 10, illustsEnd - 1);
            System.out.println("【画师作品】illusts对象长度: " + illustsContent.length());
            
            // 调试：输出illusts内容的前300个字符
            String illustsPreview = illustsContent.length() > 300 ? illustsContent.substring(0, 300) + "..." : illustsContent;
            System.out.println("【画师作品】illusts内容预览: " + illustsPreview);

            // 使用专门的方法分割画师作品对象（对象格式，不是数组格式）
            List<String> illustObjects = splitArtistIllustObjects(illustsContent);
            System.out.println("【画师作品】找到 " + illustObjects.size() + " 个作品对象");
            
            // 调试：如果有作品对象，输出第一个对象的前200个字符
            if (!illustObjects.isEmpty()) {
                String firstObjPreview = illustObjects.get(0).length() > 200 ? 
                    illustObjects.get(0).substring(0, 200) + "..." : illustObjects.get(0);
                System.out.println("【画师作品】第一个作品对象预览: " + firstObjPreview);
            }

            int count = 0;
            for (String illustObj : illustObjects) {
                if (count >= maxImages) break;

                // 使用JsonUtil中的方法解析单个作品对象
                PixivImage image = JsonUtil.parseIllustObject(illustObj);
                if (image != null) {
                    artistWorks.add(image);
                    count++;
                    System.out.println("【画师作品】解析作品: " + image.getId() + " - " + image.getTitle());
                } else {
                    System.out.println("【画师作品】解析作品失败，对象内容: " + 
                        (illustObj.length() > 100 ? illustObj.substring(0, 100) + "..." : illustObj));
                }
            }

            System.out.println("【画师作品】JSON解析完成，共找到 " + artistWorks.size() + " 个作品");

        } catch (Exception e) {
            System.out.println("【画师作品】解析JSON失败: " + e.getMessage());
            e.printStackTrace();
        }

        return artistWorks;
    }

    /**
     * 分割画师作品对象中的作品对象
     * 画师作品的illusts是对象格式：{"133850927": {...}, "133703386": {...}}
     * 而不是数组格式：[{...}, {...}]
     */
    private List<String> splitArtistIllustObjects(String illustsContent) {
        List<String> objects = new ArrayList<>();
        
        // 使用正则表达式匹配作品对象
        Pattern pattern = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{");
        Matcher matcher = pattern.matcher(illustsContent);

        while (matcher.find()) {
            int objectStart = matcher.end() - 1; // 回到 { 的位置
            
            // 找到这个对象的结束位置
            int bracketCount = 1;
            int objectEnd = objectStart;
            
            for (int i = objectStart + 1; i < illustsContent.length(); i++) {
                char c = illustsContent.charAt(i);
                if (c == '{') {
                    bracketCount++;
                } else if (c == '}') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        objectEnd = i + 1;
                        break;
                    }
                }
            }
            
            if (objectEnd > objectStart) {
                String objectContent = illustsContent.substring(objectStart, objectEnd);
                objects.add(objectContent);
                System.out.println("【画师作品】提取作品对象，ID: " + matcher.group(1) + ", 长度: " + objectContent.length());
            }
        }
        
        return objects;
    }

    // 获取画师名字，用于创建文件夹
    public String getArtistName(String artistId) throws Exception{
        String userPageUrl = "https://www.pixiv.net/ajax/user/" + artistId + "?full=1&lang=zh";

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", GlobalConfig.PORT));

        // 创建HTTP客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url(userPageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .addHeader("Cookie", GlobalConfig.COOKIE)
                .addHeader("Referer", "https://www.pixiv.net/")
                .addHeader("Accept", "application/json, text/plain, */*")
                .build();

        // 发送请求并获取响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("请求失败，状态码: " + response.code());
            }

            String responseText = response.body().string();
            if (responseText == null || responseText.isEmpty()) {
                throw new RuntimeException("响应内容为空");
            }

            // 解析响应获取artistName
            return parseArtistNameFromJson(responseText);
        }
    }
    
    /**
     * 从JSON响应中解析画师名字
     * @param jsonResponse JSON响应字符串
     * @return 画师名字
     */
    private String parseArtistNameFromJson(String jsonResponse) {
        try {
            System.out.println("【画师信息】开始解析画师信息JSON，长度: " + jsonResponse.length());
            
            // 查找body.name字段
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(jsonResponse);
            
            if (nameMatcher.find()) {
                String artistName = nameMatcher.group(1);
                System.out.println("【画师信息】成功获取画师名字: " + artistName);
                return artistName;
            } else {
                System.out.println("【画师信息】未找到画师名字字段");
                return "unknown_artist";
            }
            
        } catch (Exception e) {
            System.out.println("【画师信息】解析画师名字失败: " + e.getMessage());
            e.printStackTrace();
            return "unknown_artist";
        }
    }
}
