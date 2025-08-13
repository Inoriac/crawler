package com.pixiv.crawler.config;

// TODO：在整个程序启动的时候，需要检查各个概率之和是否为1
public class PixivCrawlerConfig {
    // 各队列的选取倾向概率
    public static final double TOP1W_SELECTION_PROBABILITY = 0.5;
    public static final double TOP5K_SELECTION_PROBABILITY = 0.3;
    public static final double TOP3K_SELECTION_PROBABILITY = 0.2;

    // 起始图片 id
    public static final String START_PID = "133461036";
    // 搜索深度
    public static final int MAX_DEPTH = 4;
    // 每轮选择的起始图片数量
    public static final int START_IMAGES_PER_ROUND = 3;
    // 队列满时的处理阈值
    public static final int QUEUE_PROCESS_THRESHOLD = 10;
    
    // 基础下载路径
    public static final String BASE_SAVE_PATH = "downloads";
    // 日榜图片下载路径
    public static final String RANKING_SAVE_PATH = BASE_SAVE_PATH + "/ranking";
    // 相关推荐图片下载路径
    public static final String RECOMMENDATIONS_SAVE_PATH = BASE_SAVE_PATH + "/recommendations";
}
