package com.pixiv.crawler.service;

import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.main.Main;
import com.pixiv.crawler.util.DateUtils;
import com.pixiv.crawler.util.Downloader;
import com.pixiv.crawler.util.ImageDownloader;
import com.pixiv.crawler.util.PixivRecHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.pixiv.crawler.model.PixivImage;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pixiv 爬虫
 */
public class PixivCrawler {
    private static final String PIXIV_RANKING_URL = "https://www.pixiv.net/ranking.php?mode=daily&content=illust";
    private Downloader downloader;
    private PixivCrawlerConfig crawlerConfig;
    
    // 记录所有提交了下载任务的保存路径，用于程序结束时清理.part文件
    private static Set<String> downloadPaths = new HashSet<>();
    
    public PixivCrawler() {
        this.downloader = new Downloader();
    }

    // 获取并下载 Pixiv 日榜图片
    public void fetchRankingImages() throws Exception {
        List<PixivImage> images = new ArrayList<>();
        // 创建代理对象
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));

        System.out.println("尝试访问排行榜页面...");

        // 访问 Pixiv 排行榜页面
        Document document = Jsoup.connect(PIXIV_RANKING_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .header("Cookie", "first_visit_datetime_pc=2025-01-24%2013%3A39%3A04; yuid_b=MCeXORk; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=129495109; _ga=GA1.1.509352844.1737693546; __utmz=235335808.1749878658.13.8.utmcsr=t.co|utmccn=(referral)|utmcmd=referral|utmcct=/FL4mTt3PlR; cc1=2025-08-06%2017%3A07%3A38; _cfuvid=dhu66.SQnP2SeQqDYX1NQPH6AvHaHtI_kcIF2iR8FHU-1754467658283-0.0.1.1-604800000; __utma=235335808.102379013.1737693545.1749878658.1754467660.14; __utmc=235335808; PHPSESSID=118685673_LVep8oNv3HJHw5ZXhrhkC8UXqS4JhYib; device_token=7aac299b90b54c3a3aba9ca0e3e2b3fd; privacy_policy_agreement=7; __cf_bm=plID.4OFYNfMroRUjYF8mrOUl6593QgUkXvVil7LgxI-1754467961-1.0.1.1-uLg7Wc6vakjhBmK1Rspog0B9k9cOhmmm9CnpDzgrqTwiqLj3sZPIlvOlB.A1f1_FvHt5Lb0V3CCyG1tO33wDiuZwDViLOAknuPC9d1t3NuV9YOgGTFhc_IsnQ1y88fT1sUWAf6INGqU_rreXVQyqTw; _ga_MZ1NL4PHH0=GS2.1.s1754467663$o2$g1$t1754467960$j36$l0$h0; c_type=22; privacy_policy_notification=0; a_type=0; b_type=1; cto_bundle=-0N60F9VcUlIYlVqQUZuTzhvamJGeFpxMm1xZDJvWFJIbG5vd3NtejBBcE9hayUyQnZwdDFZQ3d5bnBjdmhCWGhiaDFBTnlxdk1ONjRCTzhYUFJaMFd0ZWh3cUxlUnMlMkZITDA2RlA1JTJGSnp2M2dxSzZjSmFjWjZHNHJ2TFlUaWJnZHhvRXZTUkdTWUszVTdzQkF5eFdYbVk5NW5UUEElM0QlM0Q; __utmt=1; cf_clearance=ZjAvoq.fB0OvSfzra0.4btF7CQsGtUwK8d72wFEPzdk-1754468388-1.2.1.1-mQAtBJq6TR4o.DQAjzKmy6Kf_nfxJ6kBfCrVUqmkA8iuaXksLCas.pc46m0jTMUQto0e32wlkVi_6dt1AtpuItEjeRrwlSyN5L3l1YYGklI6qg96ab02Hs.3oFJvpLCv_abiKG1KJnVBAPnfA_Uy8jPcW.eAKrKhGs0KyzqqGqc.ddkmykgZhmiRgMC5iIBYFF3MjaZ3pjBUxeuvuIXOJWBsHSLOtdPdavhNKOI60MQ; FCNEC=%5B%5B%22AKsRol98GxziMSGoioFiuERYQricQBB31ShRqusG9woungPV30ba-ipsGq-EAAEvdvZ0LTn6N3Zse-ncHadmKoDoNN0U8mQ1WCdVwJd4BgZ67zj5STxul6UQEC1mvNE7X51VnbEeMyKh_ofpVkFYdCIHJ9ustrHJIQ%3D%3D%22%5D%5D; login_ever=yes; __utmv=235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^6=user_id=118685673=1^9=p_ab_id=8=1^10=p_ab_id_2=8=1^11=lang=zh=1; __utmb=235335808.4.10.1754467660; _ga_75BBYNYN9J=GS2.1.s1754467660$o15$g1$t1754468463$j59$l0$h0")
                .proxy(proxy)
                .get();

        // 解析页面，提取图片信息
        Elements items = document.select("section.ranking-item");

        for (Element item : items) {
            String id = item.attr("data-id");
            Element titleElem = item.selectFirst("h2 > a.title");
            Element artistElem = item.selectFirst("a.user-container span.user-name");
            if (titleElem == null || artistElem == null) {
                System.out.println("未找到title或artist，item内容：" + item.html());
                continue;
            }
            String title = titleElem.text();
            String artist = artistElem.text();

            PixivImage image = new PixivImage();
            image.setId(id);
            image.setTitle(title);
            image.setArtist(artist);

            // 尝试获取原图下载链接
            Element imgElem = item.selectFirst("img[data-src]");
            if (imgElem != null) {
                String thumbUrl = imgElem.attr("data-src");
                String originalUrl = thumbUrl
                        .replace("c/240x480/img-master", "img-original")
                        .replace("_master1200", "");
                image.setUrl(originalUrl);
            }

            images.add(image);
        }

        // 获取当前周的文件夹名称
        String weekFolderName = DateUtils.getCurrentWeekFolderName();
        String rankingSavePath = PixivCrawlerConfig.RANKING_BASE_PATH + "/" + weekFolderName;
        
        System.out.println("【日榜下载】当前周文件夹: " + weekFolderName);
        System.out.println("【日榜下载】完整下载路径: " + rankingSavePath);
        
        // 记录下载路径
        addDownloadPath(rankingSavePath);
        downloader.startDownload(images, 2, 5, "日榜爬取", rankingSavePath);
    }

    /**
     * 从指定图片ID出发，爬取相关图片，按收藏数分档，并提交下载任务
     *
     * @param startPid  起始图片 pid
     * @param maxDepth  最大深度 应该不超过4层
     * @param maxImages 单次最多获取图片数
     */
    public void downloadRecommendImages(String startPid, int maxDepth, int maxImages) throws Exception {
        Set<String> visited = new HashSet<>();  // 防止选取同样的图片作为起始
        Queue<String> queue = new LinkedList<>();
        queue.add(startPid);

        // 三个优先队列，按照收藏数降序
        PriorityQueue<PixivImage> top1w = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());
        PriorityQueue<PixivImage> top5k = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());
        PriorityQueue<PixivImage> top3k = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());

        // 存储所有推荐图片的完整信息，用于后续选择
        Map<String, PixivImage> allRecommendImages = new HashMap<>();

        // 当前层的待选起始图片
        List<String> currentStartImages = new ArrayList<>();
        currentStartImages.add(startPid);

        int depth = 0;
        while (!queue.isEmpty() && depth < maxDepth) {
            System.out.println("【第" + (depth + 1) + "层】开始处理，起始图片数量: " + currentStartImages.size());

            // 下一层的起始图片候选
            List<String> nextStartCandidates = new ArrayList<>();

            for(String pid : currentStartImages){
                // 跳过已爬取
                if (visited.contains(pid)) continue;
                visited.add(pid);

                // 获取图片详情页
                PixivImage image = PixivRecHelper.getImageInfoById(pid);
                if (image == null) continue;

                // 仅 depth=0 时需要进行一次分类
                if(depth == 0){
                    int fav = image.getBookmarkCount();
                    if (fav >= 10000) {
                        top1w.add(image);
                    } else if (fav >= 5000) {
                        top5k.add(image);
                    } else if (fav >= 3000) {
                        top3k.add(image);
                    } else {
                        System.out.println("【相关推荐】 收藏数低于3k，已弃用(" + image.getId() + ")");
                    }
                }

                // 获取相关推荐图片完整信息
                List<PixivImage> recImages = PixivRecHelper.getRecommendImagesByPid(pid, 20); // 获取20张推荐图片
                System.out.println("【相关推荐】获取到 " + recImages.size() + " 张推荐图片");
                
                // 将推荐图片直接添加到对应的队列中，并收集ID用于下一轮
                for (PixivImage recImage : recImages) {
                    nextStartCandidates.add(recImage.getId());
                    allRecommendImages.put(recImage.getId(), recImage); // 保存完整信息
                    
                    // 通过API获取收藏数
                    try {
                        int fav = PixivRecHelper.getBookmarkCountFromApi(recImage.getId());
                        recImage.setBookmarkCount(fav);
                        System.out.println("【收藏数】" + recImage.getId() + " -> " + fav);
                    } catch (Exception e) {
                        System.out.println("【收藏数】获取失败: " + recImage.getId() + " - " + e.getMessage());
                        recImage.setBookmarkCount(0);
                    }
                    
                    // 直接分类推荐图片到对应队列
                    int fav = recImage.getBookmarkCount();
                    if (fav >= 10000) {
                        top1w.add(recImage);
                        System.out.println("【分类】" + recImage.getId() + " -> 1w+队列 (收藏数: " + fav + ")");
                    } else if (fav >= 5000) {
                        top5k.add(recImage);
                        System.out.println("【分类】" + recImage.getId() + " -> 5k~1w队列 (收藏数: " + fav + ")");
                    } else if (fav >= 3000) {
                        top3k.add(recImage);
                        System.out.println("【分类】" + recImage.getId() + " -> 3k~5k队列 (收藏数: " + fav + ")");
                    } else {
                        System.out.println("【分类】" + recImage.getId() + " -> 收藏数低于3k，舍弃 (收藏数: " + fav + ")");
                    }
                }
            }
            // 检查并处理每个队列
            processQueue(top1w, "1w+", nextStartCandidates);
            processQueue(top5k, "5k~1w", nextStartCandidates);
            processQueue(top3k, "3k~5k", nextStartCandidates);

            //为下一层选择三张起始图片
            currentStartImages = selectNextStartImages(nextStartCandidates, visited, allRecommendImages);

            depth++;
        }

        System.out.println("【算法结束】提交剩余队列的下载任务...");
        submitQueueDownload(top1w,  "1w+");
        submitQueueDownload(top5k,  "5k~1w");
        submitQueueDownload(top3k,  "3k~5k");

    }

    // 队列满时提交下载任务
    private void processQueue(PriorityQueue<PixivImage> queue, String tag, List<String> nextStartCandidates) throws Exception {
        if(queue.size() >= PixivCrawlerConfig.QUEUE_PROCESS_THRESHOLD){
            System.out.println("【" + tag + "】" + "队列满" + PixivCrawlerConfig.QUEUE_PROCESS_THRESHOLD + "，创建下载任务...");
            // 转换为 list 以便按索引访问
            List<PixivImage> queueList = new ArrayList<>(queue);

            // 根据tag确定下载路径
            String savePath = getRecommendationsSavePath(tag);
            
            // 记录下载路径
            addDownloadPath(savePath);
            // 使用下载器进行多线程下载
            downloader.startDownload(queueList, 2, queueList.size(), "相关推荐-" + tag, savePath);
            
            // 清空队列
            queue.clear();
        }
    }

    // 提交队列下载任务(结束时调用)
    private void submitQueueDownload(PriorityQueue<PixivImage> queue, String tag) throws Exception {
        if(!queue.isEmpty()){
            System.out.println("【" + tag + "】提交剩余" + queue.size() + "张图片的下载任务...");
            List<PixivImage> queueList = new ArrayList<>(queue);
            
            // 根据tag确定下载路径
            String savePath = getRecommendationsSavePath(tag);
            
            // 记录下载路径
            addDownloadPath(savePath);
            // 使用下载器进行多线程下载
            downloader.startDownload(queueList, 2, queueList.size(), "下载-" + tag, savePath);
        }
    }

    // 从候选图片中选择下一轮的起始图片
    private List<String> selectNextStartImages(List<String> candidates, Set<String> visited,
                                             Map<String, PixivImage> allRecommendImages) throws Exception {
        List<String> selected = new ArrayList<>();
        Random random = new Random();

        // 过滤已访问的图片
        List<String> availableCandidates = candidates.stream()
                .filter(id -> !visited.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if(availableCandidates.isEmpty()){
            System.out.println("【调试】没有可用的候选图片");
            return selected;
        }

        System.out.println("【调试】候选图片总数: " + candidates.size());
        System.out.println("【调试】已访问图片数: " + visited.size());
        System.out.println("【调试】过滤后可用候选图片数: " + availableCandidates.size());

        // 将候选图片按收藏数分类
        List<String> top1wCandidates = new ArrayList<>();
        List<String> top5kCandidates = new ArrayList<>();
        List<String> top3kCandidates = new ArrayList<>();

        // 从已保存的推荐图片信息中分类
        for (String candidateId : availableCandidates) {
            PixivImage image = allRecommendImages.get(candidateId);
            if (image != null) {
                int fav = image.getBookmarkCount();
                if (fav >= 10000) {
                    top1wCandidates.add(candidateId);
                } else if (fav >= 5000) {
                    top5kCandidates.add(candidateId);
                } else if (fav >= 3000) {
                    top3kCandidates.add(candidateId);
                }
            } else {
                // 如果推荐图片信息中没有找到，说明可能是起始图片，需要通过API获取
                try {
                    int fav = PixivRecHelper.getBookmarkCountFromApi(candidateId);
                    if (fav >= 10000) {
                        top1wCandidates.add(candidateId);
                    } else if (fav >= 5000) {
                        top5kCandidates.add(candidateId);
                    } else if (fav >= 3000) {
                        top3kCandidates.add(candidateId);
                    }
                } catch (Exception e) {
                    System.out.println("【调试】获取候选图片 " + candidateId + " 收藏数失败: " + e.getMessage());
                }
            }
        }

        System.out.println("【调试】1w+候选: " + top1wCandidates.size() + " 张");
        System.out.println("【调试】5k~1w候选: " + top5kCandidates.size() + " 张");
        System.out.println("【调试】3k~5k候选: " + top3kCandidates.size() + " 张");

        // 重复选择起始图片
        int selectionAttempts = 0;
        while(selected.size() < PixivCrawlerConfig.START_IMAGES_PER_ROUND && selectionAttempts < 10){ // 添加最大尝试次数防止无限循环
            selectionAttempts++;
            String selectedPid = selectOneImageByProbability(
                    top1wCandidates, top5kCandidates, top3kCandidates, random);

            System.out.println("【调试】第" + selectionAttempts + "次选择尝试，结果: " + selectedPid);

            // 选到图片且未重复
            if(selectedPid != null && !selected.contains(selectedPid)){
                selected.add(selectedPid);
                System.out.println("【调试】成功选择图片: " + selectedPid);
            }
        }

        System.out.println("【调试】最终选择结果: " + selected);

        return selected;
    }

    // 依据概率选取一张图片
    private String selectOneImageByProbability(
            List<String> top1wCandidates,
            List<String> top5kCandidates,
            List<String> top3kCandidates,
            Random random) {

        double rand =random.nextDouble();

        if(rand < PixivCrawlerConfig.TOP1W_SELECTION_PROBABILITY && !top1wCandidates.isEmpty()){
            // 选择 1w+
            int index = random.nextInt(top1wCandidates.size());
            return top1wCandidates.get(index);
        } else if (rand >= PixivCrawlerConfig.TOP1W_SELECTION_PROBABILITY &&
                rand < PixivCrawlerConfig.TOP5K_SELECTION_PROBABILITY + PixivCrawlerConfig.TOP1W_SELECTION_PROBABILITY &&
                !top5kCandidates.isEmpty()) {
            // 选择 5k~1w
            int index = random.nextInt(top5kCandidates.size());
            return top5kCandidates.get(index);
        } else if (rand >= PixivCrawlerConfig.TOP1W_SELECTION_PROBABILITY + PixivCrawlerConfig.TOP5K_SELECTION_PROBABILITY &&
                !top3kCandidates.isEmpty()){
            // 选择 3k ~ 5k
            int index = random.nextInt(top3kCandidates.size());
            return top3kCandidates.get(index);
        }

        // 如果选取到的队列为空，则依次尝试从队列中选择
        if(!top1wCandidates.isEmpty()){
            int index = random.nextInt(top1wCandidates.size());
            return top1wCandidates.get(index);
        } else if (!top5kCandidates.isEmpty()) {
            int index = random.nextInt(top5kCandidates.size());
            return top5kCandidates.get(index);
        } else if (!top3kCandidates.isEmpty()) {
            int index = random.nextInt(top3kCandidates.size());
            return top3kCandidates.get(index);
        }

        // 所有队列为空
        return null;
    }
    
    /**
     * 添加下载路径到记录中
     * @param savePath 保存路径
     */
    private static void addDownloadPath(String savePath) {
        downloadPaths.add(savePath);
        System.out.println("【记录】添加下载路径: " + savePath);
    }
    
    /**
     * 根据收藏数标签获取相关推荐的下载路径
     * @param tag 收藏数标签（"1w+", "5k~1w", "3k~5k"）
     * @return 对应的下载路径
     */
    private String getRecommendationsSavePath(String tag) {
        // 获取当前日期作为文件夹名称
        String currentDate = DateUtils.getCurrentDate();
        
        String folderName;
        switch (tag) {
            case "1w+":
                folderName = PixivCrawlerConfig.TOP1W_FOLDER;
                break;
            case "5k~1w":
                folderName = PixivCrawlerConfig.TOP5K_FOLDER;
                break;
            case "3k~5k":
                folderName = PixivCrawlerConfig.TOP3K_FOLDER;
                break;
            default:
                folderName = "unknown";
                System.out.println("【警告】未知的收藏数标签: " + tag + "，使用默认文件夹");
        }
        
        // 构建路径：基础路径/日期/收藏数文件夹
        String savePath = PixivCrawlerConfig.RECOMMENDATIONS_BASE_PATH + "/" + currentDate + "/" + folderName;
        System.out.println("【相关推荐】" + tag + " 收藏数图片下载到: " + savePath);
        return savePath;
    }
    
    /**
     * 清理所有下载路径中的.part文件
     */
    public static void cleanupAllDownloadPaths() {
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
        cleanupRecommendationsPaths();
        
        System.out.println("【清理】所有下载路径清理完成");
    }
    
    /**
     * 清理相关推荐路径中的所有.part文件
     */
    private static void cleanupRecommendationsPaths() {
        try {
            String currentDate = DateUtils.getCurrentDate();
            String recommendationsBasePath = PixivCrawlerConfig.RECOMMENDATIONS_BASE_PATH;
            
            // 清理当前日期的相关推荐路径
            String[] folderNames = {
                PixivCrawlerConfig.TOP1W_FOLDER,
                PixivCrawlerConfig.TOP5K_FOLDER,
                PixivCrawlerConfig.TOP3K_FOLDER
            };
            
            for (String folderName : folderNames) {
                String fullPath = recommendationsBasePath + "/" + currentDate + "/" + folderName;
                try {
                    ImageDownloader.deleteFile("【清理相关推荐-" + folderName + "】", fullPath);
                    System.out.println("【清理】已清理相关推荐路径: " + fullPath);
                } catch (Exception e) {
                    System.out.println("【清理】清理相关推荐路径 " + fullPath + " 时出错: " + e.getMessage());
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
