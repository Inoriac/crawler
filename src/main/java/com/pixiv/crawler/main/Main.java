package com.pixiv.crawler.main;

import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.service.PixivCrawler;
import com.pixiv.crawler.util.ImageDownloader;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args){
        PixivCrawler crawler = new PixivCrawler();
        try{
            // 获取 Pixiv 热门图片信息
            List<PixivImage> images = crawler.fetchRankingImages();
            System.out.println("共获取到" + images.size() + "张图片信息...");

            // 下载前 5 张图片作为示例
            int count = 0;

            for(PixivImage image : images){
                if(count >= 5) break;

                String basePath = "downloads/" + image.getId();
                String jpgPath = basePath + ".jpg";
                String pngPath = basePath + ".png";
                String url = image.getUrl();

                // 检查本地是否已存在
                File jpgFile = new File(jpgPath);
                File pngFile = new File(pngPath);
                if (jpgFile.exists() || pngFile.exists()) {
                    System.out.println("已存在，无需重复下载：" + image.getTitle() + " (" + image.getId() + ")");
                    count++;
                    continue;
                }

                // 尝试进行下载
                try{
                    ImageDownloader.downloadImage(url, jpgPath);
                    System.out.println("已下载：" + image.getTitle() + " -> " + jpgPath);
                } catch (Exception e){
                    // 如果是404，尝试png
                    if (e.getMessage() != null && e.getMessage().contains("404")) {
                        String pngUrl = url.replace(".jpg", ".png");
                        try {
                            ImageDownloader.downloadImage(pngUrl, pngPath);
                            System.out.println("已下载：" + image.getTitle() + " -> " + pngPath);
                        } catch (Exception ex) {
                            System.out.println("下载失败：" + image.getTitle() + "，原因：" + ex.getMessage());
                            System.out.println("图片链接：" + pngUrl);
                        }
                    } else {
                        System.out.println("下载失败：" + image.getTitle() + "，原因：" + e.getMessage());
                        System.out.println("图片链接：" + url);
                    }
                }
                count ++;
                // 延时 2s
                Thread.sleep(2000);
            }
        } catch (Exception e){
            System.out.println("爬取或下载过程中出错：" + e.getMessage());
        }
    }
}
