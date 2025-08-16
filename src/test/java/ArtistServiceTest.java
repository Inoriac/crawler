import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.main.PixivCrawler;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.model.SavePath;
import com.pixiv.crawler.service.ArtistService;
import com.pixiv.crawler.service.Downloader;
import com.pixiv.crawler.util.ImageDownloader;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ArtistServiceTest {
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

        ArtistService artistService = new ArtistService();

        // TODO：有关该方法使用的设想，保存该画师图片的文件夹名应为画师名
        try {
            List<PixivImage> pixivImages = artistService.searchArtworksByArtistId(GlobalConfig.ARTIST_START_ID, GlobalConfig.ARTIST_MAX_IMAGE);
            String artistName = artistService.getArtistName(GlobalConfig.ARTIST_START_ID);

            String savePath = GlobalConfig.ARTIST_BASE_PATH + "/" + artistName;

            downloader.startDownload(pixivImages, "画师作品", savePath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
