package com.ofwiki.dm.rocketmq.utils;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

public class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_NO_SECOND_FORMAT = "yyyy-MM-dd HH:mm";

    public static final String GENERAL_DATE_TIME_FORMAT = "y-M-d H:m:s";
    public static final String GENERAL_DATE_FORMAT = "y-M-d";
    public static final String GENERAL_SLASH_DATE_TIME_FORMAT = "y/M/d H:m:s";
    public static final String GENERAL_SLASH_DATE_FORMAT = "y/M/d";


    public static LocalDateTime toLocalDateTime(long milliseconds) {
        return Instant.ofEpochMilli(milliseconds).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
    }

    public static Date toDate(String source) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            return sf.parse(source);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }


    /**
     * 转换日期格式(年月日)的字符串为日期格式，否则返回supplier值
     *
     * @param date           如果为空，则返回supplier提供的值
     * @param formatter
     * @param localDateSupplier 如果为空抛出空指针异常
     * @return
     */
    public static LocalDate safeConvertLocalDate(String date, DateTimeFormatter formatter, Supplier<LocalDate> localDateSupplier) {
        Objects.requireNonNull(localDateSupplier);
        if (StringUtils.isBlank(date)) {
            return localDateSupplier.get();
        }
        try {
            return LocalDate.parse(date, formatter);
        } catch (Exception e) {
            return localDateSupplier.get();
        }
    }


    /**
     * 计算两个日期的时间间隔,返回xx天xx小时xx分钟xx秒
     * @param d1 第一个日期和时间
     * @param d2 第二个日期和时间
     * @return
     */
    public static String diff(LocalDateTime d1, LocalDateTime d2) {
        long begin= Timestamp.valueOf(d1).getTime(),end= Timestamp.valueOf(d2).getTime();
        long between=(end-begin)/1000;//除以1000是为了转换成秒
        long day1=between/(24*3600);
        long hour1=between%(24*3600)/3600;
        long minute1=between%3600/60;
        long second1=between%60;
        return day1+"天"+hour1+"小时"+minute1+"分"+second1+"秒";
    }

    public static LocalDate parseLocalDate(String date){
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalTime parseTime(String time, String format) {
        if(StringUtils.isNotEmpty(time)){
            try {
                return LocalTime.parse(time, DateTimeFormatter.ofPattern(format));
            } catch (Exception e){
                logger.warn("时间" + time + "默认格式" + format + "转换异常", e);
            }
        }
        return null;
    }

    public static LocalDateTime parse(String time, String... formats) {
        if(StringUtils.isNotBlank(time)){
            String timeStr = time.trim();
            for (String format : formats) {
                try {
                    return LocalDateTime.parse(time, DateTimeFormatter.ofPattern(format));
                } catch (Exception e){
                    logger.warn("日期" + time + "默认格式" + format + "转换异常", e);
                }
            }
            try {
                if(timeStr.contains("-")){
                    if(timeStr.contains(" ")){
                        return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern(GENERAL_DATE_TIME_FORMAT));
                    }else{
                        return LocalDate.parse(timeStr, DateTimeFormatter.ofPattern(GENERAL_DATE_FORMAT)).atStartOfDay();
                    }
                }else if(timeStr.contains("/")){
                    if(timeStr.contains(" ")){
                        return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern(GENERAL_SLASH_DATE_TIME_FORMAT));
                    }else{
                        return LocalDate.parse(timeStr, DateTimeFormatter.ofPattern(GENERAL_SLASH_DATE_FORMAT)).atStartOfDay();
                    }
                }
            }catch (Exception e){
                logger.warn("日期" + time + "格式转换异常",e);
            }
        }
        return null;
    }

    public static String format(LocalDateTime dateTime, String format){
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(format));
    }

    public static String format(LocalDateTime dateTime){
        return format(dateTime, DEFAULT_DATE_TIME_FORMAT);
    }

    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }
}
