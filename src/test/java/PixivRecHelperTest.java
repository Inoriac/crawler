import com.pixiv.crawler.util.PixivRecHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
public class PixivRecHelperTest {
    @Test
    public void testExtractRecommendIds(){
        String html = "<div class=\"gtm-illust-recommend-zone\">" +
                "<a data-gtm-recommend-illust-id=\"123456\"></a>" +
                "<a data-gtm-recommend-illust-id=\"654321\"></a>" +
                "<a data-gtm-recommend-illust-id=\"111222\"></a>" +
                "</div>";

        Document document = Jsoup.parse(html);
//        Set<String> ids = PixivRecHelper.extractRecommendIds(document, 8);

        assertEquals(3, ids.size());
        assertTrue(ids.contains("123456"));
        assertTrue(ids.contains("654321"));
        assertTrue(ids.contains("111222"));

    }
}
