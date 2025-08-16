package com.pixiv.crawler.util;

import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.model.PixivImage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class PixivRecHelper {
    private static final String baseUrl = "https://www.pixiv.net/artworks/";

    private static final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", PixivCrawlerConfig.PORT));

    /**
     * 根据 pid 获取下面相关推荐列表
     * @param pid
     * @param maxImages
     * @return
     */
    public static List<PixivImage> getRecommendImagesByPid(String pid, int maxImages, int port) throws Exception{
        System.out.println("【相关推荐】尝试通过AJAX API获取<" + pid + ">作品的推荐图片...");

        List<PixivImage> recommendImages = new ArrayList<>();

        String ajaxUrl = "https://www.pixiv.net/ajax/illust/" + pid + "/recommend/init";
        String queryParams = "limit=18&lang=zh";

        System.out.println("【相关推荐】使用推荐API端点: " + ajaxUrl);

        try {
            System.out.println("【相关推荐】AJAX请求URL: " + ajaxUrl + "?" + queryParams);

            // 使用OkHttp来发送AJAX请求
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(ajaxUrl + "?" + queryParams)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .addHeader("Cookie", "first_visit_datetime_pc=2025-01-24%2013%3A39%3A04; yuid_b=MCeXORk; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=129495109; _ga=GA1.1.509352844.1737693546; __utmz=235335808.1749878658.13.8.utmcsr=t.co|utmccn=(referral)|utmcmd=referral|utmcct=/FL4mTt3PlR; cc1=2025-08-06%2017%3A07%3A38; _cfuvid=dhu66.SQnP2SeQqDYX1NQPH6AvHaHtI_kcIF2iR8FHU-1754467658283-0.0.1.1-604800000; __utma=235335808.102379013.1737693545.1749878658.1754467660.14; __utmc=235335808; PHPSESSID=118685673_LVep8oNv3HJHw5ZXhrhkC8UXqS4JhYib; device_token=7aac299b90b54c3a3aba9ca0e3e2b3fd; privacy_policy_agreement=7; __cf_bm=plID.4OFYNfMroRUjYF8mrOUl6593QgUkXvVil7LgxI-1754467961-1.0.1.1-uLg7Wc6vakjhBmK1Rspog0B9k9cOhmmm9CnpDzgrqTwiqLj3sZPIlvOlB.A1f1_FvHt5Lb0V3CCyG1tO33wDiuZwDViLOAknuPC9d1t3NuV9YOgGTFhc_IsnQ1y88fT1sUWAf6INGqU_rreXVQyqTw; _ga_MZ1NL4PHH0=GS2.1.s1754467663$o2$g1$t1754467960$j36$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=1; cto_bundle=-0N60F9VcUlIYlVqQUZuTzhvamJGeFpxMm1xZDJvWFJIbG5vd3NtejBBcE9hayUyQnZwdDFZQ3d5bnBjdmhCWGhiaDFBTnlxdk1ONjRCTzhYUFJaMFd0ZWh3cUxlUnMlMkZITDA2RlA1JTJGSnp2M2dxSzZjSmFjWjZHNHJ2TFlUaWJnZHhvRXZTUkdTWUszVTdzQkF5eFdYbVk5NW5UUEElM0QlM0Q; __utmt=1; cf_clearance=ZjAvoq.fB0OvSfzra0.4btF7CQsGtUwK8d72wFEPzdk-1754468388-1.2.1.1-mQAtBJq6TR4o.DQAjzKmy6Kf_nfxJ6kBfCrVUqmkA8iuaXksLCas.pc46m0jTMUQto0e32wlkVi_6dt1AtpuItEjeRrwlSyN5L3l1YYGklI6qg96ab02Hs.3oFJvpLCv_abiKG1KJnVBAPnfA_Uy8jPcW.eAKrKhGs0KyzqqGqc.ddkmykgZhmiRgMC5iIBYFF3MjaZ3pjBUxeuvuIXOJWBsHSLOtdPdavhNKOI60MQ; FCNEC=%5B%5B%22AKsRol98GxziMSGoioFiuERYQricQBB31ShRqusG9woungPV30ba-ipsGq-EAAEvdvZ0LTn6N3Zse-ncHadmKoDoNN0U8mQ1WCdVwJd4BgZ67zj5STxul6UQEC1mvNE7X51VnbEeMyKh_ofpVkFYdCIHJ9ustrHJIQ%3D%3D%22%5D%5D; login_ever=yes; __utmv=235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^6=user_id=118685673=1^9=p_ab_id=8=1^10=p_ab_id_2=8=1^11=lang=zh=1; __utmb=235335808.4.10.1754467660; _ga_75BBYNYN9J=GS2.1.s1754467660$o15$g1$t1754468463$j59$l0$h0")
                    .addHeader("Referer", baseUrl + pid)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .build();

            okhttp3.Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                System.out.println("【相关推荐】推荐API返回状态码: " + response.code());
                return recommendImages;
            }

            // 获取JSON响应
            String responseText = response.body().string();
            System.out.println("【相关推荐】AJAX响应长度: " + responseText.length());
            System.out.println("【相关推荐】响应内容类型: " + response.header("Content-Type"));

            // 保存响应到根目录用于检查tag
            // String filename = "ajax_response_" + pid + "_init.json";
            // saveResponseToFile(responseText, filename);

            // 解析JSON获取推荐图片完整信息
            recommendImages = JsonUtil.parseRecommendImagesFromJson(responseText, maxImages);
            System.out.println("【相关推荐】从推荐API获取到 " + recommendImages.size() + " 个推荐图片");

        } catch (Exception e) {
            System.out.println("【相关推荐】推荐API请求失败: " + e.getMessage());
            // 不打印完整的堆栈跟踪，只显示错误信息
        }

        System.out.println("【相关推荐】已查找" + recommendImages.size() + "张图片");
        return recommendImages;
    }

    /**
     * 从API获取收藏数
     * @param pid 作品ID
     * @return 收藏数，如果获取失败返回0
     */
    public static int getBookmarkCountFromApi(String pid) throws Exception {
        // 尝试使用作品详情API获取收藏数
        String apiUrl = "https://www.pixiv.net/ajax/illust/" + pid;
        
        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .addHeader("Cookie", "first_visit_datetime_pc=2025-01-24%2013%3A39%3A04; yuid_b=MCeXORk; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=129495109; _ga=GA1.1.509352844.1737693546; __utmz=235335808.1749878658.13.8.utmcsr=t.co|utmccn=(referral)|utmcmd=referral|utmcct=/FL4mTt3PlR; cc1=2025-08-06%2017%3A07%3A38; _cfuvid=dhu66.SQnP2SeQqDYX1NQPH6AvHaHtI_kcIF2iR8FHU-1754467658283-0.0.1.1-604800000; __utma=235335808.102379013.1737693545.1749878658.1754467660.14; __utmc=235335808; PHPSESSID=118685673_LVep8oNv3HJHw5ZXhrhkC8UXqS4JhYib; device_token=7aac299b90b54c3a3aba9ca0e3e2b3fd; privacy_policy_agreement=7; __cf_bm=plID.4OFYNfMroRUjYF8mrOUl6593QgUkXvVil7LgxI-1754467961-1.0.1.1-uLg7Wc6vakjhBmK1Rspog0B9k9cOhmmm9CnpDzgrqTwiqLj3sZPIlvOlB.A1f1_FvHt5Lb0V3CCyG1tO33wDiuZwDViLOAknuPC9d1t3NuV9YOgGTFhc_IsnQ1y88fT1sUWAf6INGqU_rreXVQyqTw; _ga_MZ1NL4PHH0=GS2.1.s1754467663$o2$g1$t1754467960$j36$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=1; cto_bundle=-0N60F9VcUlIYlVqQUZuTzhvamJGeFpxMm1xZDJvWFJIbG5vd3NtejBBcE9hayUyQnZwdDFZQ3d5bnBjdmhCWGhiaDFBTnlxdk1ONjRCTzhYUFJaMFd0ZWh3cUxlUnMlMkZITDA2RlA1JTJGSnp2M2dxSzZjSmFjWjZHNHJ2TFlUaWJnZHhvRXZTUkdTWUszVTdzQkF5eFdYbVk5NW5UUEElM0QlM0Q; __utmt=1; cf_clearance=ZjAvoq.fB0OvSfzra0.4btF7CQsGtUwK8d72wFEPzdk-1754468388-1.2.1.1-mQAtBJq6TR4o.DQAjzKmy6Kf_nfxJ6kBfCrVUqmkA8iuaXksLCas.pc46m0jTMUQto0e32wlkVi_6dt1AtpuItEjeRrwlSyN5L3l1YYGklI6qg96ab02Hs.3oFJvpLCv_abiKG1KJnVBAPnfA_Uy8jPcW.eAKrKhGs0KyzqqGqc.ddkmykgZhmiRgMC5iIBYFF3MjaZ3pjBUxeuvuIXOJWBsHSLOtdPdavhNKOI60MQ; FCNEC=%5B%5B%22AKsRol98GxziMSGoioFiuERYQricQBB31ShRqusG9woungPV30ba-ipsGq-EAAEvdvZ0LTn6N3Zse-ncHadmKoDoNN0U8mQ1WCdVwJd4BgZ67zj5STxul6UQEC1mvNE7X51VnbEeMyKh_ofpVkFYdCIHJ9ustrHJIQ%3D%3D%22%5D%5D; login_ever=yes; __utmv=235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^6=user_id=118685673=1^9=p_ab_id=8=1^10=p_ab_id_2=8=1^11=lang=zh=1; __utmb=235335808.4.10.1754467660; _ga_75BBYNYN9J=GS2.1.s1754467660$o15$g1$t1754468463$j59$l0$h0")
                    .addHeader("Referer", baseUrl + pid)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .build();

            okhttp3.Response response = client.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                System.out.println("【图片信息】作品详情API返回状态码: " + response.code());
                return 0;
            }
            
            String responseText = response.body().string();

            // 查找收藏数字段
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"totalBookmarks\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(responseText);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            
            // 查找 "bookmarkCount": 数字 的模式
            pattern = java.util.regex.Pattern.compile("\"bookmarkCount\"\\s*:\\s*(\\d+)");
            matcher = pattern.matcher(responseText);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            
            // 查找 "bookmarks": 数字 的模式
            pattern = java.util.regex.Pattern.compile("\"bookmarks\"\\s*:\\s*(\\d+)");
            matcher = pattern.matcher(responseText);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            
            // 查找 "bookmarkCount": 数字 的模式（在body中）
            pattern = java.util.regex.Pattern.compile("\"body\"\\s*:\\s*\\{[^}]*\"bookmarkCount\"\\s*:\\s*(\\d+)");
            matcher = pattern.matcher(responseText);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            
            System.out.println("【图片信息】作品详情API响应中未找到收藏数字段");
            return 0;
            
        } catch (Exception e) {
            System.out.println("【图片信息】从API获取收藏数失败: " + e.getMessage());
            return 0;
        } finally {
            // 添加延迟避免请求过于频繁
            try {
                int sleepTime = (int)(Math.random() * 500); // 0-0.5秒随机延迟
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                System.out.println("【图片信息】休眠被中断");
            }
        }
    }

    /**
     * 保存响应到文件用于调试
     */
    private static void saveResponseToFile(String response, String filename) {
        try {
            // 获取项目根目录路径
            String rootDir = System.getProperty("user.dir");
            java.nio.file.Path filePath = java.nio.file.Paths.get(rootDir, filename);

            java.nio.file.Files.write(
                    filePath,
                    response.getBytes("UTF-8")
            );
            System.out.println("【调试】AJAX响应已保存到文件: " + filePath.toString());
        } catch (Exception e) {
            System.out.println("【调试】保存AJAX响应文件失败: " + e.getMessage());
        }
    }
}
