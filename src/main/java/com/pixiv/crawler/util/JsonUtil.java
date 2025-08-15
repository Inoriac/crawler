package com.pixiv.crawler.util;

import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.model.PixivImage;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    private static final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", PixivCrawlerConfig.PORT));

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
            java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
            java.util.regex.Matcher idMatcher = idPattern.matcher(illustObj);
            if (idMatcher.find()) {
                image.setId(idMatcher.group(1));
            } else {
                System.out.println("【相关推荐】解析失败：未找到ID");
                return null;
            }

            // 提取标题
            java.util.regex.Pattern titlePattern = java.util.regex.Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher titleMatcher = titlePattern.matcher(illustObj);
            if (titleMatcher.find()) {
                image.setTitle(titleMatcher.group(1));
            } else {
                image.setTitle("未知标题");
            }

            // 提取作者
            java.util.regex.Pattern userPattern = java.util.regex.Pattern.compile("\"userName\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher userMatcher = userPattern.matcher(illustObj);
            if (userMatcher.find()) {
                image.setArtist(userMatcher.group(1));
            } else {
                image.setArtist("未知作者");
            }

            // 提取创建日期并构造下载URL
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("\"createDate\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher dateMatcher = datePattern.matcher(illustObj);

            if (dateMatcher.find()) {
                String createDate = dateMatcher.group(1);
                // 使用createDate构造下载URL
                try {
                    java.time.ZonedDateTime dateTime = java.time.ZonedDateTime.parse(createDate);
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
     * 从作品JSON对象中提取tags字段
     */
    private static String extractTagsFromIllust(String illustObj) {
        String[] possibleTagFields = {
            "\"tags\":", "\"tag\":", "\"tagList\":", "\"tag_list\":",
            "\"illustTags\":", "\"illust_tags\":", "\"userTags\":", "\"user_tags\":"
        };
        
        for (String tagField : possibleTagFields) {
            int tagStart = illustObj.indexOf(tagField);
            if (tagStart != -1) {
                return extractTagContentFromIllust(illustObj, tagStart + tagField.length());
            }
        }
        
        return null;
    }
    
    /**
     * 提取tags字段的内容
     */
    private static String extractTagContentFromIllust(String illustObj, int startPos) {
        int bracketCount = 0;
        boolean inArray = false;
        int endPos = startPos;
        
        for (int i = startPos; i < illustObj.length(); i++) {
            char c = illustObj.charAt(i);
            
            if (c == '[' && !inArray) {
                inArray = true;
                bracketCount = 1;
            } else if (c == '[' && inArray) {
                bracketCount++;
            } else if (c == ']' && inArray) {
                bracketCount--;
                if (bracketCount == 0) {
                    endPos = i;
                    break;
                }
            } else if (c == '{' && !inArray) {
                inArray = true;
                bracketCount = 1;
            } else if (c == '{' && inArray) {
                bracketCount++;
            } else if (c == '}' && inArray) {
                bracketCount--;
                if (bracketCount == 0) {
                    endPos = i;
                    break;
                }
            }
        }
        
        if (endPos > startPos) {
            return illustObj.substring(startPos, endPos + 1);
        }
        
        return null;
    }
    
    /**
     * 解析作品对象中的tags字段
     */
    public static List<String> parseTags(String illustObj) {
        if (illustObj == null || illustObj.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String tagsContent = extractTagsFromIllust(illustObj);
            if (tagsContent == null || tagsContent.isEmpty()) {
                return new ArrayList<>();
            }
            
            return parseTagsArray(tagsContent);
            
        } catch (Exception e) {
            System.out.println("【标签解析】解析tags时发生错误: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 解析tags数组字符串为List<String>
     */
    private static List<String> parseTagsArray(String tagsContent) {
        List<String> tags = new ArrayList<>();
        
        if (tagsContent == null || tagsContent.isEmpty()) {
            return tags;
        }
        
        // 移除方括号
        if (tagsContent.startsWith("[") && tagsContent.endsWith("]")) {
            tagsContent = tagsContent.substring(1, tagsContent.length() - 1);
        }
        
        // 分割标签
        String[] tagArray = tagsContent.split(",");
        for (String tag : tagArray) {
            tag = tag.trim();
            // 移除引号
            if ((tag.startsWith("\"") && tag.endsWith("\"")) || 
                (tag.startsWith("'") && tag.endsWith("'"))) {
                tag = tag.substring(1, tag.length() - 1);
            }
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        
        return tags;
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
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // 构建API请求
            String apiUrl = "https://www.pixiv.net/ajax/illust/" + pid;
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .addHeader("Cookie", "first_visit_datetime_pc=2025-01-24%2013%3A39%3A04; yuid_b=MCeXORk; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=129495109; _ga=GA1.1.509352844.1737693546; __utmz=235335808.1749878658.13.8.utmcsr=t.co|utmccn=(referral)|utmcmd=referral|utmcct=/FL4mTt3PlR; cc1=2025-08-06%2017%3A07%3A38; _cfuvid=dhu66.SQnP2SeQqDYX1NQPH6AvHaHtI_kcIF2iR8FHU-1754467658283-0.0.1.1-604800000; __utma=235335808.102379013.1737693545.1749878658.1754467660.14; __utmc=235335808; PHPSESSID=118685673_LVep8oNv3HJHw5ZXhrhkC8UXqS4JhYib; device_token=7aac299b90b54c3a3aba9ca0e3e2b3fd; privacy_policy_agreement=7; __cf_bm=plID.4OFYNfMroRUjYF8mrOUl6593QgUkXvVil7LgxI-1754467961-1.0.1.1-uLg7Wc6vakjhBmK1Rspog0B9k9cOhmmm9CnpDzgrqTwiqLj3sZPIlvOlB.A1f1_FvHt5Lb0V3CCyG1tO33wDiuZwDViLOAknuPC9d1t3NuV9YOgGTFhc_IsnQ1y88fT1sUWAf6INGqU_rreXVQyqTw; _ga_MZ1NL4PHH0=GS2.1.s1754467663$o2$g1$t1754467960$j36$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=1; cto_bundle=-0N60F9VcUlIYlVqQUZuTzhvamJGeFpxMm1xZDJvWFJIbG5vd3NtejBBcE9hayUyQnZwdDFZQ3d5bnBjdmhCWGhiaDFBTnlxdk1ONjRCTzhYUFJaMFd0ZWh3cUxlUnMlMkZITDA2RlA1JTJGSnp2M2dxSzZjSmFjWjZHNHJ2TFlUaWJnZHhvRXZTUkdTWUszVTdzQkF5eFdYbVk5NW5UUEElM0QlM0Q; __utmt=1; cf_clearance=ZjAvoq.fB0OvSfzra0.4btF7CQsGtUwK8d72wFEPzdk-1754468388-1.2.1.1-mQAtBJq6TR4o.DQAjzKmy6Kf_nfxJ6kBfCrVUqmkA8iuaXksLCas.pc46m0jTMUQto0e32wlkVi_6dt1AtpuItEjeRrwlSyN5L3l1YYGklI6qg96ab02Hs.3oFJvpLCv_abiKG1KJnVBAPnfA_Uy8jPcW.eAKrKhGs0KyzqqGqc.ddkmykgZhmiRgMC5iIBYFF3MjaZ3pjBUxeuvuIXOJWBsHSLOtdPdavhNKOI60MQ; FCNEC=%5B%5B%22AKsRol98GxziMSGoioFiuERYQricQBB31ShRqusG9woungPV30ba-ipsGq-EAAEvdvZ0LTn6N3Zse-ncHadmKoDoNN0U8mQ1WCdVwJd4BgZ67zj5STxul6UQEC1mvNE7X51VnbEeMyKh_ofpVkFYdCIHJ9ustrHJIQ%3D%3D%22%5D%5D; login_ever=yes; __utmv=235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^6=user_id=118685673=1^9=p_ab_id=8=1^10=p_ab_id_2=8=1^11=lang=zh=1; __utmb=235335808.4.10.1754467660; _ga_75BBYNYN9J=GS2.1.s1754467660$o15$g1$t1754468463$j59$l0$h0")
                    .addHeader("Referer", "https://www.pixiv.net/artworks/" + pid)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .build();

            // 发送请求
            okhttp3.Response response = client.newCall(request).execute();
            
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
            List<String> tags = parseTags(responseText);
            image.setTags(tags);
            
            // 检测R-18和漫画
            detectContentFlags(tags, image);
            
            // 构造下载URL
            constructDownloadUrl(responseText, image);

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
            java.util.regex.Pattern titlePattern = java.util.regex.Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher titleMatcher = titlePattern.matcher(responseText);
            if (titleMatcher.find()) {
                image.setTitle(titleMatcher.group(1));
            } else {
                image.setTitle("未知标题");
            }

            // 提取作者
            java.util.regex.Pattern userPattern = java.util.regex.Pattern.compile("\"userName\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher userMatcher = userPattern.matcher(responseText);
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
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(responseText);
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
     * 构造下载URL
     */
    private static void constructDownloadUrl(String responseText, PixivImage image) {
        try {
            // 提取createDate
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("\"createDate\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher dateMatcher = datePattern.matcher(responseText);
            
            if (dateMatcher.find()) {
                String createDate = dateMatcher.group(1);
                // 使用createDate构造下载URL
                try {
                    java.time.ZonedDateTime dateTime = java.time.ZonedDateTime.parse(createDate);
                    String datePath = String.format("%04d/%02d/%02d/%02d/%02d/%02d",
                            dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());

                    String downloadUrl = "https://i.pximg.net/img-original/img/" + datePath + "/" + image.getId() + "_p0.jpg";
                    image.setUrl(downloadUrl);
                    System.out.println("【JsonUtil】构造下载URL: " + downloadUrl);
                } catch (Exception e) {
                    // 如果日期解析失败，使用备用URL
                    String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
                    image.setUrl(fallbackUrl);
                    System.out.println("【JsonUtil】日期解析失败，使用备用URL: " + fallbackUrl);
                }
            } else {
                // 如果没有createDate，使用备用URL
                String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
                image.setUrl(fallbackUrl);
                System.out.println("【JsonUtil】无createDate，使用备用URL: " + fallbackUrl);
            }
        } catch (Exception e) {
            // 如果构造URL失败，使用备用URL
            String fallbackUrl = "https://embed.pixiv.net/artwork.php?illust_id=" + image.getId();
            image.setUrl(fallbackUrl);
            System.out.println("【JsonUtil】构造URL失败，使用备用URL: " + fallbackUrl);
        }
    }
}
