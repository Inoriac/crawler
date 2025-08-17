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

// TODO：若查询到的数组为空，可能意味着搜索tag有误或者是该tag下作品较少，前者直接返回相关tag，后者需要提供一个全新的方法
public class PopularImageService {
    public List<PixivImage> getPopularImagesByTag(String tag) throws Exception{
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(GlobalConfig.HOST, GlobalConfig.PORT));

        // TODO：目前尚未进行禁止显示AI作品的ajax请求参数适配(可能有所区别)
        String searchUrl = "https://www.pixiv.net/ajax/search/artworks/" + tag + "?word=sea&order=date_d&mode=all&p=1&csw=0&s_mode=s_tag&type=all&lang=zh";

        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url(searchUrl)
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

            return parsePopularWorksFromJson(responseText);
        }
    }

    /**
     * 从JSON响应中解析热门作品信息
     * @param jsonResponse JSON响应字符串
     * @return 热门作品列表
     */
    private List<PixivImage> parsePopularWorksFromJson(String jsonResponse) {
        List<PixivImage> popularImages = new ArrayList<>();

        try {
            System.out.println("【热门作品】开始解析JSON响应，长度: " + jsonResponse.length());

            // 查找popular对象的开始位置
            int popularStart = jsonResponse.indexOf("\"popular\":{");
            if (popularStart == -1) {
                System.out.println("【热门作品】未找到popular对象");
                return popularImages;
            }

            // 找到popular对象的结束位置
            int bracketCount = 0;
            int popularEnd = popularStart;
            boolean inObject = false;

            for (int i = popularStart; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                if (c == '{' && !inObject) {
                    inObject = true;
                    bracketCount = 1;
                } else if (c == '{' && inObject) {
                    bracketCount++;
                } else if (c == '}' && inObject) {
                    bracketCount--;
                    if (bracketCount == 0) {
                        popularEnd = i + 1;
                        break;
                    }
                }
            }

            if (popularEnd <= popularStart) {
                System.out.println("【热门作品】无法确定popular对象的结束位置");
                return popularImages;
            }

            // 提取popular对象内容
            String popularContent = jsonResponse.substring(popularStart + 10, popularEnd - 1);
            System.out.println("【热门作品】popular对象长度: " + popularContent.length());
            
            // 调试：输出popular对象的前500个字符
            String popularPreview = popularContent.length() > 500 ? popularContent.substring(0, 500) + "..." : popularContent;
            System.out.println("【热门作品】popular对象预览: " + popularPreview);
            
            // 调试：检查是否包含recent和permanent字段
            boolean hasRecent = popularContent.contains("\"recent\":");
            boolean hasPermanent = popularContent.contains("\"permanent\":");
            System.out.println("【热门作品】包含recent字段: " + hasRecent);
            System.out.println("【热门作品】包含permanent字段: " + hasPermanent);

            // 解析recent数组
            List<PixivImage> recentImages = parsePopularArray(popularContent, "recent");
            popularImages.addAll(recentImages);

            // 解析permanent数组
            List<PixivImage> permanentImages = parsePopularArray(popularContent, "permanent");
            popularImages.addAll(permanentImages);

            System.out.println("【热门作品】JSON解析完成，共找到 " + popularImages.size() + " 个热门作品");

        } catch (Exception e) {
            System.out.println("【热门作品】解析JSON失败: " + e.getMessage());
            e.printStackTrace();
        }

        return popularImages;
    }

    /**
     * 解析popular对象中的数组（recent或permanent）
     * @param popularContent popular对象内容
     * @param arrayName 数组名称（"recent"或"permanent"）
     * @return 作品列表
     */
    private List<PixivImage> parsePopularArray(String popularContent, String arrayName) {
        List<PixivImage> images = new ArrayList<>();

        try {
            // 查找数组的开始位置 - 修复正则表达式
            String arrayStartPattern = "\"" + arrayName + "\":\\[";
            int arrayStart = popularContent.indexOf(arrayStartPattern);
            if (arrayStart == -1) {
                System.out.println("【热门作品】未找到" + arrayName + "数组，尝试其他方式查找");
                // 尝试更宽松的查找方式
                arrayStart = popularContent.indexOf("\"" + arrayName + "\":");
                if (arrayStart == -1) {
                    System.out.println("【热门作品】完全未找到" + arrayName + "字段");
                    return images;
                }
                // 找到冒号后的第一个 [ 符号
                int bracketStart = popularContent.indexOf("[", arrayStart);
                if (bracketStart == -1) {
                    System.out.println("【热门作品】未找到" + arrayName + "数组的开始括号");
                    return images;
                }
                arrayStart = bracketStart;
            } else {
                // 如果找到了完整模式，调整到 [ 的位置
                arrayStart = popularContent.indexOf("[", arrayStart);
            }

            // 找到数组的结束位置
            int bracketCount = 0;
            int arrayEnd = arrayStart;
            boolean inArray = false;

            for (int i = arrayStart; i < popularContent.length(); i++) {
                char c = popularContent.charAt(i);
                if (c == '[' && !inArray) {
                    inArray = true;
                    bracketCount = 1;
                } else if (c == '[' && inArray) {
                    bracketCount++;
                } else if (c == ']' && inArray) {
                    bracketCount--;
                    if (bracketCount == 0) {
                        arrayEnd = i + 1;
                        break;
                    }
                }
            }

            if (arrayEnd <= arrayStart) {
                System.out.println("【热门作品】无法确定" + arrayName + "数组的结束位置");
                return images;
            }

            // 提取数组内容
            String arrayContent = popularContent.substring(arrayStart + 1, arrayEnd - 1);
            System.out.println("【热门作品】" + arrayName + "数组长度: " + arrayContent.length());

            // 分割每个作品对象
            List<String> illustObjects = JsonUtil.splitIllustObjects(arrayContent);
            System.out.println("【热门作品】" + arrayName + "数组找到 " + illustObjects.size() + " 个作品对象");

            for (String illustObj : illustObjects) {
                // 使用JsonUtil中的parseIllustObject方法
                PixivImage image = JsonUtil.parseIllustObject(illustObj);
                if (image != null) {
                    images.add(image);
                    System.out.println("【热门作品】解析" + arrayName + "作品: " + image.getId() + " - " + image.getTitle());
                }
            }

            System.out.println("【热门作品】" + arrayName + "数组解析完成，共找到 " + images.size() + " 个作品");

        } catch (Exception e) {
            System.out.println("【热门作品】解析" + arrayName + "数组失败: " + e.getMessage());
            e.printStackTrace();
        }

        return images;
    }
}
