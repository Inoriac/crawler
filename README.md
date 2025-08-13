# Pixiv 爬虫项目

这是一个用于爬取 Pixiv 插画的 Java 项目，支持日榜爬取和相关推荐算法。

## 功能特性

### 1. 日榜爬取
- 爬取 Pixiv 日榜热门插画
- 支持多线程下载
- 自动处理 JPG/PNG 格式
- 避免重复下载

### 2. 相关推荐算法
基于算法思路实现的分层推荐系统：
- 从起始图片出发，爬取相关图片
- 按收藏数分层：1w+、5k~1w、3k~5k
- 智能选择下一轮起始图片
- 概率控制下载分布
- 递归爬取，最大深度可配置

## 使用方法

### 运行日榜爬取
```bash
mvn compile exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main"
```

### 运行相关推荐算法
```bash
# 使用默认参数
mvn compile exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main" -Dexec.args="recommend"

# 指定起始图片ID
mvn compile exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main" -Dexec.args="recommend 133461036"

# 指定起始图片ID、最大深度、每次获取图片数
mvn compile exec:java -Dexec.mainClass="com.pixiv.crawler.main.Main" -Dexec.args="recommend 133461036 3 10"
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

## 项目结构

```
crawler/
├── src/main/java/com/pixiv/crawler/
│   ├── main/
│   │   └── Main.java              # 主程序入口
│   ├── model/
│   │   └── PixivImage.java        # 图片数据模型
│   ├── service/
│   │   └── PixivCrawler.java      # 爬虫核心服务
│   └── util/
│       ├── ImageDownloader.java    # 图片下载工具
│       └── PixivRecHelper.java     # 相关推荐工具
├── downloads/                      # 下载文件目录
└── pom.xml                        # Maven 配置
```

## 配置说明

### 代理设置
项目使用本地代理 `127.0.0.1:7897`，请确保代理服务正常运行。

### Cookie 配置
在 `PixivRecHelper.java` 和 `PixivCrawler.java` 中配置有效的 Pixiv Cookie。

## 注意事项

1. 请遵守 Pixiv 的使用条款和爬虫协议
2. 建议设置适当的请求间隔，避免对服务器造成压力
3. 下载的图片仅供个人学习使用，请勿用于商业用途
4. 确保网络连接稳定，代理配置正确

## 依赖项

- Java 8+
- Maven 3.6+
- Jsoup (HTML 解析)
- Jackson (JSON 处理)
