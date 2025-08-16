import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.service.PixivCrawler;
import org.junit.jupiter.api.Test;

public class PixivRecommendTest {
    @Test
    public void RecommendTest(){
        PixivCrawler crawler = new PixivCrawler();

        try {
            System.out.println("【相关推荐】开始运行算法...");
            System.out.println("起始图片ID: " + GlobalConfig.START_PID);
            System.out.println("最大深度: " + GlobalConfig.MAX_DEPTH);
            System.out.println("每次获取图片数: " + GlobalConfig.START_IMAGES_PER_ROUND);

            crawler.downloadRecommendImages(GlobalConfig.START_PID,
                    GlobalConfig.MAX_DEPTH,
                    GlobalConfig.START_IMAGES_PER_ROUND);

            System.out.println("【相关推荐】算法执行完成");
        } catch (Exception e) {
            System.out.println("【相关推荐】执行过程中出错：" + e.getMessage());
            e.printStackTrace();
        }
    }


}
