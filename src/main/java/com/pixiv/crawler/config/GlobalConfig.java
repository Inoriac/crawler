package com.pixiv.crawler.config;

// TODO：在整个程序启动的时候，需要检查各个概率之和是否为1
// TODO：可以提供一个排除AI作品的开关(实现思路类似于检测R-18标签)
public class GlobalConfig {
    // 默认主机地址
    public static final String HOST = "127.0.0.1";
    // VPN 端口号
    public static int PORT = 7897;
    // 用户p站的cookie，用于绕过反爬
    public static String COOKIE = "first_visit_datetime_pc=2025-01-24%2013%3A39%3A04; yuid_b=MCeXORk; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=129495109; __utmz=235335808.1749878658.13.8.utmcsr=t.co|utmccn=(referral)|utmcmd=referral|utmcct=/FL4mTt3PlR; PHPSESSID=118685673_LVep8oNv3HJHw5ZXhrhkC8UXqS4JhYib; device_token=7aac299b90b54c3a3aba9ca0e3e2b3fd; privacy_policy_agreement=7; _ga_MZ1NL4PHH0=GS2.1.s1754467663$o2$g1$t1754467960$j36$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=1; login_ever=yes; __utmv=235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^9=p_ab_id=8=1^10=p_ab_id_2=8=1; AMZN-Token=v2FweIBzMlVqNmZFRlhwRVpRanRtR21rOWxBVXIyWDQrNlJYQ29qbU5OcDhYamtSSUtIR0s4Y3k3S2VTK29QeEdsSjVCSlhCNTI4R3d2NENyQlVYYytrMFVuSzgveUJ3ZEUzcG1uaVZjbjV0d25XMkVadUg5V1BocG5GQmRsUW5yQXFNbWJrdgFiaXZ4IDc3KzlEKysvdmUrL3ZlKy92ZSsvdmUrL3ZkaUNIRFE9/w==; _gcl_au=1.1.551567821.1754808878; _ga=GA1.1.509352844.1737693546; cto_bundle=-FTUA19VcUlIYlVqQUZuTzhvamJGeFpxMm1ubFdmN0RLaVA0dlAlMkZEb2wxQzglMkJXJTJCM3Z6N3JqMGdIWUF4a1IxQkFTNzM4QmF6MFFaNGhFbiUyRmN2WHhOV21TOUxTUkU4OUpDUWpod3J5SEpBdnNCbWJadk84bXlpOFVMZHRPVGRYVExkVG81R3RVcE95eVpDbkVZSkxuSWNYVm80QSUzRCUzRA; FCNEC=%5B%5B%22AKsRol8qbptErXAx73NK8VvVzFXha_aW1KEnTG5aj52WmXmh9MIZOh161vm3jcbytNGnAda5grwOt6ndcC8K25Ja6acfRukWYcilnMFADYFYKx8CS2TDo1xQsoZ35nriIrFc8V8HGkEUxJPFzKmTC8NGdk-9i0BvTA%3D%3D%22%5D%5D; _cfuvid=WPi99y7QRfVotOMU4hwaW0NWeaQJKYUdML8lGS38ibQ-1755086486094-0.0.1.1-604800000; __utma=235335808.102379013.1737693545.1755083446.1755086489.21; __utmc=235335808; __utmt=1; __utmb=235335808.1.10.1755086489; cf_clearance=0aXyQBcAotZbTzqS_LklUxM6OANEQtwcJ2.P.fm_WJw-1755086489-1.2.1.1-UxR0rUQmioeIXrGNCD8N.p5P_tkaZqd9eze3QsiNZFUdX0KOeltcPm.iYyKZFg5RpFvNSDJ8AIyb5GHiQKN3ORV2cGuLwPC57JLxN8pYyZIRXopS0GMMPhMunIIgCWGg2foRBH7h7CcjuEhH10pSZKHbjgLT7BS_8uFXHCHkwO1LKqtTdd3ru4TDpzeEhgm8D7yu4bH6c5kCPt0bdCR1U2R6M1aJGoEiDU6nB3w0dWA; _ga_75BBYNYN9J=GS2.1.s1755086488$o24$g1$t1755086494$j54$l0$h0";
    // 目标画师 id
    public static String ARTIST_START_ID = "1960050";
    // 起始图片 id
    public static String ARTWORK_START_PID = "133558043";

    // 各队列的选取倾向概率
    public static double TOP1W_SELECTION_PROBABILITY = 0.25;
    public static double TOP5K_SELECTION_PROBABILITY = 0.3;
    public static double TOP3K_SELECTION_PROBABILITY = 0.3;
    public static double TOP1K_SELECTION_PROBABILITY = 0.15;

    // 搜索深度
    public static int MAX_DEPTH = 4;
    // 每轮选择的起始图片数量
    public static int RECOMMEND_START_IMAGES_PER_ROUND = 3;
    // 每个作品最大下载图片数量
    public static int MAX_IMAGES_PER_WORK = 5;
    // 队列满时的处理阈值
    public static int QUEUE_PROCESS_THRESHOLD = 10;
    // 推荐图片单次获取数量(推荐值在25左右，不可超过100，值越大，运行时间越长)
    public static int PER_RECOMMEND_MAX_IMAGE = 32;
    // 获取画师图片数量
    public static int ARTIST_MAX_IMAGE = 30;

    // 最大下载线程数
    public static int THREAD_COUNT = 5;
    // 基础下载路径
    public static String BASE_SAVE_PATH = "E:/crawler/downloads";
    // 日榜图片基础下载路径（不包含周文件夹）
    public static String RANKING_BASE_PATH = BASE_SAVE_PATH + "/ranking";
    // 相关推荐图片基础下载路径（不包含日期和收藏数文件夹）
    public static String RECOMMENDATIONS_BASE_PATH = BASE_SAVE_PATH + "/recommendations";
    // 画师作品基础保存路径
    public static String ARTIST_BASE_PATH = BASE_SAVE_PATH + "/artists";
    // 热门作品基础保存路径
    public static String POPULAR_BASE_PATH = BASE_SAVE_PATH + "/popular";
    // 相关推荐图片按收藏数分类的文件夹名称
    public static final String TOP1W_FOLDER = "1w+";
    public static final String TOP5K_FOLDER = "5k-1w";
    public static final String TOP3K_FOLDER = "3k-5k";
    public static final String TOP1K_FOLDER = "1k-3k";

    // R-18下载开关
    public static boolean R18_DOWNLOAD_ENABLED = true;
    // R-18作品文件夹名称
    public static final String R18_FOLDER = "r-18";
    // 普通作品文件夹名称
    public static final String NORMAL_FOLDER = "normal";
    
    // 漫画排除开关
    public static boolean MANGA_EXCLUDE_ENABLED = true;
    // 漫画标签关键词
    public static String MANGA_TAG_KEYWORD = "漫画";
    
    // 网络配置
    public static int CONNECT_TIMEOUT = 10; // 连接超时时间（秒）
    public static int READ_TIMEOUT = 120; // 读取超时时间（秒）
    public static int RETRY_COUNT = 3; // 重试次数
    
    // 下载配置
    public static boolean AUTO_CLEAN_PART_FILES = true; // 自动清理.part文件
    public static boolean SHOW_DOWNLOAD_PROGRESS = true; // 显示下载进度

    // 图片识别tag服务相关
    public static int TAG_SERVICE_PORT = 8000;  // 本地tag识别服务端口
    public static double TAG_SERVICE_THRESHOLD = 0.35;  // 控制tag识别的效果，0.35为佳
    public static String TAG_SERVICE_URL_REMOTE = "";   // 远程api调用 留空，目前没有找到可调用的稳定免费api
    public static double TAG_PROBABILITY = 0.5; // 选用tag的最低出现概率值
    public static double TAG_FINAL_PROB = 0.4;  // 持久化 tagMap 中tag的最低概率值
    public static String TAG_SERVICE_JSON_NAME = "tags.json";   // 用户持久化偏好文件名
    public static int TAG_SERVICE_JSON_FILE_MAX_NUMBER = 10;    // 最大持久化备份数
    public static String SEARCH_API_PRE = "https://www.pixiv.net/rpc/cps.php?keyword="; // url 前缀
    public static String SEARCH_API_SUF = "&lang=zh";   // url 后缀
    public static double SEARCH_API_INTERVAL = 0.1; // 使用搜索api的间隔

    public static String TAG_SERVICE_URL_LOCAL = "http://127.0.0.1:" + TAG_SERVICE_PORT + "/predict?threshold=" + TAG_SERVICE_THRESHOLD;    // 本地服务请求地址
    public static  String PIXIV_RANKING_URL = "https://www.pixiv.net/ranking.php?mode=daily&content=illust";    // 日榜作品获取地址
    public static String LOCAL_MAPPING_URL = "/mapping.json"; // 本地映射用json路径
    public static double PUNISHMENT = 0.11; // 惩罚强度


    // 验证概率总和是否为1
    public static boolean validateProbabilities() {
        double sum = TOP1W_SELECTION_PROBABILITY + TOP5K_SELECTION_PROBABILITY + 
                    TOP3K_SELECTION_PROBABILITY + TOP1K_SELECTION_PROBABILITY;
        return Math.abs(sum - 1.0) < 0.001;
    }
    
    // 更新基础路径时同步更新其他路径
    public static void updateBasePath(String newBasePath) {
        BASE_SAVE_PATH = newBasePath;
        RANKING_BASE_PATH = BASE_SAVE_PATH + "/ranking";
        RECOMMENDATIONS_BASE_PATH = BASE_SAVE_PATH + "/recommendations";
        ARTIST_BASE_PATH = BASE_SAVE_PATH + "/artists";
        POPULAR_BASE_PATH = BASE_SAVE_PATH + "/popular";
    }
}
