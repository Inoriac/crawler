package com.pixiv.crawler.main;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.SavePath;
import com.pixiv.crawler.service.Downloader;
import com.pixiv.crawler.util.JsonUtil;

import java.util.Optional;

// TODO：可以提供下载进度条(待图形化之后考虑加入这个东西)
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
            SavePath.cleanDownloadPaths();
        }));

        PixivCrawler crawler = new PixivCrawler();

//        try {
//            crawler.fetchRankingImages();
//        } catch (Exception e) {
//            System.out.println("【日榜爬取】爬取或下载过程中出错：" + e.getMessage());
//        }

        try {
            System.out.println("【相关推荐】开始运行算法...");
            System.out.println("起始图片ID: " + GlobalConfig.ARTWORK_START_PID);
            System.out.println("最大深度: " + GlobalConfig.MAX_DEPTH);
            System.out.println("每次获取图片数: " + GlobalConfig.RECOMMEND_START_IMAGES_PER_ROUND);

            crawler.downloadRecommendImages(JsonUtil.getImageInfoById(GlobalConfig.ARTWORK_START_PID), GlobalConfig.RECOMMENDATIONS_BASE_PATH);

            System.out.println("【相关推荐】算法执行完成");
        } catch (Exception e) {
            System.out.println("【相关推荐】执行过程中出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

}
