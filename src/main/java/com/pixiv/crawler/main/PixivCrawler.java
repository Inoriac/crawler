package com.pixiv.crawler.main;

import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.service.impl.Downloader;
import com.pixiv.crawler.util.DateUtils;
import com.pixiv.crawler.util.JsonUtil;
import com.pixiv.crawler.util.RecommendUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.pixiv.crawler.model.PixivImage;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pixiv 爬虫
 */
public class PixivCrawler {
    private Downloader downloader;

    public PixivCrawler() {
        this.downloader = new Downloader();
    }

    // 获取并下载 Pixiv 日榜图片
    public void fetchRankingImages() throws Exception {
        List<PixivImage> images = new ArrayList<>();
        List<PixivImage> mangaImages = new ArrayList<>();
        // 创建代理对象
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", GlobalConfig.PORT));

        System.out.println("尝试访问排行榜页面...");

        // 访问 Pixiv 排行榜页面
        Document document = Jsoup.connect(GlobalConfig.PIXIV_RANKING_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .header("Cookie", GlobalConfig.COOKIE)
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

            // 通过API获取作品的完整信息（包括tags、pageCount等）
            try {
                PixivImage detailedImage = JsonUtil.getImageInfoById(id);
                if (detailedImage != null) {
                    // 检查是否为漫画作品
                    if (GlobalConfig.MANGA_EXCLUDE_ENABLED && detailedImage.isManga()) {
                        mangaImages.add(detailedImage);
                        System.out.println("【日榜】作品 " + id + " 为漫画作品，已排除");
                        continue;
                    }
                    
                    // 使用API获取的完整信息（包含pageCount）
                    images.add(detailedImage);
                    System.out.println("【日榜】使用API信息：作品 " + id + " 共 " + detailedImage.getPageCount() + " 页");
                } else {
                    // 如果API获取失败，使用HTML解析的基础信息
                    System.out.println("【日榜】API获取失败，使用HTML解析信息：作品 " + id);
                    images.add(image);
                }
            } catch (Exception e) {
                System.out.println("【日榜】获取作品 " + id + " 详细信息失败: " + e.getMessage());
                // 如果获取详细信息失败，使用HTML解析的基础信息
                images.add(image);
            }
        }

        // 输出统计信息
        if (GlobalConfig.MANGA_EXCLUDE_ENABLED) {
            System.out.println("【日榜】总共解析到 " + (images.size() + mangaImages.size()) + " 个作品");
            System.out.println("【日榜】排除 " + mangaImages.size() + " 个漫画作品（漫画排除已启用）");
            System.out.println("【日榜】实际下载 " + images.size() + " 个作品");
        } else {
            System.out.println("【日榜】总共解析到 " + images.size() + " 个作品（漫画排除已禁用）");
        }

        // 获取当前周的文件夹名称
        String weekFolderName = DateUtils.getCurrentWeekFolderName();
        String rankingSavePath = GlobalConfig.RANKING_BASE_PATH + "/" + weekFolderName;
        
        System.out.println("【日榜下载】当前周文件夹: " + weekFolderName);
        System.out.println("【日榜下载】完整下载路径: " + rankingSavePath);
        
        // 记录下载路径
        downloader.startDownload(images, "日榜爬取", rankingSavePath);
    }

    // 提供重载，兼容现有方法
    public void downloadRecommendImages(PixivImage startImage, String baseUrl) throws Exception {
        downloadRecommendImages(startImage, baseUrl, null);
    }

    /**
     * 从指定图片ID出发，爬取相关图片，按收藏数分档，并提交下载任务
     * @param startImage  起始图片 pid
     */
    public void downloadRecommendImages(PixivImage startImage, String baseUrl, String tag) throws Exception {
        // TODO： 此处需要增加统计 各个类别下载多少张，一共下载多少张
        Set<String> visited = new HashSet<>();  // 防止选取同样的图片作为起始

        // 当前层的待选起始图片
        List<PixivImage> currentStartImages = new ArrayList<>();
        currentStartImages.add(startImage);

        // 优先队列，按照收藏数降序
        PriorityQueue<PixivImage> top1w = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());
        PriorityQueue<PixivImage> top5k = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());
        PriorityQueue<PixivImage> top3k = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());
        PriorityQueue<PixivImage> top1k = new PriorityQueue<>(Comparator.comparingInt(PixivImage::getBookmarkCount).reversed());

        // 存储所有推荐图片的完整信息，用于后续选择
        Map<String, PixivImage> allRecommendImages = new HashMap<>();

        int depth = 0;
        while (!currentStartImages.isEmpty() && depth < GlobalConfig.MAX_DEPTH) {
            System.out.println("【第" + (depth + 1) + "层】开始处理，起始图片数量: " + currentStartImages.size());

            // 下一层的起始图片候选
            List<String> nextStartCandidates = new ArrayList<>();

            for(PixivImage image : currentStartImages){
                // 跳过null对象
                if (image == null) {
                    System.out.println("【警告】发现null的PixivImage对象，跳过处理");
                    continue;
                }
                
                String pid = image.getId();
                if (pid == null || pid.isEmpty()) {
                    System.out.println("【警告】发现ID为空的PixivImage对象，跳过处理");
                    continue;
                }

                // 跳过已爬取
                if (visited.contains(pid)) continue;
                visited.add(pid);

                // 跳过漫画作品
                if(image.isManga() && GlobalConfig.MANGA_EXCLUDE_ENABLED) continue;

                // 跳过 tag 不符合
                if(tag != null && !(image.getTags().contains(tag))) continue;

                // 仅 depth=0 时需要进行一次分类
                if(depth == 0){
                    // 排除漫画作品
                    if (image.isManga()) {
                        System.out.println("【相关推荐】起始图片 " + image.getId() + " 为漫画作品，已排除");
                        continue;
                    }
                    
                    int fav = image.getBookmarkCount();
                    if (fav >= 10000) {
                        top1w.add(image);
                    } else if (fav >= 5000) {
                        top5k.add(image);
                    } else if (fav >= 3000) {
                        top3k.add(image);
                    } else if (fav >= 1000){
                        top1k.add(image);
                    } else {
                        System.out.println("【相关推荐】 收藏数低于1k，已弃用(" + image.getId() + ")");
                    }
                }

                // 获取相关推荐图片完整信息
                List<PixivImage> recImages = RecommendUtil.getRecommendImagesByPid(pid, GlobalConfig.PER_RECOMMEND_MAX_IMAGE); // 获取20张推荐图片
                System.out.println("【相关推荐】获取到 " + recImages.size() + " 张推荐图片");
                
                // 将推荐图片直接添加到对应的队列中，并收集ID用于下一轮
                for (PixivImage recImage : recImages) {
                    // 跳过 tag 不符合的图片
                    if(tag != null && !(recImage.getTags().contains(tag))) {
                        System.out.println("【tag】" + recImage.getId() + "因tag不符合被排除");
                        continue;
                    }

                    nextStartCandidates.add(recImage.getId());
                    allRecommendImages.put(recImage.getId(), recImage); // 保存完整信息
                    
                    // 通过API获取收藏数
                    try {
                        int fav = RecommendUtil.getBookmarkCountFromApi(recImage.getId());
                        recImage.setBookmarkCount(fav);
                        System.out.println("【收藏数】" + recImage.getId() + " -> " + fav);
                    } catch (Exception e) {
                        System.out.println("【收藏数】获取失败: " + recImage.getId() + " - " + e.getMessage());
                        recImage.setBookmarkCount(0);
                    }
                    
                    // 直接分类推荐图片到对应队列（排除漫画作品）
                    if (recImage.isManga() && GlobalConfig.MANGA_EXCLUDE_ENABLED) {
                        System.out.println("【分类】" + recImage.getId() + " -> 漫画作品，已排除 (收藏数: " + recImage.getBookmarkCount() + ")");
                        continue;
                    }
                    
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
                    } else if (fav >= 1000) {
                        top1k.add(recImage);
                        System.out.println("【分类】" + recImage.getId() + " -> 1k~3k队列 (收藏数: " + fav + ")");
                    }else {
                        System.out.println("【分类】" + recImage.getId() + " -> 收藏数低于1k，舍弃 (收藏数: " + fav + ")");
                    }
                }
            }
            // 检查并处理每个队列
            processQueue(top1w, "1w+", baseUrl);
            processQueue(top5k, "5k~1w", baseUrl);
            processQueue(top3k, "3k~5k", baseUrl);
            processQueue(top1k, "1k~3k", baseUrl);

            //为下一层选择三张起始图片
            currentStartImages = selectNextStartImages(nextStartCandidates, visited, allRecommendImages);

            depth++;
        }

        System.out.println("【算法结束】提交剩余队列的下载任务...");
        submitQueueDownload(top1w,  "1w+", baseUrl);
        submitQueueDownload(top5k,  "5k~1w", baseUrl);
        submitQueueDownload(top3k,  "3k~5k", baseUrl);
        submitQueueDownload(top1k,  "1k~3k", baseUrl);

    }

    // 队列满时提交下载任务
    private void processQueue(PriorityQueue<PixivImage> queue, String tag, String baseUrl) throws Exception {
        if(queue.size() >= GlobalConfig.QUEUE_PROCESS_THRESHOLD){
            System.out.println("【" + tag + "】" + "队列满" + GlobalConfig.QUEUE_PROCESS_THRESHOLD + "，创建下载任务...");
            
            // 分离R-18和非R-18作品，同时排除漫画作品
            List<PixivImage> r18Images = new ArrayList<>();
            List<PixivImage> normalImages = new ArrayList<>();
            List<PixivImage> mangaImages = new ArrayList<>();
            
            for (PixivImage image : queue) {
                if (image.isManga()) {
                    mangaImages.add(image);
                } else if (image.isR18()) {
                    r18Images.add(image);
                } else {
                    normalImages.add(image);
                }
            }
            
            // 记录漫画作品排除情况
            if (!mangaImages.isEmpty()) {
                System.out.println("【" + tag + "】排除" + mangaImages.size() + "个漫画作品的下载（漫画排除已启用）");
            }
            
            // 下载非R-18作品到normal文件夹
            if (!normalImages.isEmpty()) {
                String normalSavePath = getRecommendationsSavePath(tag, false, baseUrl);
                downloader.startDownload(normalImages,  "相关推荐-" + tag + "-普通", normalSavePath);
            }
            
            // 下载R-18作品到r-18文件夹（如果开启R-18下载）
            if (!r18Images.isEmpty() && GlobalConfig.R18_DOWNLOAD_ENABLED) {
                String r18SavePath = getRecommendationsSavePath(tag, true, baseUrl);
                downloader.startDownload(r18Images, "相关推荐-" + tag + "-R18", r18SavePath);
            } else if (!r18Images.isEmpty() && !GlobalConfig.R18_DOWNLOAD_ENABLED) {
                System.out.println("【" + tag + "】跳过" + r18Images.size() + "个R-18作品的下载（R-18下载已禁用）");
            }
            
            // 清空队列
            queue.clear();
        }
    }

    // 提交队列下载任务(结束时调用)
    private void submitQueueDownload(PriorityQueue<PixivImage> queue, String tag, String baseUrl) throws Exception {
        if(!queue.isEmpty()){
            System.out.println("【" + tag + "】提交剩余" + queue.size() + "张图片的下载任务...");
            
            // 分离R-18和非R-18作品，同时排除漫画作品
            List<PixivImage> r18Images = new ArrayList<>();
            List<PixivImage> normalImages = new ArrayList<>();
            List<PixivImage> mangaImages = new ArrayList<>();
            
            for (PixivImage image : queue) {
                if (image.isManga()) {
                    mangaImages.add(image);
                } else if (image.isR18()) {
                    r18Images.add(image);
                } else {
                    normalImages.add(image);
                }
            }
            
            // 记录漫画作品排除情况
            if (!mangaImages.isEmpty()) {
                System.out.println("【" + tag + "】排除" + mangaImages.size() + "个漫画作品的下载（漫画排除已启用）");
            }
            
            // 下载非R-18作品到normal文件夹
            if (!normalImages.isEmpty()) {
                String normalSavePath = getRecommendationsSavePath(tag, false, baseUrl);
                downloader.startDownload(normalImages, "下载-" + tag + "-普通", normalSavePath);
            }
            
            // 下载R-18作品到r-18文件夹（如果开启R-18下载）
            if (!r18Images.isEmpty() && GlobalConfig.R18_DOWNLOAD_ENABLED) {
                String r18SavePath = getRecommendationsSavePath(tag, true, baseUrl);
                downloader.startDownload(r18Images, "下载-" + tag + "-R18", r18SavePath);
            } else if (!r18Images.isEmpty() && !GlobalConfig.R18_DOWNLOAD_ENABLED) {
                System.out.println("【" + tag + "】跳过" + r18Images.size() + "个R-18作品的下载（R-18下载已禁用）");
            }
        }
    }

    // 从候选图片中选择下一轮的起始图片
    private List<PixivImage> selectNextStartImages(List<String> candidates, Set<String> visited,
                                             Map<String, PixivImage> allRecommendImages) throws Exception {
        List<PixivImage> selected = new ArrayList<>();
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
        List<String> top1kCandidates = new ArrayList<>();

        // 从已保存的推荐图片信息中分类（排除漫画作品）
        for (String candidateId : availableCandidates) {
            PixivImage image = allRecommendImages.get(candidateId);
            if (image != null) {
                // 排除漫画作品
                if (image.isManga()) {
                    System.out.println("【调试】候选图片 " + candidateId + " 为漫画作品，已排除");
                    continue;
                }
                
                int fav = image.getBookmarkCount();
                if (fav >= 10000) {
                    top1wCandidates.add(candidateId);
                } else if (fav >= 5000) {
                    top5kCandidates.add(candidateId);
                } else if (fav >= 3000) {
                    top3kCandidates.add(candidateId);
                } else if (fav >= 1000) {
                    top1kCandidates.add(candidateId);
                }
            } else {
                // 如果推荐图片信息中没有找到，说明可能是起始图片，需要通过API获取
                try {
                    int fav = RecommendUtil.getBookmarkCountFromApi(candidateId);
                    if (fav >= 10000) {
                        top1wCandidates.add(candidateId);
                    } else if (fav >= 5000) {
                        top5kCandidates.add(candidateId);
                    } else if (fav >= 3000) {
                        top3kCandidates.add(candidateId);
                    } else if (fav >= 1000) {
                        top1kCandidates.add(candidateId);
                    }
                } catch (Exception e) {
                    System.out.println("【调试】获取候选图片 " + candidateId + " 收藏数失败: " + e.getMessage());
                }
            }
        }

        System.out.println("【调试】1w+候选: " + top1wCandidates.size() + " 张");
        System.out.println("【调试】5k~1w候选: " + top5kCandidates.size() + " 张");
        System.out.println("【调试】3k~5k候选: " + top3kCandidates.size() + " 张");
        System.out.println("【调试】1k~3k候选: " + top1kCandidates.size() + " 张");

        // 重复选择起始图片
        int selectionAttempts = 0;
        while(selected.size() < GlobalConfig.RECOMMEND_START_IMAGES_PER_ROUND && selectionAttempts < 10){ // 添加最大尝试次数防止无限循环
            selectionAttempts++;
            String selectedPid = selectOneImageByProbability(
                    top1wCandidates, top5kCandidates, top3kCandidates, top1kCandidates, random);

            System.out.println("【调试】第" + selectionAttempts + "次选择尝试，结果: " + selectedPid);

            // 选到图片且未重复
            if(selectedPid != null){
                // 检查是否已经选择了这个ID的图片
                boolean alreadySelected = selected.stream()
                    .anyMatch(img -> img != null && selectedPid.equals(img.getId()));
                
                if (!alreadySelected) {
                    PixivImage image = JsonUtil.getImageInfoById(selectedPid);
                    if (image != null) {
                        selected.add(image);
                        System.out.println("【调试】成功选择图片: " + selectedPid);
                    }
//                    else {
//                        System.out.println("【警告】获取图片信息失败，跳过: " + selectedPid);
//                    }
                }
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
            List<String> top1kCandidates,
            Random random) {

        double rand =random.nextDouble();

        if(rand < GlobalConfig.TOP1W_SELECTION_PROBABILITY && !top1wCandidates.isEmpty()){
            // 选择 1w+
            int index = random.nextInt(top1wCandidates.size());
            return top1wCandidates.get(index);
        } else if (rand >= GlobalConfig.TOP1W_SELECTION_PROBABILITY &&
                rand < (GlobalConfig.TOP5K_SELECTION_PROBABILITY + GlobalConfig.TOP1W_SELECTION_PROBABILITY) &&
                !top5kCandidates.isEmpty()) {
            // 选择 5k~1w
            int index = random.nextInt(top5kCandidates.size());
            return top5kCandidates.get(index);
        } else if (rand >= (GlobalConfig.TOP5K_SELECTION_PROBABILITY + GlobalConfig.TOP1W_SELECTION_PROBABILITY) &&
                rand < (1 - GlobalConfig.TOP1K_SELECTION_PROBABILITY) &&
                !top3kCandidates.isEmpty()){
            // 选择 3k ~ 5k
            int index = random.nextInt(top3kCandidates.size());
            return top3kCandidates.get(index);
        } else if (rand >= (1 - GlobalConfig.TOP1K_SELECTION_PROBABILITY) &&
                !top1kCandidates.isEmpty()){
            // 选择 1k ~ 3k
            int index = random.nextInt(top1kCandidates.size());
            return top1kCandidates.get(index);
        }

        // 如果选取到的队列为空，则依次尝试从队列中选择
        if (!top5kCandidates.isEmpty()) {
            int index = random.nextInt(top5kCandidates.size());
            return top5kCandidates.get(index);
        } else if (!top3kCandidates.isEmpty()) {
            int index = random.nextInt(top3kCandidates.size());
            return top3kCandidates.get(index);
        } else if (!top1kCandidates.isEmpty()) {
            int index = random.nextInt(top1kCandidates.size());
            return top1kCandidates.get(index);
        } else if(!top1wCandidates.isEmpty()){
            int index = random.nextInt(top1wCandidates.size());
            return top1wCandidates.get(index);
        }

        // 所有队列为空
        return null;
    }

    /**
     * 根据收藏数标签和R-18状态获取相关推荐图片的保存路径
     */
    private String getRecommendationsSavePath(String tag, boolean isR18, String baseUrl) {
        // 获取当前日期作为文件夹名称
        String currentDate = DateUtils.getCurrentDate();
        
        String folderName;
        switch (tag) {
            case "1w+":
                folderName = GlobalConfig.TOP1W_FOLDER;
                break;
            case "5k~1w":
                folderName = GlobalConfig.TOP5K_FOLDER;
                break;
            case "3k~5k":
                folderName = GlobalConfig.TOP3K_FOLDER;
                break;
            case "1k~3k":
                folderName = GlobalConfig.TOP1K_FOLDER;
                break;
            default:
                folderName = "unknown";
                System.out.println("【警告】未知的收藏数标签: " + tag + "，使用默认文件夹");
        }
        
        // 根据R-18状态添加对应的子文件夹
        if (isR18) {
            folderName = folderName + "/" + GlobalConfig.R18_FOLDER;
        } else {
            folderName = folderName + "/" + GlobalConfig.NORMAL_FOLDER;
        }
        
        // 构建路径：基础路径/日期/收藏数文件夹/normal或r-18
        String savePath = baseUrl + "/" + currentDate + "/" + folderName;

        System.out.println("【相关推荐】" + tag + (isR18 ? " R-18" : " 普通") + " 图片下载到: " + savePath);
        return savePath;
    }
}
