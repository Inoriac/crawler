import com.pixiv.crawler.service.TagService;
import com.pixiv.crawler.service.impl.TagServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class TagServiceTest {
    @Test
    public void testResponse() throws IOException {
        TagService tagService = new TagServiceImpl();

        String tag = "skirt";
        List<String> tags = tagService.getSimilarTagByApi(tag);
        System.out.println(tags);
    }
}
