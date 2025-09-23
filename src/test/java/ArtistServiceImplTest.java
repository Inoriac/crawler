import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.model.SavePath;
import com.pixiv.crawler.service.impl.ArtistServiceImpl;
import com.pixiv.crawler.service.impl.Downloader;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ArtistServiceImplTest {
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

        ArtistServiceImpl artistServiceImpl = new ArtistServiceImpl();

        try {
            List<PixivImage> pixivImages = artistServiceImpl.searchArtworksByArtistId(GlobalConfig.ARTIST_START_ID, GlobalConfig.ARTIST_MAX_IMAGE);
            String artistName = artistServiceImpl.getArtistName(GlobalConfig.ARTIST_START_ID);

            String savePath = GlobalConfig.ARTIST_BASE_PATH + "/" + artistName;

            downloader.startDownload(pixivImages, "画师作品", savePath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
