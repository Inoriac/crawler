package com.pixiv.crawler.util;

import com.pixiv.crawler.model.PixivImage;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 多线程图片下载器
 */
public class Downloader {
    
    private volatile boolean stopFlag = false;
    private Thread[] threads;
    
    /**
     * 开始多线程下载任务（使用默认下载路径）
     * @param images 要下载的图片列表
     * @param threadCount 线程数
     * @param maxDownload 最大下载数量
     * @param taskName 任务名称（用于日志输出）
     * @param savePath 图片保存地址
     */
    public void startDownload(List<PixivImage> images, int threadCount, int maxDownload, String taskName, String savePath) {
        if (images == null || images.isEmpty()) {
            System.out.println("【" + taskName + "】图片列表为空，跳过下载");
            return;
        }
        
        System.out.println("【" + taskName + "】共获取到" + images.size() + "张图片信息...");
        
        // 创建任务队列
        BlockingQueue<PixivImage> queue = new LinkedBlockingQueue<>();
        
        // 将图片添加到队列中
        int downloadCount = Math.min(maxDownload, images.size());
        for (int i = 0; i < downloadCount; i++) {
            queue.offer(images.get(i));
        }
        
        // 创建下载工作线程
        Runnable worker = () -> {
            while (!stopFlag && !Thread.currentThread().isInterrupted()) {
                PixivImage image = queue.poll();
                if (image == null) break;
                
                downloadSingleImage(image, taskName, savePath);
                
                // 检查停止标记
                if (stopFlag || Thread.currentThread().isInterrupted()) {
                    System.out.println("【" + taskName + "-thread】线程收到停止信号，退出下载");
                    break;
                }
                
                // 线程间延迟
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.out.println("【" + taskName + "-thread】线程休眠时被中断，退出下载");
                    break;
                }
            }
        };
        
        // 启动线程
        threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(worker, "downloader-" + (i + 1));
            threads[i].start();
        }
        
        // 等待所有线程结束
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.out.println("【" + taskName + "】等待线程结束时被中断");
            stopDownload();
        }
        
        System.out.println("【" + taskName + "】所有下载任务已完成");
    }
    
    /**
     * 下载单张图片
     * @param image 图片信息
     * @param taskName 任务名称
     * @param savePath 保存路径
     */
    private void downloadSingleImage(PixivImage image, String taskName, String savePath) {
        // 确保下载目录存在
        File saveDir = new File(savePath);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        
        String basePath = savePath + "/" + image.getId();
        String jpgPath = basePath + ".jpg";
        String pngPath = basePath + ".png";
        String url = image.getUrl();
        
        // 检查本地是否已存在
        File jpgFile = new File(jpgPath);
        File pngFile = new File(pngPath);
        if (jpgFile.exists() || pngFile.exists()) {
            System.out.println("【" + taskName + "-download】已存在，无需重复下载：" + image.getTitle() + " (" + image.getId() + ")");
            return;
        }
        
        try {
            ImageDownloader.downloadImage(url, jpgPath);
            System.out.println("【" + taskName + "-download】已下载：" + image.getTitle() + " -> " + jpgPath);
        } catch (Exception e) {
            // 尝试 png
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                String pngUrl = url.replace(".jpg", ".png");
                try {
                    ImageDownloader.downloadImage(pngUrl, pngPath);
                    System.out.println("【" + taskName + "-download】已下载：" + image.getTitle() + " -> " + pngPath);
                } catch (Exception ex) {
                    System.out.println("【" + taskName + "-download】下载失败：" + image.getTitle() + "，原因：" + ex.getMessage());
                    System.out.println("图片链接：" + pngUrl);
                }
            } else {
                System.out.println("【" + taskName + "-download】下载失败：" + image.getTitle() + "，原因：" + e.getMessage());
                System.out.println("图片链接：" + url);
            }
        }
    }
    
    /**
     * 停止下载任务
     */
    public void stopDownload() {
        stopFlag = true;
        if (threads != null) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
        }
    }
    
    /**
     * 检查是否正在下载
     * @return 是否正在下载
     */
    public boolean isDownloading() {
        if (threads == null) return false;
        for (Thread thread : threads) {
            if (thread.isAlive()) return true;
        }
        return false;
    }
}
