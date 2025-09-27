import com.pixiv.crawler.service.TagService;
import com.pixiv.crawler.service.impl.TagServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TagServiceTest {
    @Test
    public void testResponse() throws IOException {
        TagService tagService = new TagServiceImpl();
        String tag = "skirt";

        // 测试远程api 已通过
//        List<String> tags = tagService.getSimilarTagByApi(tag);
//        System.out.println(tags);

        // 测试映射 已通过
//        List<String> fromLocalMapping = tagService.findFromLocalMapping(tag);
//        System.out.println(fromLocalMapping);

        // 测试getTags方法
        // TODO：目前存在的问题是，服务端给出的tag的概率值都比较高，大多数都在0.8以上，这会导致我简单的加权平均求概率没有实际作用，需要重新设计算法
        File file = new File("E:/Desktop/杂/1/__otonose_kanade_hololive_and_1_more_drawn_by_dotori_seulseul__20e0eaded4c1fc4032a3e8f5684cbea6.png");
        if (!file.exists()) {
            System.err.println("测试图片不存在，请检查路径！");
            return;
        }

        Map<String, Double> tags = tagService.getTags(file);

        // 打印结果，观察是否返回了映射标签
        tags.forEach((k, v) -> System.out.println(k + " -> " + v));

        // TODO：Json服务尚待测试
    }
}
