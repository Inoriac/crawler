package com.pixiv.crawler.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 */
public class DateUtils {
    
    /**
     * 获取当前日期所在周的文件夹名称（周日至周六）
     * 格式：2025-01-19_2025-01-25
     * @return 周文件夹名称
     */
    public static String getCurrentWeekFolderName() {
        LocalDate today = LocalDate.now();
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);
        LocalDate saturday = today.with(DayOfWeek.SATURDAY);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return sunday.format(formatter) + "_" + saturday.format(formatter);
    }
    
    /**
     * 获取当前日期
     * @return 当前日期字符串，格式：yyyy-MM-dd
     */
    public static String getCurrentDate() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return today.format(formatter);
    }
}
