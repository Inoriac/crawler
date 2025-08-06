package com.pixiv.crawler.main;

import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.service.PixivCrawler;
import com.pixiv.crawler.util.ImageDownloader;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;

// TODO：目前采用双线程进行下载，后续需要修改为其他形式，如线程池等
// TODO：可以提供下载进度条
public class Main {
    private static volatile boolean stopFlag = false;
    private static Thread t1, t2;
    public static void main(String[] args){
        // 注册关闭钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("【crawler】 正在优雅地关闭程序...");
            stopFlag = true;

            if (t1 != null) t1.interrupt();
            if (t2 != null) t2.interrupt();

            ImageDownloader.deleteFile("【日榜爬取】");
        }));

        PixivCrawler crawler = new PixivCrawler();

        // 下载前五张
        try{
            List<PixivImage> images = crawler.fetchRankingImages();
            System.out.println("【日榜爬取】 共获取到" + images.size() + "张图片信息...");

            int maxThreads = 2; // 线程数
            int maxDownload = 5;    //下载前五张
            BlockingQueue<PixivImage> queue = new LinkedBlockingQueue<>();

            for(int i = 0; i < Math.min(maxDownload, images.size()); i++){
                queue.put(images.get(i));
            }

            Runnable worker = () -> {
                while(!stopFlag && !Thread.currentThread().isInterrupted()){
                    PixivImage image = queue.poll();
                    if(image == null) break;

                    String basePath = "downloads/" + image.getId();
                    String jpgPath = basePath + ".jpg";
                    String pngPath = basePath + ".png";
                    String url = image.getUrl();

                    // 检查本地是否已存在
                    File jpgFile = new File(jpgPath);
                    File pngFile = new File(pngPath);
                    if (jpgFile.exists() || pngFile.exists()) {
                        System.out.println("【日榜爬取-download】 已存在，无需重复下载：" + image.getTitle() + " (" + image.getId() + ")");
                    } else {
                        try{
                            ImageDownloader.downloadImage(url, jpgPath);
                            System.out.println("【日榜爬取-download】 已下载：" + image.getTitle() + " -> " + jpgPath);
                        } catch (Exception e){
                            // 尝试 png
                            if (e.getMessage() != null && e.getMessage().contains("404")) {
                                String pngUrl = url.replace(".jpg", ".png");
                                try {
                                    ImageDownloader.downloadImage(pngUrl, pngPath);
                                    System.out.println("【日榜爬取-download】 已下载：" + image.getTitle() + " -> " + pngPath);
                                } catch (Exception ex) {
                                    System.out.println("【日榜爬取-download】 下载失败：" + image.getTitle() + "，原因：" + ex.getMessage());
                                    System.out.println("图片链接：" + pngUrl);
                                }
                            } else {
                                System.out.println("【日榜爬取-download】 下载失败：" + image.getTitle() + "，原因：" + e.getMessage());
                                System.out.println("图片链接：" + url);
                            }
                        }
                    }
                    // 检查停止标记
                    if(stopFlag || Thread.currentThread().isInterrupted()){
                        System.out.println("【日榜爬取-thread】 线程收到停止信号，退出下载");
                        break;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        System.out.println("【日榜爬取-thread】 线程休眠时被中断，退出下载");
                        break;
                    }
                }
            };

            // 启动线程
            t1 = new Thread(worker, "downloader-1");
            t2 = new Thread(worker, "downloader-2");
            t1.start();
            t2.start();

            // 等待所有线程结束
            t1.join();
            t2.join();

            System.out.println("【日榜爬取】 所有下载任务已完成");

        } catch (Exception e){
            System.out.println("【日榜爬取】 爬取或下载过程中出错：" + e.getMessage());
        }
    }
}
