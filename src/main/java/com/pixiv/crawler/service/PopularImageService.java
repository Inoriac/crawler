package com.pixiv.crawler.service;

import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface PopularImageService {
    public List<PixivImage> getPopularImagesByTag(String tag) throws Exception;

    /**
     * 从JSON响应中解析热门作品信息
     * @param jsonResponse JSON响应字符串
     * @return 热门作品列表
     */
    default List<PixivImage> parsePopularWorksFromJson(String jsonResponse) {
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

            // 解析permanent数组
            List<PixivImage> permanentImages = parsePopularArray(popularContent, "permanent");
            popularImages.addAll(permanentImages);

            // 解析recent数组
            List<PixivImage> recentImages = parsePopularArray(popularContent, "recent");
            popularImages.addAll(recentImages);

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
                // 使用正则表达式提取ID，确保正确处理引号
                String pid = null;
                Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
                Matcher idMatcher = idPattern.matcher(illustObj);
                if (idMatcher.find()) {
                    pid = idMatcher.group(1); // 返回第一个捕获组，即数字ID
                }

                if (pid != null) {
                    // 使用JsonUtil中的getImageInfoById方法获取完整信息
                    PixivImage image = JsonUtil.getImageInfoById(pid);
                    if (image.getBookmarkCount() >= 500) {
                        images.add(image);
                        System.out.println("【热门作品】解析" + arrayName + "作品: " + image.getId() + " - " + image.getTitle());
                    } else {
                        System.out.println("【热门作品】收藏数小于500，舍弃");
                    }
                } else {
                    System.out.println("【热门作品】无法从JSON对象中提取ID");
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
