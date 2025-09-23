package com.pixiv.crawler.service.impl;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.model.SavePath;
import com.pixiv.crawler.util.ImageDownloader;

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
     * @param taskName 任务名称（用于日志输出）
     * @param savePath 图片保存地址
     */
    public void startDownload(List<PixivImage> images, String taskName, String savePath) {
        if (images == null || images.isEmpty()) {
            System.out.println("【" + taskName + "】图片列表为空，跳过下载");
            return;
        }

        SavePath.addPath(savePath);

        System.out.println("【" + taskName + "】共获取到" + images.size() + "张图片信息...");
        
        // 创建任务队列
        BlockingQueue<PixivImage> queue = new LinkedBlockingQueue<>();

        // 将图片添加到队列中
        for (int i = 0; i < images.size(); i++) {
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
                
                // 线程间延迟（3-5秒随机）
                try {
                    int sleepTime = 3000 + (int)(Math.random() * 2000); // 3000-5000ms
                    System.out.println("【" + taskName + "-thread】休眠 " + (sleepTime/1000) + " 秒后继续下载...");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    System.out.println("【" + taskName + "-thread】线程休眠时被中断，退出下载");
                    break;
                }
            }
        };
        
        // 启动线程
        threads = new Thread[GlobalConfig.THREAD_COUNT];
        for (int i = 0; i < GlobalConfig.THREAD_COUNT; i++) {
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
     * 下载单张图片（支持多页）
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
        
        int pageCount = image.getPageCount();
        System.out.println("【" + taskName + "-download】开始下载作品：" + image.getTitle() + " (" + image.getId() + ")，共 " + pageCount + " 页");
        
        // 如果只有1页，直接下载到主目录，不创建子文件夹
        if (pageCount == 1) {
            String basePath = savePath + "/" + image.getId();
            String jpgPath = basePath + ".jpg";
            String pngPath = basePath + ".png";
            
            // 检查是否已经下载过
            File jpgFile = new File(jpgPath);
            File pngFile = new File(pngPath);
            if (jpgFile.exists() || pngFile.exists()) {
                System.out.println("【" + taskName + "-download】已存在，无需重复下载：" + image.getTitle() + " (" + image.getId() + ")");
                return;
            }
            
            try {
                ImageDownloader.downloadImage(image.getUrl(), jpgPath);
                System.out.println("【" + taskName + "-download】已下载：" + image.getTitle() + " -> " + jpgPath);
            } catch (Exception e) {
                // 尝试 png
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    String pngUrl = image.getUrl().replace(".jpg", ".png");
                    try {
                        ImageDownloader.downloadImage(pngUrl, pngPath);
                        System.out.println("【" + taskName + "-download】已下载：" + image.getTitle() + " -> " + pngPath);
                    } catch (Exception ex) {
                        System.out.println("【" + taskName + "-download】下载失败：" + image.getTitle() + "，原因：" + ex.getMessage());
                        System.out.println("图片链接：" + pngUrl);
                    }
                } else {
                    System.out.println("【" + taskName + "-download】下载失败：" + image.getTitle() + "，原因：" + e.getMessage());
                    System.out.println("图片链接：" + image.getUrl());
                }
            }
        } else {
            // 多页作品，创建独立文件夹
            String workDir = savePath + "/" + image.getId();
            File workFolder = new File(workDir);
            if (!workFolder.exists()) {
                workFolder.mkdirs();
            }
            
            // 检查是否已经下载过（检查文件夹是否存在且包含文件）
            if (workFolder.exists() && workFolder.listFiles() != null && workFolder.listFiles().length > 0) {
                System.out.println("【" + taskName + "-download】已存在，无需重复下载：" + image.getTitle() + " (" + image.getId() + ")");
                return;
            }
            
            // 下载所有页面
            for (int i = 0; i < pageCount; i++) {
                // 构造当前页面的URL
                String url = constructPageUrl(image.getUrl(), i);
                
                String basePath = workDir + "/" + image.getId() + "_p" + i;
                String jpgPath = basePath + ".jpg";
                String pngPath = basePath + ".png";
                
                try {
                    ImageDownloader.downloadImage(url, jpgPath);
                    System.out.println("【" + taskName + "-download】已下载第" + (i + 1) + "页：" + image.getTitle() + " -> " + jpgPath);
                } catch (Exception e) {
                    // 尝试 png
                    if (e.getMessage() != null && e.getMessage().contains("404")) {
                        String pngUrl = url.replace(".jpg", ".png");
                        try {
                            ImageDownloader.downloadImage(pngUrl, pngPath);
                            System.out.println("【" + taskName + "-download】已下载第" + (i + 1) + "页：" + image.getTitle() + " -> " + pngPath);
                        } catch (Exception ex) {
                            System.out.println("【" + taskName + "-download】下载第" + (i + 1) + "页失败：" + image.getTitle() + "，原因：" + ex.getMessage());
                            System.out.println("图片链接：" + pngUrl);
                        }
                    } else {
                        System.out.println("【" + taskName + "-download】下载第" + (i + 1) + "页失败：" + image.getTitle() + "，原因：" + e.getMessage());
                        System.out.println("图片链接：" + url);
                    }
                }
            }
        }
        
        System.out.println("【" + taskName + "-download】作品下载完成：" + image.getTitle() + " (" + image.getId() + ")");
    }
    
    /**
     * 根据基础URL和页面索引构造页面URL
     * @param baseUrl 基础URL（通常是p0的URL）
     * @param pageIndex 页面索引（0, 1, 2, ...）
     * @return 页面URL
     */
    private String constructPageUrl(String baseUrl, int pageIndex) {
        if (pageIndex == 0) {
            return baseUrl; // 第一页直接使用基础URL
        }
        
        // 替换URL中的页面编号
        return baseUrl.replace("_p0.jpg", "_p" + pageIndex + ".jpg");
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
