package com.pixiv.crawler.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;


public class ImageDownloader {
    /**
     * 下载图片到本地
     * @param imageUrl 图片的 URL
     * @param savePath 保存到本地的路径
     * @throws Exception 下载失败时抛出异常
     */
    public static void downloadImage(String imageUrl, String savePath) throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        String tempPath = savePath + ".part";

        Request request = new Request.Builder()
                .url(imageUrl)
                .addHeader("Referer", "https://www.pixiv.net/") // Pixiv防盗链
                .build();

        try(Response response = client.newCall(request).execute()){
            if(!response.isSuccessful()){
                throw new RuntimeException("下载失败：" + response);
            }
            InputStream inputStream = response.body().byteStream();
            FileUtils.copyInputStreamToFile(inputStream, new File(tempPath));
        }

        // 下载成功后重命名
        File tempFile = new File(tempPath);
        File finalFile = new File(savePath);
        if(!tempFile.renameTo(finalFile)){
            throw new RuntimeException("【图片下载】 重命名文件失败" + tempPath);
        }
    }

    public static void deleteFile(String funcName){
        File downloadDir = new File("downloads");
        File[] partFiles = downloadDir.listFiles((dir, name) -> name.endsWith(".part"));
        if(partFiles != null){
            for(File file : partFiles){
                if(file.delete()){
                    System.out.println(funcName + " 已删除未完成文件");
                }
            }
        }
    }

}
