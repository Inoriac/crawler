package com.pixiv.crawler.util;

import com.pixiv.crawler.model.PixivImage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PixivRecHelper {
    private static final String baseUrl = "https://www.pixiv.net/artworks/";

    /**
     * 根据 pid 获取下面相关推荐列表
     * @param pid
     * @param maxImages
     * @return
     */
    public static List<String> getRecommendIdsByPid(String pid, int maxImages) throws Exception{
        String imageUrl = baseUrl + pid;
        // 创建代理对象
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));

        System.out.println("【相关推荐】尝试访问<" +pid + ">作品的详情页...");

        // 访问作品详情页
        Document document = Jsoup.connect(imageUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .header("Cookie","first_visit_datetime_pc=2025-01-24%2013%3A39%3A04; yuid_b=MCeXORk; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=129495109; _ga=GA1.1.509352844.1737693546; __utmz=235335808.1749878658.13.8.utmcsr=t.co|utmccn=(referral)|utmcmd=referral|utmcct=/FL4mTt3PlR; cc1=2025-08-06%2017%3A07%3A38; _cfuvid=dhu66.SQnP2SeQqDYX1NQPH6AvHaHtI_kcIF2iR8FHU-1754467658283-0.0.1.1-604800000; __utma=235335808.102379013.1737693545.1749878658.1754467660.14; __utmc=235335808; PHPSESSID=118685673_LVep8oNv3HJHw5ZXhrhkC8UXqS4JhYib; device_token=7aac299b90b54c3a3aba9ca0e3e2b3fd; privacy_policy_agreement=7; __cf_bm=plID.4OFYNfMroRUjYF8mrOUl6593QgUkXvVil7LgxI-1754467961-1.0.1.1-uLg7Wc6vakjhBmK1Rspog0B9k9cOhmmm9CnpDzgrqTwiqLj3sZPIlvOlB.A1f1_FvHt5Lb0V3CCyG1tO33wDiuZwDViLOAknuPC9d1t3NuV9YOgGTFhc_IsnQ1y88fT1sUWAf6INGqU_rreXVQyqTw; _ga_MZ1NL4PHH0=GS2.1.s1754467663$o2$g1$t1754467960$j36$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=1; cto_bundle=-0N60F9VcUlIYlVqQUZuTzhvamJGeFpxMm1xZDJvWFJIbG5vd3NtejBBcE9hayUyQnZwdDFZQ3d5bnBjdmhCWGhiaDFBTnlxdk1ONjRCTzhYUFJaMFd0ZWh3cUxlUnMlMkZITDA2RlA1JTJGSnp2M2dxSzZjSmFjWjZHNHJ2TFlUaWJnZHhvRXZTUkdTWUszVTdzQkF5eFdYbVk5NW5UUEElM0QlM0Q; __utmt=1; cf_clearance=ZjAvoq.fB0OvSfzra0.4btF7CQsGtUwK8d72wFEPzdk-1754468388-1.2.1.1-mQAtBJq6TR4o.DQAjzKmy6Kf_nfxJ6kBfCrVUqmkA8iuaXksLCas.pc46m0jTMUQto0e32wlkVi_6dt1AtpuItEjeRrwlSyN5L3l1YYGklI6qg96ab02Hs.3oFJvpLCv_abiKG1KJnVBAPnfA_Uy8jPcW.eAKrKhGs0KyzqqGqc.ddkmykgZhmiRgMC5iIBYFF3MjaZ3pjBUxeuvuIXOJWBsHSLOtdPdavhNKOI60MQ; FCNEC=%5B%5B%22AKsRol98GxziMSGoioFiuERYQricQBB31ShRqusG9woungPV30ba-ipsGq-EAAEvdvZ0LTn6N3Zse-ncHadmKoDoNN0U8mQ1WCdVwJd4BgZ67zj5STxul6UQEC1mvNE7X51VnbEeMyKh_ofpVkFYdCIHJ9ustrHJIQ%3D%3D%22%5D%5D; login_ever=yes; __utmv=235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^6=user_id=118685673=1^9=p_ab_id=8=1^10=p_ab_id_2=8=1^11=lang=zh=1; __utmb=235335808.4.10.1754467660; _ga_75BBYNYN9J=GS2.1.s1754467660$o15$g1$t1754468463$j59$l0$h0")
                .proxy(proxy)
                .get();

        List<String> recommendIds = new ArrayList<>();
        Elements recommendZone = document.select("div.gtm-illust-recommend-zone");
        if(recommendZone.isEmpty()){
            System.out.println("【相关推荐】未找到推荐区");
            return recommendIds;
        }
        Elements links = recommendZone.select(("a[data-gtm-recommend-illust-id]"));
        for(Element link : links){
            String id = link.attr("data-gtm-recommend-illust-id");
            if(id != null && !id.isEmpty()){
                recommendIds.add(id);
            }
        }

        System.out.println("【相关推荐】已查找" + maxImages + "张图片");
        return recommendIds;
    }

    /**
     * 根据 pid 获取图片详情和收藏数
     * @param pid 图片 pid
     */
    public static PixivImage getImageInfoById(String pid){
        return null;
    }
}
