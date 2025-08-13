package com.pixiv.crawler.util;

import com.pixiv.crawler.model.PixivImage;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

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

            return image;

        } catch (Exception e) {
            System.out.println("【相关推荐】解析作品对象失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
