package com.pixiv.crawler.service;

import com.pixiv.crawler.model.TagInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

// TODO: 在图形化界面需要提供修改用api，需可视化
public interface TagService {
    // 调用推理api
    public Map<String, Double> getTags(File imageFile) throws IOException;
    // 处理单张图片
    public void processImage(File imageFile, Map<String, TagInfo> tagMap) throws IOException;
    // 批量处理图片
    public void processImages(List<File> imageFiles, Map<String, TagInfo> tagMap) throws IOException;
    // 保存 tagmap 为 json 文件
    public void saveToJson(Map<String, TagInfo> tagMap) throws IOException;
    // 加载 json 文件到 tagmap
    public Map<String, TagInfo> loadFromJson() throws IOException;
    // 清理旧的 json
    public void clearJson();

    // 计算 tag 相似度
    public double calculateTagSimilarity(List<String> tags, Map<String, TagInfo> tagMap);
}
