package com.pixiv.crawler.service;

import com.pixiv.crawler.model.TagInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

// TODO: 在图形化界面需要提供修改偏好tag用 api，需可视化
public interface TagService {
    // 调用推理api
    public Map<String, Double> getTags(File imageFile) throws IOException;
    // 处理单张图片
    public void processImage(File imageFile) throws IOException;
    // 批量处理图片
    public void processImages(List<File> imageFiles) throws IOException;

    // 获取用户偏好角色词条
    public List<String> getPreferCharacterTags() throws IOException;
    // 保存用户偏好角色词
    public void saveCharacterTagToJson(List<String> characters);

    // 保存 tagmap 为 json 文件
    public void saveToJson();
    // 加载 json 文件到 tagmap
    public Map<String, TagInfo> loadFromJson() throws IOException;
    // 清理旧的 json
    public void clearJson();

    // 计算 tag 相似度，用于衡量图片匹配度
    public double calculateTagSimilarity(List<String> tags);
    // 通过p站搜索api获取相近tag
    public List<String> getSimilarTagByApi(String tag) throws IOException;
    // 本地 tag 映射
    public List<String> findFromLocalMapping(String tag);
}
