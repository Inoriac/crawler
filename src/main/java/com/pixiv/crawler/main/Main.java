package com.pixiv.crawler.main;

import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.service.PixivCrawler;
import com.pixiv.crawler.util.Downloader;
import com.pixiv.crawler.util.ImageDownloader;

import java.util.List;

// TODO：可以提供下载进度条(待图形化之后考虑加入这个东西)
// TODO：有关爬日榜的文件保存，按周进行保存，新的一周新建一个文件夹
public class Main {
    private static volatile boolean stopFlag = false;
    private static Downloader downloader;
    public static void main(String[] args){
        // 注册关闭钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("【crawler】 正在优雅地关闭程序...");
            stopFlag = true;

            if (downloader != null) {
                downloader.stopDownload();
            }

            // 清理所有下载路径中的.part文件
            PixivCrawler.cleanupAllDownloadPaths();
        }));

        PixivCrawler crawler = new PixivCrawler();

//        try {
//            crawler.fetchRankingImages();
//        } catch (Exception e) {
//            System.out.println("【日榜爬取】爬取或下载过程中出错：" + e.getMessage());
//        }
        try {
            System.out.println("【相关推荐】开始运行算法...");
            System.out.println("起始图片ID: " + PixivCrawlerConfig.START_PID);
            System.out.println("最大深度: " + PixivCrawlerConfig.MAX_DEPTH);
            System.out.println("每次获取图片数: " + PixivCrawlerConfig.START_IMAGES_PER_ROUND);

            crawler.downloadRecommendImages(PixivCrawlerConfig.START_PID,
                    PixivCrawlerConfig.MAX_DEPTH,
                    PixivCrawlerConfig.START_IMAGES_PER_ROUND);

            System.out.println("【相关推荐】算法执行完成");
        } catch (Exception e) {
            System.out.println("【相关推荐】执行过程中出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

}
