# Pixiv 爬虫项目

这是一个用于爬取 Pixiv 插画的 Java 项目，支持日榜爬取和相关推荐算法。

## 功能特性

### 1. 日榜爬取
- 爬取 Pixiv 日榜热门插画
- 支持多线程下载
- 自动处理 JPG/PNG 格式
- 按周自动分类保存
- 避免重复下载

### 2. 相关推荐算法
基于算法思路实现的分层推荐系统：
- 从起始图片出发，爬取相关图片
- 按收藏数分层：1w+、5k~1w、3k~5k
- 智能选择下一轮起始图片
- 概率控制下载分布
- 递归爬取，最大深度可配置

### 3. 内容过滤功能
- **R-18 内容控制**：可配置是否下载 R-18 作品
- **漫画作品排除**：可配置是否排除包含"漫画"标签的作品
- 自动分类保存到不同文件夹

## 项目结构

```
crawler/
├── src/main/java/com/pixiv/crawler/
│   ├── main/
│   │   └── Main.java              # 主程序入口
│   ├── config/
│   │   └── PixivCrawlerConfig.java # 配置文件
│   ├── model/
│   │   └── PixivImage.java        # 图片数据模型
│   ├── service/
│   │   └── PixivCrawler.java      # 爬虫核心服务
│   └── util/
│       ├── ImageDownloader.java    # 图片下载工具
│       ├── Downloader.java         # 下载管理器
│       ├── PixivRecHelper.java     # 相关推荐工具
│       ├── DateUtils.java          # 日期工具
│       └── JsonUtil.java           # JSON处理工具
├── downloads/                      # 下载文件目录
│   ├── ranking/                    # 日榜图片目录
│   └── recommendations/            # 相关推荐图片目录
└── pom.xml                        # Maven 配置
```

## 配置说明

### 核心配置文件：`PixivCrawlerConfig.java`

#### 网络配置
```java
// VPN 端口号
public static final int PORT = 7897;
// 用户p站的cookie，用于绕过反爬
public static final String COOKIE = "";
```

#### 爬取参数配置
```java
// 起始图片 id
public static final String START_PID = "127455493";

// 各队列的选取倾向概率（总和应为1.0）
public static final double TOP1W_SELECTION_PROBABILITY = 0.5;
public static final double TOP5K_SELECTION_PROBABILITY = 0.3;
public static final double TOP3K_SELECTION_PROBABILITY = 0.2;

// 搜索深度
public static final int MAX_DEPTH = 4;
// 每轮选择的起始图片数量
public static final int START_IMAGES_PER_ROUND = 3;
// 队列满时的处理阈值
public static final int QUEUE_PROCESS_THRESHOLD = 10;
```

#### 文件保存配置
```java
// 基础下载路径
public static final String BASE_SAVE_PATH = "downloads";
// 日榜图片基础下载路径（不包含周文件夹）
public static final String RANKING_BASE_PATH = BASE_SAVE_PATH + "/ranking";
// 相关推荐图片基础下载路径（不包含日期和收藏数文件夹）
public static final String RECOMMENDATIONS_BASE_PATH = BASE_SAVE_PATH + "/recommendations";
// 相关推荐图片按收藏数分类的文件夹名称
public static final String TOP1W_FOLDER = "1w+";
public static final String TOP5K_FOLDER = "5k-1w";
public static final String TOP3K_FOLDER = "3k-5k";
```

#### 内容过滤配置
```java
// R-18下载开关
public static final boolean R18_DOWNLOAD_ENABLED = true;
// R-18作品文件夹名称
public static final String R18_FOLDER = "r-18";
// 普通作品文件夹名称
public static final String NORMAL_FOLDER = "normal";

// 漫画排除开关
public static final boolean MANGA_EXCLUDE_ENABLED = true;
// 漫画标签关键词
public static final String MANGA_TAG_KEYWORD = "漫画";
```

## 使用方法

### 环境要求
- Java 17+
- Maven 3.6+
- 本地代理服务（默认端口：7897）

### 编译项目
```bash
cd crawler
mvn clean compile
```

### 运行日榜爬取
```bash
mvn exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main"
```

### 运行相关推荐算法
```bash
# 使用默认参数
mvn exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main" -Dexec.args="recommend"

# 指定起始图片ID
mvn exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main" -Dexec.args="recommend 133461036"

# 指定起始图片ID、最大深度、每次获取图片数
mvn exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main" -Dexec.args="recommend 133461036 3 10"
```

## 算法思路

### 相关推荐算法流程
1. **起始阶段**：从指定图片ID出发，爬取其相关图片的ID（默认10张）
2. **分类阶段**：获取每个图片的收藏数，按收藏数排序分到对应队列
3. **选择阶段**：选取3张图片作为下一轮的起始待选图片，通过概率控制分布
4. **下载阶段**：当队列超过10张时提交下载任务，保留第2张和第7张作为基础
5. **递归阶段**：重复以上步骤直到达到最大深度
6. **结束阶段**：提交所有剩余队列的下载任务

### 分层策略
- **1w+队列**：收藏数 ≥ 10000
- **5k~1w队列**：收藏数 5000-9999
- **3k~5k队列**：收藏数 3000-4999
- **弃用**：收藏数 < 3000

### 概率控制
- 使用递减概率分布选择下一轮起始图片
- 前面的图片被选中的概率更高
- 确保下载图片的多样性

## 文件保存结构

### 日榜图片
```
downloads/ranking/
└── 2025-W01/          # 按周分类
    ├── image1.jpg
    ├── image2.png
    └── ...
```

### 相关推荐图片
```
downloads/recommendations/
└── 2025-01-24/        # 按日期分类
    ├── 1w+/           # 收藏数1w+
    │   ├── image1.jpg
    │   └── ...
    ├── 5k-1w/         # 收藏数5k-1w
    │   ├── image2.jpg
    │   └── ...
    └── 3k-5k/         # 收藏数3k-5k
        ├── image3.jpg
        └── ...
```

## 配置注意事项

### 1. 代理设置
- 项目使用本地代理 `127.0.0.1:7897`
- 请确保代理服务正常运行
- 可在 `PixivCrawlerConfig.java` 中修改 `PORT` 值

### 2. Cookie 配置
- 在 `PixivCrawlerConfig.java` 中配置有效的 Pixiv Cookie
- Cookie 用于绕过反爬虫机制
- 建议定期更新 Cookie

### 3. 概率配置
- 确保 `TOP1W_SELECTION_PROBABILITY + TOP5K_SELECTION_PROBABILITY + TOP3K_SELECTION_PROBABILITY = 1.0`
- 可根据需要调整各队列的选择概率

### 4. 性能调优
- `MAX_DEPTH`：控制爬取深度，建议不超过4
- `START_IMAGES_PER_ROUND`：每轮起始图片数量
- `QUEUE_PROCESS_THRESHOLD`：队列处理阈值

## 依赖项

- **Jsoup** (1.17.2) - HTML 解析
- **OkHttp** (4.12.0) - HTTP 请求
- **Jackson** (2.16.1) - JSON 处理
- **Commons IO** (2.15.1) - 文件操作
- **SLF4J** (2.0.9) - 日志记录

## 漫画排除功能

### 功能概述
新增的漫画排除功能可以自动识别并排除包含"漫画"标签的作品，避免下载漫画类型的内容。

### 工作原理
1. **标签检测**：在解析 AJAX JSON 响应时，检查作品的 `tags` 字段
2. **关键词匹配**：如果标签中包含"漫画"关键词，则标记为漫画作品
3. **排除处理**：在下载过程中自动跳过漫画作品

### 应用范围
- **相关推荐算法**：在推荐图片分类和下载时排除漫画作品
- **日榜爬取**：在日榜图片下载时排除漫画作品（需要额外API调用）

### 配置选项
- `MANGA_EXCLUDE_ENABLED`：控制是否启用漫画排除功能
- `MANGA_TAG_KEYWORD`：设置漫画标签的关键词（默认为"漫画"）

### 日志输出
程序运行时会输出漫画检测和排除的详细信息：

**相关推荐算法：**
```
【漫画检测】作品 123456 被标记为漫画 (标签: 漫画)
【分类】123456 -> 漫画作品，已排除 (收藏数: 5000)
【1w+】排除2个漫画作品的下载（漫画排除已启用）
```

**日榜爬取：**
```
【日榜】作品 123456 为漫画作品，已排除
【日榜】总共解析到 50 个作品
【日榜】排除 3 个漫画作品（漫画排除已启用）
【日榜】实际下载 47 个作品
```

### 测试验证
可以通过运行测试来验证漫画检测功能：
```bash
mvn test -Dtest=PixivRecHelperTest#testMangaTagDetection
```

## 注意事项

1. **合规使用**：请遵守 Pixiv 的使用条款和爬虫协议
2. **请求频率**：建议设置适当的请求间隔，避免对服务器造成压力
3. **版权保护**：下载的图片仅供个人学习使用，请勿用于商业用途
4. **网络稳定**：确保网络连接稳定，代理配置正确
5. **存储空间**：注意磁盘空间，及时清理不需要的文件
6. **内容过滤**：R-18 和漫画排除功能可以独立配置

## 故障排除

### 常见问题
1. **代理连接失败**：检查代理服务是否正常运行
2. **Cookie 失效**：更新 `PixivCrawlerConfig.java` 中的 Cookie
3. **下载失败**：检查网络连接和磁盘空间
4. **概率配置错误**：确保三个概率值之和为1.0

### 日志查看
程序运行时会输出详细的日志信息，包括：
- 爬取进度
- 下载状态
- 错误信息
- 文件保存路径
