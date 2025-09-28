package com.pixiv.crawler.model;

import com.pixiv.crawler.service.TagService;
import com.pixiv.crawler.service.impl.TagServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CharacterTagHolder {
    private static final CharacterTagHolder INSTANCE = new CharacterTagHolder();    // 单例
    private final List<String> characterTags;

    private CharacterTagHolder() {
        TagService tagService = new TagServiceImpl();
        try {
            this.characterTags = tagService.getPreferCharacterTags();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
    public static CharacterTagHolder getInstance() {
        return INSTANCE;
    }
    public List<String> getCharacterTags(){
        return characterTags;
    }
}
