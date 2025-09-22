package com.pixiv.crawler.util;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.PixivImage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: 对于 Pattern与Matcher 可能需要手动销毁对象
public class JsonUtil {
    private static final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", GlobalConfig.PORT));

    /**
     * 从JSON响应中解析推荐图片完整信息
     * @param jsonResponse JSON响应字符串
     * @param maxImages 最大图片数量
     * @return 推荐图片列表
     */
    public static List<PixivImage> parseRecommendImagesFromJson(String jsonResponse, int maxImages) {
        List<PixivImage> recommendImages = new ArrayList<>();

        try {
            System.out.println("【相关推荐】开始解析JSON响应，长度: " + jsonResponse.length());

            // 查找illusts数组的开始位置
            int illustsStart = jsonResponse.indexOf("\"illusts\":[");
            if (illustsStart == -1) {
                System.out.println("【相关推荐】未找到illusts数组");
                return recommendImages;
            }

            // 找到illusts数组的结束位置
            int bracketCount = 0;
            int illustsEnd = illustsStart;
            boolean inArray = false;

            for (int i = illustsStart; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                if (c == '[' && !inArray) {
                    inArray = true;
                    bracketCount = 1;
                } else if (c == '[' && inArray) {
                    bracketCount++;
                } else if (c == ']' && inArray) {
                    bracketCount--;
                    if (bracketCount == 0) {
                        illustsEnd = i + 1;
                        break;
                    }
                }
            }

            if (illustsEnd <= illustsStart) {
                System.out.println("【相关推荐】无法确定illusts数组的结束位置");
                return recommendImages;
            }

            // 提取illusts数组内容
            String illustsContent = jsonResponse.substring(illustsStart + 10, illustsEnd - 1);
            System.out.println("【相关推荐】illusts数组长度: " + illustsContent.length());

            // 分割每个作品对象
            List<String> illustObjects = splitIllustObjects(illustsContent);
            System.out.println("【相关推荐】找到 " + illustObjects.size() + " 个作品对象");

            int count = 0;
            for (String illustObj : illustObjects) {
                if (count >= maxImages) break;

                PixivImage image = parseIllustObject(illustObj);
                if (image != null) {
                    recommendImages.add(image);
                    count++;
                    System.out.println("【相关推荐】解析作品: " + image.getId() + " - " + image.getTitle());
                }
            }

            System.out.println("【相关推荐】JSON解析完成，共找到 " + recommendImages.size() + " 个推荐图片");

        } catch (Exception e) {
            System.out.println("【相关推荐】解析JSON失败: " + e.getMessage());
            e.printStackTrace();
        }

        return recommendImages;
    }

    /**
     * 分割illusts数组中的作品对象
     */
    public static List<String> splitIllustObjects(String illustsContent) {
        List<String> objects = new ArrayList<>();
        int bracketCount = 0;
        int start = -1;
        boolean inObject = false;

        for (int i = 0; i < illustsContent.length(); i++) {
            char c = illustsContent.charAt(i);

            if (c == '{' && !inObject) {
                inObject = true;
                start = i;
                bracketCount = 1;
            } else if (c == '{' && inObject) {
                bracketCount++;
            } else if (c == '}' && inObject) {
                bracketCount--;
                if (bracketCount == 0) {
                    objects.add(illustsContent.substring(start, i + 1));
                    inObject = false;
                    start = -1;
                }
            }
        }

        return objects;
    }

    /**
     * 解析单个作品对象
     */
    public static PixivImage parseIllustObject(String illustObj) {
        try {
            PixivImage image = new PixivImage();

            // 提取ID
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
            Matcher idMatcher = idPattern.matcher(illustObj);
            if (idMatcher.find()) {
                image.setId(idMatcher.group(1));
            } else {
                System.out.println("【相关推荐】解析失败：未找到ID");
                return null;
            }

            // 提取标题
            Pattern titlePattern = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
            Matcher titleMatcher = titlePattern.matcher(illustObj);
            if (titleMatcher.find()) {
                image.setTitle(titleMatcher.group(1));
            } else {
                image.setTitle("未知标题");
            }

            // 提取作者
            Pattern userPattern = Pattern.compile("\"userName\"\\s*:\\s*\"([^\"]+)\"");
            Matcher userMatcher = userPattern.matcher(illustObj);
            if (userMatcher.find()) {
                image.setArtist(userMatcher.group(1));
            } else {
                image.setArtist("未知作者");
            }

            // 提取创建日期并构造下载URL
            Pattern datePattern = Pattern.compile("\"createDate\"\\s*:\\s*\"([^\"]+)\"");
            Matcher dateMatcher = datePattern.matcher(illustObj);

            if (dateMatcher.find()) {
                String createDate = dateMatcher.group(1);
                // 使用createDate构造下载URL
                try {
                    ZonedDateTime dateTime = ZonedDateTime.parse(createDate);
                    String datePath = String.format("%04d/%02d/%02d/%02d/%02d/%02d",
                            dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());

                    String downloadUrl = "https://i.pximg.net/img-original/img/" + datePath + "/" + image.getId() + "_p0.jpg";
                    image.setUrl(downloadUrl);
                    System.out.println("【相关推荐】构造下载URL: " + downloadUrl);
                } catch (Exception e) {
                    // 如果日期解析失败，使用备用URL
                    String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
                    image.setUrl(fallbackUrl);
                    System.out.println("【相关推荐】日期解析失败，使用备用URL: " + fallbackUrl);
                }
            } else {
                // 如果没有createDate，使用备用URL
                String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
                image.setUrl(fallbackUrl);
                System.out.println("【相关推荐】无createDate，使用备用URL: " + fallbackUrl);
            }

            // 确保URL不为null
            if (image.getUrl() == null) {
                String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
                image.setUrl(fallbackUrl);
                System.out.println("【相关推荐】URL为null，设置备用URL: " + fallbackUrl);
            }

            // 收藏数通过API获取，这里先设为0，后续会更新
            image.setBookmarkCount(0);

            // 解析tags并检测R-18
            List<String> tags = parseTags(illustObj);
            image.setTags(tags);
            
            // R-18检测：检查第一个标签是否为R-18
            if (tags != null && !tags.isEmpty()) {
                String firstTag = tags.get(0);
                if ("R-18".equals(firstTag) || "R18".equals(firstTag)) {
                    image.setR18(true);
                    System.out.println("【R-18检测】作品 " + image.getId() + " 被标记为R-18 (标签: " + firstTag + ")");
                }
            }
            
            // 漫画检测：检查tags中是否包含漫画关键词
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags) {
                    if (tag.contains("漫画")) {
                        image.setManga(true);
                        System.out.println("【漫画检测】作品 " + image.getId() + " 被标记为漫画 (标签: " + tag + ")");
                        break;
                    }
                }
            }

            return image;

        } catch (Exception e) {
            System.out.println("【相关推荐】解析作品对象失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从Illust对象中提取作品ID
     * @param illustObj Illust对象的JSON字符串
     * @return 作品ID，如果提取失败则返回null
     */
    public static String extractIdFromIllustObj(String illustObj) {
        try {
            // 使用正则表达式提取ID，确保正确处理引号
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
            Matcher idMatcher = idPattern.matcher(illustObj);
            if (idMatcher.find()) {
                return idMatcher.group(1); // 返回第一个捕获组，即数字ID
            }
            return null;
        } catch (Exception e) {
            System.out.println("【JsonUtil】从Illust对象中提取ID失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    
    /**
     * 解析作品对象中的tags字段
     * 解析嵌套格式：body.tags.tags数组，每个对象包含tag属性
     */
    public static List<String> parseTags(String illustObj) {
        if (illustObj == null || illustObj.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            List<String> tags = new ArrayList<>();
            
            // 查找 "tags": { ... "tags": [ ... ] ... }
            Pattern nestedTagsPattern = Pattern.compile("\"tags\"\\s*:\\s*\\{[^}]*\"tags\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = nestedTagsPattern.matcher(illustObj);
            
            if (matcher.find()) {
                String tagsArrayContent = matcher.group(1);
                System.out.println("【标签解析】找到嵌套tags数组内容: " + tagsArrayContent.substring(0, Math.min(100, tagsArrayContent.length())) + "...");
                
                // 解析数组中的每个对象，提取tag属性
                Pattern tagObjectPattern = Pattern.compile("\\{[^}]*\"tag\"\\s*:\\s*\"([^\"]+)\"");
                Matcher tagMatcher = tagObjectPattern.matcher(tagsArrayContent);
                
                while (tagMatcher.find()) {
                    String tag = tagMatcher.group(1);
                    if (tag != null && !tag.trim().isEmpty()) {
                        String trimmedTag = tag.trim();
                        // 处理Unicode转义字符
                        String unescapedTag = unescapeUnicode(trimmedTag);
                        tags.add(unescapedTag);
                        System.out.println("【标签解析】提取到标签: [" + unescapedTag + "] (长度: " + unescapedTag.length() + ")");
                    }
                }
                
                System.out.println("【标签解析】解析完成，找到 " + tags.size() + " 个标签");
            } else {
                System.out.println("【标签解析】未找到嵌套tags结构");
            }
            
            return tags;
            
        } catch (Exception e) {
            System.out.println("【标签解析】解析tags时发生错误: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 将Unicode转义字符转换为正常字符
     * 例如: \\u30b9\\u30ba\\u30e9\\u30f3 -> スズラン
     */
    private static String unescapeUnicode(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < str.length()) {
                if (i < str.length() - 5 && str.charAt(i) == '\\' && str.charAt(i + 1) == 'u') {
                    // 找到 \\uXXXX 格式
                    String hex = str.substring(i + 2, i + 6);
                    try {
                        int codePoint = Integer.parseInt(hex, 16);
                        sb.append((char) codePoint);
                        i += 6;
                    } catch (NumberFormatException e) {
                        // 如果不是有效的十六进制，保持原样
                        sb.append(str.charAt(i));
                        i++;
                    }
                } else {
                    sb.append(str.charAt(i));
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println("【Unicode转义】处理失败: " + e.getMessage());
            return str; // 如果处理失败，返回原字符串
        }
    }

    /**
     * 根据pid获取完整的PixivImage对象
     * 这是一个通用的方法，整合了从API获取作品信息、tags、收藏数、URL等所有功能
     * 
     * @param pid 作品ID
     * @return PixivImage对象，如果获取失败返回null
     */
    public static PixivImage getImageInfoById(String pid) {
        if (pid == null || pid.isEmpty()) {
            System.out.println("【JsonUtil】作品ID为空");
            return null;
        }

        try {
            // 创建HTTP客户端
            OkHttpClient client = new OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 构建API请求
            String apiUrl = "https://www.pixiv.net/ajax/illust/" + pid;
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .addHeader("Cookie", GlobalConfig.COOKIE)
                    .addHeader("Referer", "https://www.pixiv.net/artworks/" + pid)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .build();

            // 发送请求
            Response response = client.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                System.out.println("【JsonUtil】API请求失败，状态码: " + response.code());
                return null;
            }

            String responseText = response.body().string();
            if (responseText == null || responseText.isEmpty()) {
                System.out.println("【JsonUtil】API响应为空");
                return null;
            }

            // 创建PixivImage对象
            PixivImage image = new PixivImage();
            image.setId(pid);

            // 解析基本信息
            parseBasicInfo(responseText, image);
            
            // 解析tags信息
            // TODO: tags 解析有问题
            List<String> tags = parseTags(responseText);
            image.setTags(tags);

            System.out.println("【JsonUtil】" + pid + " tags:" + tags);
            
            // 检测R-18和漫画
            detectContentFlags(tags, image);
            
            // 提取pageCount并构造下载URL
            extractPageCountAndConstructUrl(responseText, image);

            System.out.println("【JsonUtil】成功获取作品信息: " + pid + " - " + image.getTitle());
            return image;

        } catch (Exception e) {
            System.out.println("【JsonUtil】获取作品信息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析基本信息（标题、作者、收藏数等）
     */
    private static void parseBasicInfo(String responseText, PixivImage image) {
        try {
            // 提取标题
            Pattern titlePattern = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
            Matcher titleMatcher = titlePattern.matcher(responseText);
            if (titleMatcher.find()) {
                image.setTitle(titleMatcher.group(1));
            } else {
                image.setTitle("未知标题");
            }

            // 提取作者
            Pattern userPattern = Pattern.compile("\"userName\"\\s*:\\s*\"([^\"]+)\"");
            Matcher userMatcher = userPattern.matcher(responseText);
            if (userMatcher.find()) {
                image.setArtist(userMatcher.group(1));
            } else {
                image.setArtist("未知作者");
            }

            // 提取收藏数
            int bookmarkCount = extractBookmarkCount(responseText);
            image.setBookmarkCount(bookmarkCount);

        } catch (Exception e) {
            System.out.println("【JsonUtil】解析基本信息失败: " + e.getMessage());
        }
    }

    /**
     * 提取收藏数
     */
    private static int extractBookmarkCount(String responseText) {
        try {
            // 尝试多种可能的收藏数字段
            String[] patterns = {
                "\"totalBookmarks\"\\s*:\\s*(\\d+)",
                "\"bookmarkCount\"\\s*:\\s*(\\d+)",
                "\"bookmarks\"\\s*:\\s*(\\d+)",
                "\"body\"\\s*:\\s*\\{[^}]*\"bookmarkCount\"\\s*:\\s*(\\d+)"
            };

            for (String pattern : patterns) {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(responseText);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception e) {
            System.out.println("【JsonUtil】提取收藏数失败: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 检测内容标志（R-18、漫画等）
     */
    private static void detectContentFlags(List<String> tags, PixivImage image) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        // R-18检测
        String firstTag = tags.get(0);
        if ("R-18".equals(firstTag) || "R18".equals(firstTag)) {
            image.setR18(true);
            System.out.println("【JsonUtil】作品 " + image.getId() + " 被标记为R-18 (标签: " + firstTag + ")");
        }

        // 漫画检测
        for (String tag : tags) {
            if (tag.contains("漫画")) {
                image.setManga(true);
                System.out.println("【JsonUtil】作品 " + image.getId() + " 被标记为漫画 (标签: " + tag + ")");
                break;
            }
        }
    }

    /**
     * 提取pageCount并构造下载URL
     */
    private static void extractPageCountAndConstructUrl(String responseText, PixivImage image) {
        try {
            // 提取 pageCount
            int pageCount = Math.min(extractPageCount(responseText), GlobalConfig.MAX_IMAGES_PER_WORK);
            image.setPageCount(pageCount);
            System.out.println("【JsonUtil】作品 " + image.getId() + " 共有 " + pageCount + " 页");
            
            // 从urls.original获取URL
            Pattern originalUrlPattern = Pattern.compile("\"original\"\\s*:\\s*\"([^\"]+)\"");
            Matcher originalUrlMatcher = originalUrlPattern.matcher(responseText);
            
            if (originalUrlMatcher.find()) {
                String originalUrl = originalUrlMatcher.group(1);
                image.setUrl(originalUrl);
                System.out.println("【JsonUtil】使用原始URL: " + originalUrl);
            }
        } catch (Exception e) {
            // 如果构造URL失败，使用备用URL
            String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
            image.setUrl(fallbackUrl);
            System.out.println("【JsonUtil】构造URL失败，使用备用URL: " + fallbackUrl);
        }
    }
    
    // 提取 pageCount
    private static int extractPageCount(String responseText) {
        try {
            String pattern ="\"pageCount\"\\s*:\\s*(\\d+)";

            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(responseText);
            if (m.find()) {
                int pageCount = Integer.parseInt(m.group(1));
                System.out.println("【JsonUtil】提取到pageCount: " + pageCount);
                return pageCount;
            }

        } catch (Exception e) {
            System.out.println("【JsonUtil】提取pageCount失败: " + e.getMessage());
        }
        System.out.println("【JsonUtil】未找到pageCount，默认为1页");
        return 1;
    }
}
