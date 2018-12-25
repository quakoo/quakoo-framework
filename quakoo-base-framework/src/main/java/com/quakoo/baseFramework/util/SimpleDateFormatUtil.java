package com.quakoo.baseFramework.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleDateFormatUtil {

    public static class SimpleDateFormatUtilResult {
        private Date date;

        private SimpleDateFormat sdf;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public SimpleDateFormat getSdf() {
            return sdf;
        }

        public void setSdf(SimpleDateFormat sdf) {
            this.sdf = sdf;
        }

        public SimpleDateFormatUtilResult(Date date, SimpleDateFormat sdf) {
            super();
            this.date = date;
            this.sdf = sdf;
        }

    }

    public static SimpleDateFormatUtilResult parseDate(List<SimpleDateFormat> list, String dateStr, boolean synch) {
        for (SimpleDateFormat sdf : list) {
            try {
                Date date = null;
                if (synch) {
                    synchronized (sdf) {
                        date = sdf.parse(dateStr);
                    }
                } else {
                    date = sdf.parse(dateStr);
                }
                return new SimpleDateFormatUtilResult(date, sdf);
            } catch (Exception e) {
            }
        }
        return null;
    }

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static SimpleDateFormat sdf0 = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    static SimpleDateFormat sdf1 = new SimpleDateFormat("MM-dd HH:mm");

    static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

    static SimpleDateFormat sdf3 = new SimpleDateFormat("前天 HH:mm");

    static SimpleDateFormat sdf4 = new SimpleDateFormat("昨天 HH:mm");

    static SimpleDateFormat sdf5 = new SimpleDateFormat("今天 HH:mm");

    static SimpleDateFormat sdf6 = new SimpleDateFormat("yyyy年MM月dd日");

    static SimpleDateFormat sdf7 = new SimpleDateFormat("MM月dd日 HH:mm");

    static SimpleDateFormat sdf8 = new SimpleDateFormat("MM月dd日 HH:mm");

    static SimpleDateFormat sdf9 = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);

    static Pattern hourPattern = Pattern.compile("([\\d]+)小时前");

    static Pattern dayPattern = Pattern.compile("([\\d]+)天前");

    static Pattern monthPattern = Pattern.compile("([\\d]+)个月前");

    static Pattern monthPattern1 = Pattern.compile("([\\d]+)月前");

    static Pattern yearPattern = Pattern.compile("([\\d]+)年前");

    static Pattern minPattern = Pattern.compile("([\\d]+)分钟前");

    static Pattern minPattern1 = Pattern.compile("刚刚");

    static List<SimpleDateFormat> list = new ArrayList<SimpleDateFormat>();
    static {
        list.add(sdf);
        list.add(sdf0);
        list.add(sdf1);
        list.add(sdf2);
        list.add(sdf3);
        list.add(sdf4);
        list.add(sdf5);
        list.add(sdf6);
        list.add(sdf7);
        list.add(sdf8);
        list.add(sdf9);
    }

    public static Date paresDateLikeYouku(String dateStr) throws Exception {
        Date nowDate = new Date();
        SimpleDateFormatUtilResult sdfur = parseDate(list, dateStr, true);
        if (sdfur == null) {
            Matcher hourMatcher = hourPattern.matcher(dateStr);
            if (hourMatcher.find()) {
                int bhour = Integer.parseInt(hourMatcher.group(1));
                nowDate.setHours(nowDate.getHours() - bhour);
                return nowDate;
            }

            Matcher dayMatcher = dayPattern.matcher(dateStr);
            if (dayMatcher.find()) {
                int bday = Integer.parseInt(dayMatcher.group(1));
                nowDate.setDate(nowDate.getDay() - bday);
                return nowDate;
            }

            Matcher monthMatcher = monthPattern.matcher(dateStr);
            if (monthMatcher.find()) {
                int bday = Integer.parseInt(monthMatcher.group(1));
                nowDate.setMonth(nowDate.getMonth() - bday);
                return nowDate;
            }

            Matcher monthMatcher1 = monthPattern1.matcher(dateStr);
            if (monthMatcher1.find()) {
                int bday = Integer.parseInt(monthMatcher1.group(1));
                nowDate.setMonth(nowDate.getMonth() - bday);
                return nowDate;
            }

            Matcher yearPatternMatcher = yearPattern.matcher(dateStr);
            if (yearPatternMatcher.find()) {
                int bday = Integer.parseInt(yearPatternMatcher.group(1));
                nowDate.setYear(nowDate.getYear() - bday);
                return nowDate;
            }

            Matcher minPatternMatcher = minPattern.matcher(dateStr);
            if (minPatternMatcher.find()) {
                int bday = Integer.parseInt(minPatternMatcher.group(1));
                nowDate.setMinutes(nowDate.getMinutes() - bday);
                return nowDate;
            }

            Matcher minPattern1Matcher = minPattern1.matcher(dateStr);
            if (minPattern1Matcher.find()) {
                return nowDate;
            }

            throw new Exception("不能解析这个日期:" + dateStr);
        }
        if (sdfur.getSdf() == sdf1 || sdfur.getSdf() == sdf7 || sdfur.getSdf() == sdf8) {
            sdfur.getDate().setYear(nowDate.getYear());
        }
        if (sdfur.getSdf() == sdf3) {
            sdfur.getDate().setYear(nowDate.getYear());
            sdfur.getDate().setMonth(nowDate.getMonth());
            sdfur.getDate().setDate(nowDate.getDate() - 2);
        }
        if (sdfur.getSdf() == sdf4) {
            sdfur.getDate().setYear(nowDate.getYear());
            sdfur.getDate().setMonth(nowDate.getMonth());
            sdfur.getDate().setDate(nowDate.getDate() - 1);
        }
        if (sdfur.getSdf() == sdf5) {
            sdfur.getDate().setYear(nowDate.getYear());
            sdfur.getDate().setMonth(nowDate.getMonth());
            sdfur.getDate().setDate(nowDate.getDate());
        }

        return sdfur.getDate();
    }

    public static void main(String[] sdg) throws Exception {
        // String data = "Sun Nov 11 11:43:03 2012";
        // SimpleDateFormat sdf = new
        // SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        // System.out.println(sdf.parse(data));
        // System.out.println(SimpleDateFormatUtil.paresDateLikeYouku("2天前"));
        // -javaagent:E:\web-framework\framework-common\target\framework-common-1.0-SNAPSHOT.jar
        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        // System.out.println(sdf.format(new Date(1355139845549l)));
        // System.out.println(sdf.parse("2013-02-20 20:00").getTime());
        System.out.println(new Date(1390616513971l));
        System.out.println(SimpleDateFormatUtil.paresDateLikeYouku("Thu Nov 21 23:23:27 2013"));
    }

}
