import com.pixiv.crawler.util.JsonUtil;
import java.util.List;

/**
 * 测试两种格式的tags解析功能
 */
public class JsonTest {

    public static void main(String[] args) {
        System.out.println("=== 双格式Tags解析测试 ===");

        // 测试Popular作品格式
        testPopularFormat();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试相关推荐格式
        testRecommendFormat();
    }

    private static void testPopularFormat() {
        System.out.println("--- 测试Popular作品格式 ---");

        String popularJson = """
        {
            "body": {
                "tags": {
                    "tags": [
                        {
                            "tag": "\\u30b9\\u30ba\\u30e9\\u30f3(\\u30a2\\u30fc\\u30af\\u30ca\\u30a4\\u30c4)"
                        },
                        {
                            "tag": "\\u660e\\u65e5\\u65b9\\u821f"
                        }
                    ]
                }
            }
        }
        """;

        List<String> tags = JsonUtil.parseTags(popularJson);
        System.out.println("Popular格式解析结果:");
        for (int i = 0; i < tags.size(); i++) {
            System.out.println("  tag[" + i + "]: [" + tags.get(i) + "]");
        }
    }

    private static void testRecommendFormat() {
        System.out.println("--- 测试相关推荐格式 ---");

        String recommendJson = """
        {
            "body": {
                "illusts": [
                    {
                        "id": "71244389",
                        "title": "COMITIA126✦新刊",
                        "tags": [
                            "漫画",
                            "オリジナル",
                            "女子高生",
                            "百合",
                            "COMITIA126"
                        ]
                    }
                ]
            }
        }
        """;

        List<String> tags = JsonUtil.parseTags(recommendJson);
        System.out.println("相关推荐格式解析结果:");
        for (int i = 0; i < tags.size(); i++) {
            System.out.println("  tag[" + i + "]: [" + tags.get(i) + "]");
        }
    }
}