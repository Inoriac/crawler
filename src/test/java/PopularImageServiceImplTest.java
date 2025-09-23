import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.main.PixivCrawler;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.model.SavePath;
import com.pixiv.crawler.service.impl.Downloader;
import com.pixiv.crawler.service.impl.PopularImageServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;


public class PopularImageServiceImplTest {
    private static volatile boolean stopFlag = false;
    private static Downloader downloader = new Downloader();
    @Test
    public void test(){
        // 注册关闭钩子函数，清理画师作品的.part文件
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("【crawler】 正在优雅地关闭程序...");
            stopFlag = true;

            if (downloader != null) {
                downloader.stopDownload();
            }

            // 清理所有下载路径中的.part文件
            SavePath.cleanDownloadPaths();
        }));

        PopularImageServiceImpl popularImageServiceImpl = new PopularImageServiceImpl();
        PixivCrawler pixivCrawler = new PixivCrawler();

        try{
            String tag = "スズラン(アークナイツ)";
//            String tag = "黒スト";
            List<PixivImage> popularImages = popularImageServiceImpl.getPopularImagesByTag(tag);

            String savePath = GlobalConfig.POPULAR_BASE_PATH + "/" + tag;
            downloader.startDownload(popularImages, "热门作品" , savePath);

            // 根据获取到的热门图片，依次获取下面的推荐图片
            for (int i = 0; i < popularImages.size(); i++) {
                pixivCrawler.downloadRecommendImages(popularImages.get(i), savePath + "/recommend", tag);
            }

        }catch (Exception e){
            throw new RuntimeException(e);
        }


    }
}
