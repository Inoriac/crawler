package com.pixiv.crawler.service.impl;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.service.ArtistService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArtistServiceImpl implements ArtistService {
    public List<PixivImage> searchArtworksByArtistId(String artistId, int maxImages) throws Exception{
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(GlobalConfig.HOST, GlobalConfig.PORT));

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
}
