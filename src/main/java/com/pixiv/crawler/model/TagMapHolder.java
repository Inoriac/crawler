package com.pixiv.crawler.model;

import com.pixiv.crawler.service.TagService;
import com.pixiv.crawler.service.impl.TagServiceImpl;

import java.io.IOException;
import java.util.Map;

public class TagMapHolder {
    private static final TagMapHolder INSTANCE = new TagMapHolder();    // 单例
    private final Map<String, TagInfo> tagMap;

    private TagMapHolder() {
        try {
            TagService tagService = new TagServiceImpl();
            this.tagMap = tagService.loadFromJson();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
    public static TagMapHolder getInstance() {
        return INSTANCE;
    }
    public Map<String, TagInfo> getTagMap(){
        return tagMap;
    }
}
