import com.pixiv.crawler.config.PixivCrawlerConfig;
import com.pixiv.crawler.service.PixivCrawler;
import org.junit.jupiter.api.Test;

public class PixivRecommendTest {
    @Test
    public void RecommendTest(){
        PixivCrawler crawler = new PixivCrawler();

        try {
            System.out.println("【相关推荐】开始运行算法...");
            System.out.println("起始图片ID: " + PixivCrawlerConfig.START_PID);
            System.out.println("最大深度: " + PixivCrawlerConfig.MAX_DEPTH);
            System.out.println("每次获取图片数: " + PixivCrawlerConfig.START_IMAGES_PER_ROUND);

            crawler.downloadRecommendImages(PixivCrawlerConfig.START_PID,
                    PixivCrawlerConfig.MAX_DEPTH,
                    PixivCrawlerConfig.START_IMAGES_PER_ROUND);

            System.out.println("【相关推荐】算法执行完成");
        } catch (Exception e) {
            System.out.println("【相关推荐】执行过程中出错：" + e.getMessage());
            e.printStackTrace();
        }
    }


}
