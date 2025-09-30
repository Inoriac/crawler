import com.pixiv.crawler.config.GlobalConfig;
import com.pixiv.crawler.model.CharacterTagHolder;
import com.pixiv.crawler.model.PixivImage;
import com.pixiv.crawler.model.TagInfo;
import com.pixiv.crawler.model.TagMapHolder;
import com.pixiv.crawler.service.TagService;
import com.pixiv.crawler.service.impl.TagServiceImpl;
import com.pixiv.crawler.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagServiceTest {
    
    private TagService tagService = new TagServiceImpl();
    
    @BeforeEach
    public void setUp() {
        tagService = new TagServiceImpl();
    }

    @AfterEach
    public void tearDown() {
        // 清理测试产生的文件
        File testFile = new File(GlobalConfig.TAG_SERVICE_JSON_NAME);
        if (testFile.exists()) {
            testFile.delete();
        }
    }
    
    @Test
    public void testResponse() throws IOException {

        String tag = "skirt";
        Map<String, TagInfo> tagMap = TagMapHolder.getInstance().getTagMap();

        // 测试远程api 已通过
//        List<String> tags = tagService.getSimilarTagByApi(tag);
//        System.out.println(tags);

        // 测试映射 已通过
//        List<String> fromLocalMapping = tagService.findFromLocalMapping(tag);
//        System.out.println(fromLocalMapping);

        // 测试getTags方法
        File file = new File("E:/Desktop/杂/1/__otonose_kanade_hololive_and_1_more_drawn_by_dotori_seulseul__20e0eaded4c1fc4032a3e8f5684cbea6.png");
        if (!file.exists()) {
            System.err.println("测试图片不存在，请检查路径！");
            return;
        }

//        Map<String, Double> tags = tagService.getTags(file);
//
//        // 打印结果，观察是否返回了映射标签
//        tags.forEach((k, v) -> System.out.println(k + " -> " + v));

        // 测试图片处理方法
        tagService.processImage(file);
        tagMap.forEach((k, v) -> System.out.println(k + " -> ( " + v.getCount() + ", " + v.getAvgProbability() + " )"));

        PixivImage image = JsonUtil.getImageInfoById("135611059");

        double v = tagService.calculateTagSimilarity(image.getTags());

        System.out.println("该图片匹配度为：" + v);
    }
    @Test
    public void testSaveToJson() throws IOException {
        // 创建测试数据
        Map<String, TagInfo> tagMap = TagMapHolder.getInstance().getTagMap();
        tagMap.put("1girl", new TagInfo(0.8, 5));
        tagMap.put("anime", new TagInfo(0.7, 3));
        tagMap.put("cute", new TagInfo(0.9, 8));

        // 保存到JSON
        tagService.saveToJson();

        // 验证文件是否创建
        File jsonFile = new File(GlobalConfig.TAG_SERVICE_JSON_NAME);
        assert jsonFile.exists() : "JSON文件应该被创建";

        System.out.println("JSON保存测试通过");
    }

    @Test
    public void testLoadFromJson() throws IOException {
        // 先创建测试数据并保存
        Map<String, TagInfo> tagMap = TagMapHolder.getInstance().getTagMap();
        tagMap.put("1girl", new TagInfo(0.8, 5));
        tagMap.put("anime", new TagInfo(0.7, 3));
        tagService.saveToJson();

        // 从JSON加载
        Map<String, TagInfo> loadedTagMap = tagService.loadFromJson();

        // 验证数据
        assert loadedTagMap.size() == 2 : "加载的标签数量应该为2";
        assert loadedTagMap.containsKey("1girl") : "应该包含1girl标签";
        assert loadedTagMap.get("1girl").getAvgProbability() == 0.8 : "1girl的概率应该为0.8";
        assert loadedTagMap.get("1girl").getCount() == 5 : "1girl的计数应该为5";

        System.out.println("JSON加载测试通过");
    }
    @Test
    public void testJsonRoundTrip() throws IOException {
        // 测试完整的保存-加载循环
        Map<String, TagInfo> tagMap = TagMapHolder.getInstance().getTagMap();
        tagMap.put("test_tag", new TagInfo(0.95, 10));
        tagMap.put("another_tag", new TagInfo(0.6, 2));

        // 保存
        tagService.saveToJson();

        // 加载
        Map<String, TagInfo> loadedTagMap = tagService.loadFromJson();

        // 验证数据完整性
        assert loadedTagMap.size() == tagMap.size() : "标签数量应该一致";
        for (String key : tagMap.keySet()) {
            assert loadedTagMap.containsKey(key) : "应该包含标签: " + key;
            TagInfo original = tagMap.get(key);
            TagInfo loaded = loadedTagMap.get(key);
            assert Math.abs(original.getAvgProbability() - loaded.getAvgProbability()) < 0.001 : "概率应该一致";
            assert original.getCount() == loaded.getCount() : "计数应该一致";
        }

        System.out.println("JSON往返测试通过");
    }
}
