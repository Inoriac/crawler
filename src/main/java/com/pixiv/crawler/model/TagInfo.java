package com.pixiv.crawler.model;

public class TagInfo {
    private double avgProbability;  // 平均概率
    private int count;              // 出现次数

    public TagInfo() {}

    public TagInfo(double prob){
        this.avgProbability = prob;
        count = 1;
    }

    public TagInfo(double prob, int count){
        this.avgProbability = prob;
        this.count = count;
    }

    public double getAvgProbability(){
        return avgProbability;
    }

    public int getCount(){
        return count;
    }

    public void update(double newProb){
        // 计算加权平均
        avgProbability = (avgProbability * count + newProb) / (count + 1);
        count++;
    }
}
