package com.pixiv.crawler.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PixivImage {
    @JsonProperty("id")
    private String id;
    @JsonProperty("title")
    private String title;
    @JsonProperty("url")
    private String url;
    @JsonProperty("tags")
    private List<String> tags;
    @JsonProperty("artist")
    private String artist;
    @JsonProperty("viewCount")
    private int viewCount;
    @JsonProperty("likeCount")
    private int likeCount;
    @JsonProperty("bookmarkCount")
    private int bookmarkCount;
    private boolean isR18;
    private boolean isManga;
    @JsonProperty("pageCount")
    private int pageCount;

    // 构造函数
    public PixivImage() {
        this.tags = new ArrayList<>();
        this.pageCount = 1; // 默认为1页
    }

    public PixivImage(String id, String title, String url, List<String> tags, String artist) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.artist = artist;
        this.pageCount = 1; // 默认为1页
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(int bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }

    public boolean isR18() {
        return isR18;
    }

    public void setR18(boolean r18) {
        isR18 = r18;
    }
    
    public boolean isManga() {
        return isManga;
    }

    public void setManga(boolean manga) {
        isManga = manga;
    }
    
    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}
