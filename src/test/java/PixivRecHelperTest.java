import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.util.PixivRecHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class PixivRecHelperTest {
    @Test
    public void testGetImageInfoById(){
        System.out.println(PixivRecHelper.getImageInfoById(PixivCrawlerConfig.START_PID));
    }
}
