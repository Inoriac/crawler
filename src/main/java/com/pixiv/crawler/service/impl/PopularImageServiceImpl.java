package com.pixiv.crawler.service.impl;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.service.PopularImageService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO：若查询到的数组为空，可能意味着搜索tag有误或者是该tag下作品较少，前者直接返回相关tag，后者需要提供一个全新的方法
public class PopularImageServiceImpl implements PopularImageService {
    public List<PixivImage> getPopularImagesByTag(String tag) throws Exception{
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(GlobalConfig.HOST, GlobalConfig.PORT));

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
}
