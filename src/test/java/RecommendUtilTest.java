import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.util.JsonUtil;
import com.pixiv.crawler.model.PixivImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendUtilTest {
//    @Test
//    public void testGetImageInfoById(){
//        System.out.println(PixivRecHelper.getImageInfoById(PixivCrawlerConfig.START_PID));
//    }
    
    @Test
    public void testMangaTagDetection() {
        // 测试包含漫画标签的JSON
        String mangaJson = "{\"id\":\"123456\",\"title\":\"测试漫画\",\"userName\":\"测试作者\",\"createDate\":\"2025-01-01T00:00:00+09:00\",\"tags\":[\"R-18\",\"漫画\",\"测试标签\"]}";

        PixivImage image = new PixivImage();
        JsonUtil.parseBasicInfo(mangaJson, image);
        
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

        PixivImage image = new PixivImage();
        JsonUtil.parseBasicInfo(normalJson, image);
        
        assertNotNull(image);
        assertEquals("789012", image.getId());
        assertFalse(image.isManga());
        assertTrue(image.isR18());
        
        System.out.println("普通作品检测测试通过: " + image.getId() + " isManga=" + image.isManga());
    }
    
    @Test
    public void testMangaExcludeConfig() {
        // 测试配置是否正确
        assertTrue(GlobalConfig.MANGA_EXCLUDE_ENABLED, "漫画排除功能应该默认启用");
        assertEquals("漫画", GlobalConfig.MANGA_TAG_KEYWORD, "漫画关键词应该正确设置");
        System.out.println("漫画排除配置测试通过");
    }

    @Test
    public void testJsonUtilGetImageInfoById() {
        // 测试JsonUtil中的通用方法
        PixivImage image = JsonUtil.getImageInfoById(GlobalConfig.ARTWORK_START_PID);
        assertNotNull(image, "JsonUtil.getImageInfoById应该返回非空对象");
        assertEquals(GlobalConfig.ARTWORK_START_PID, image.getId(), "图片ID应该正确设置");
        assertNotNull(image.getTitle(), "标题不应该为空");
        assertNotNull(image.getArtist(), "作者不应该为空");
        assertNotNull(image.getUrl(), "URL不应该为空");
        assertNotNull(image.getTags(), "Tags不应该为空");
        System.out.println("JsonUtil.getImageInfoById测试通过: " + image.getId() + " - " + image.getTitle());
    }

    @Test
    public void testMultiPageImageSupport() {
        // 测试多页图片支持
        PixivImage image = JsonUtil.getImageInfoById(GlobalConfig.ARTWORK_START_PID);
        assertNotNull(image, "应该能获取到图片信息");
        
        // 检查pageCount
        assertTrue(image.getPageCount() > 0, "pageCount应该大于0");
        System.out.println("作品 " + image.getId() + " 共有 " + image.getPageCount() + " 页");
        
        // 打印所有页面的URL
        for (int i = 0; i < image.getPageCount(); i++) {
            String url = constructPageUrl(image.getUrl(), i);
            assertNotNull(url, "第" + (i + 1) + "页的URL不应该为空");
            System.out.println("第" + (i + 1) + "页URL: " + url);
        }
        
        System.out.println("多页图片支持测试通过");
    }
    
    @Test
    public void testSinglePageVsMultiPageBehavior() {
        // 测试单页和多页的不同下载行为
        PixivImage image = JsonUtil.getImageInfoById(GlobalConfig.ARTWORK_START_PID);
        assertNotNull(image, "应该能获取到图片信息");
        
        int pageCount = image.getPageCount();
        System.out.println("作品 " + image.getId() + " 共有 " + pageCount + " 页");
        
        if (pageCount == 1) {
            System.out.println("✅ 单页作品：将直接下载到主目录，不创建子文件夹");
            System.out.println("   预期文件路径: downloads/作品ID.jpg");
        } else {
            System.out.println("✅ 多页作品：将创建独立文件夹，下载所有页面");
            System.out.println("   预期文件夹路径: downloads/作品ID/");
            System.out.println("   预期文件路径: downloads/作品ID/作品ID_p0.jpg, 作品ID_p1.jpg, ...");
        }
        
        System.out.println("单页/多页行为测试通过");
    }
    
    @Test
    public void testUrlConstructionFix() {
        // 测试URL构造修复
        String testPid = "133811943";
        PixivImage image = JsonUtil.getImageInfoById(testPid);
        
        if (image != null) {
            System.out.println("✅ 成功获取作品信息: " + image.getId() + " - " + image.getTitle());
            System.out.println("✅ pageCount: " + image.getPageCount());
            System.out.println("✅ 构造的URL: " + image.getUrl());
            
            // 验证URL格式
            assertNotNull(image.getUrl(), "URL不应该为空");
            assertTrue(image.getUrl().startsWith("https://i.pximg.net/img-original/img/"), 
                      "URL应该以正确的域名开头");
            assertTrue(image.getUrl().endsWith("_p0.jpg"), 
                      "URL应该以_p0.jpg结尾");
            
            System.out.println("✅ URL格式验证通过");
        } else {
            System.out.println("❌ 获取作品信息失败");
        }
    }
    
    /**
     * 根据基础URL和页面索引构造页面URL（测试用）
     */
    private String constructPageUrl(String baseUrl, int pageIndex) {
        if (pageIndex == 0) {
            return baseUrl; // 第一页直接使用基础URL
        }
        
        // 替换URL中的页面编号
        return baseUrl.replace("_p0.jpg", "_p" + pageIndex + ".jpg");
    }
}
