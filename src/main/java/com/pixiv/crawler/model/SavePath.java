package com.pixiv.crawler.model;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.util.DateUtils;
import com.pixiv.crawler.util.ImageDownloader;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SavePath {
    // 记录所有提交了下载任务的保存路径，用于程序结束时清理.part文件
    private static Set<String> downloadPaths = new HashSet<>();

    public static Set<String> getDownloadPaths() {
        return downloadPaths;
    }

    public static void addPath(String savePath){
        downloadPaths.add(savePath);
        System.out.println("【记录】添加下载路径: " + savePath);
    }

    public static void cleanDownloadPaths(){
        System.out.println("【清理】开始清理所有下载路径中的.part文件...");

        // 1. 清理已记录的路径
        for (String path : downloadPaths) {
            try {
                ImageDownloader.deleteFile("【清理-" + path + "】", path);
                System.out.println("【清理】已清理路径: " + path);
            } catch (Exception e) {
                System.out.println("【清理】清理路径 " + path + " 时出错: " + e.getMessage());
            }
        }

        // 2. 清理相关推荐的基础路径（按日期和收藏数分类）
//        cleanupRecommendationsPaths();

        System.out.println("【清理】所有下载路径清理完成");
    }

    /**
     * 清理相关推荐路径中的所有.part文件
     */
    private static void cleanupRecommendationsPaths() {
        try {
            String currentDate = DateUtils.getCurrentDate();
            String recommendationsBasePath = GlobalConfig.RECOMMENDATIONS_BASE_PATH;

            // 清理当前日期的相关推荐路径
            String[] folderNames = {
                    GlobalConfig.TOP1W_FOLDER,
                    GlobalConfig.TOP5K_FOLDER,
                    GlobalConfig.TOP3K_FOLDER,
                    GlobalConfig.TOP1K_FOLDER
            };

            for (String folderName : folderNames) {
                // 清理normal文件夹
                String normalPath = recommendationsBasePath + "/" + currentDate + "/" + folderName + "/" + GlobalConfig.NORMAL_FOLDER;
                try {
                    ImageDownloader.deleteFile("【清理相关推荐-" + folderName + "-normal】", normalPath);
                    System.out.println("【清理】已清理相关推荐normal路径: " + normalPath);
                } catch (Exception e) {
                    System.out.println("【清理】清理相关推荐normal路径 " + normalPath + " 时出错: " + e.getMessage());
                }

                // 清理R-18文件夹
                String r18Path = recommendationsBasePath + "/" + currentDate + "/" + folderName + "/" + GlobalConfig.R18_FOLDER;
                try {
                    ImageDownloader.deleteFile("【清理相关推荐-" + folderName + "-r18】", r18Path);
                    System.out.println("【清理】已清理相关推荐r-18路径: " + r18Path);
                } catch (Exception e) {
                    System.out.println("【清理】清理相关推荐r-18路径 " + r18Path + " 时出错: " + e.getMessage());
                }
            }

            // 3. 额外清理：扫描整个相关推荐目录，清理所有.part文件
            cleanupAllPartFilesInDirectory(recommendationsBasePath);

        } catch (Exception e) {
            System.out.println("【清理】清理相关推荐路径时出错: " + e.getMessage());
        }
    }

    /**
     * 递归清理指定目录及其子目录中的所有.part文件
     */
    private static void cleanupAllPartFilesInDirectory(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                return;
            }

            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 递归清理子目录
                        cleanupAllPartFilesInDirectory(file.getAbsolutePath());
                    } else if (file.getName().endsWith(".part")) {
                        // 删除.part文件
                        if (file.delete()) {
                            System.out.println("【清理】删除.part文件: " + file.getAbsolutePath());
                        } else {
                            System.out.println("【清理】删除.part文件失败: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("【清理】递归清理目录 " + directoryPath + " 时出错: " + e.getMessage());
        }
    }

}
