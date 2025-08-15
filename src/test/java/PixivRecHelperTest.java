import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.util.PixivRecHelper;
import com.pixiv.crawler.util.JsonUtil;
import com.pixiv.crawler.model.PixivImage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PixivRecHelperTest {
//    @Test
//    public void testGetImageInfoById(){
//        System.out.println(PixivRecHelper.getImageInfoById(PixivCrawlerConfig.START_PID));
//    }
    
    @Test
    public void testMangaTagDetection() {
        // 测试包含漫画标签的JSON
        String mangaJson = "{\"id\":\"123456\",\"title\":\"测试漫画\",\"userName\":\"测试作者\",\"createDate\":\"2025-01-01T00:00:00+09:00\",\"tags\":[\"R-18\",\"漫画\",\"测试标签\"]}";
        
        PixivImage image = JsonUtil.parseIllustObject(mangaJson);
        
        assertNotNull(image);
        assertEquals("123456", image.getId());
        assertTrue(image.isManga());
        assertTrue(image.isR18());
        
        System.out.println("漫画检测测试通过: " + image.getId() + " isManga=" + image.isManga());
    }
    
    @Test
    public void testNormalTagDetection() {
        // 测试不包含漫画标签的JSON
        String normalJson = "{\"id\":\"789012\",\"title\":\"测试插画\",\"userName\":\"测试作者\",\"createDate\":\"2025-01-01T00:00:00+09:00\",\"tags\":[\"R-18\",\"插画\",\"测试标签\"]}";
        
        PixivImage image = JsonUtil.parseIllustObject(normalJson);
        
        assertNotNull(image);
        assertEquals("789012", image.getId());
        assertFalse(image.isManga());
        assertTrue(image.isR18());
        
        System.out.println("普通作品检测测试通过: " + image.getId() + " isManga=" + image.isManga());
    }
    
    @Test
    public void testMangaExcludeConfig() {
        // 测试配置是否正确
        assertTrue(PixivCrawlerConfig.MANGA_EXCLUDE_ENABLED, "漫画排除功能应该默认启用");
        assertEquals("漫画", PixivCrawlerConfig.MANGA_TAG_KEYWORD, "漫画关键词应该正确设置");
        System.out.println("漫画排除配置测试通过");
    }

    @Test
    public void testJsonUtilGetImageInfoById() {
        // 测试JsonUtil中的通用方法
        PixivImage image = JsonUtil.getImageInfoById(PixivCrawlerConfig.START_PID);
        assertNotNull(image, "JsonUtil.getImageInfoById应该返回非空对象");
        assertEquals(PixivCrawlerConfig.START_PID, image.getId(), "图片ID应该正确设置");
        assertNotNull(image.getTitle(), "标题不应该为空");
        assertNotNull(image.getArtist(), "作者不应该为空");
        assertNotNull(image.getUrl(), "URL不应该为空");
        assertNotNull(image.getTags(), "Tags不应该为空");
        System.out.println("JsonUtil.getImageInfoById测试通过: " + image.getId() + " - " + image.getTitle());
    }
}
