package com.quakoo.baseFramework.util;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;



/**
 * 
 * @author liyongbiao
 *
 */
public class DateUtil {
    /**
     * 一分钟
     */
    public static final long LONG_ONE_MINUTE = 60000L;

    /**
     * 一小时
     */
    public static final long LONG_ONE_HOUR = 3600000L;

    /**
     * 一天
     */
    public static final long LONG_ONE_DAY = 86400000L;

    /**
     * 一月
     */
    public static final long LONG_ONE_MONTH = 86400000L * 30;

    /**
     * 一年
     */
    public static final long LONG_ONE_YEAR = 86400000L * 30 * 12;

    public static final String toRelativeString(long time) {
        double mills = System.currentTimeMillis() - time;

        long seconds = format(mills / 1000L);
        if (seconds < 60) {
            return seconds + "秒前";
        } else if (seconds == 60) {
            return "1分钟前";
        }
        long minutes = format(mills / LONG_ONE_MINUTE);
        if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes == 60) {
            return "1小时前";
        }
        long hours = format(mills / LONG_ONE_HOUR);
        if (hours < 24) {
            return hours + "小时前";
        } else if (hours == 24) {
            return "1天前";
        }
        long days = format(mills / LONG_ONE_DAY);
        if (days < 30) {
            return days + "天前";
        } else if (days == 30) {
            return "1月前";
        }
        long months = format(mills / LONG_ONE_MONTH);
        if (months < 12) {
            return months + "月前";
        } else if (months == 12) {
            return "1年前";
        }
        return (format(mills / LONG_ONE_YEAR)) + "年前";
    }

    private static final long format(double mill) {
        return Long.parseLong(new DecimalFormat("0").format(mill));
    }

    public static boolean equalsDay(Date d1, Date d2) {
        SimpleDateFormat st = new SimpleDateFormat("yyyyMMdd");
        int i = Integer.parseInt(st.format(d1));
        int i2 = Integer.parseInt(st.format(d2));
        return i == i2;
    }
    
    public static String parseDate(Date date,String pattern){
        SimpleDateFormat st = new SimpleDateFormat(pattern);
        String  ret =   st.format(date);
        return ret;
    }
    
    /**
     * 当前时间是否在该时间内
     * @param startHour
     * @param endHour
     * @return
     */
    public static boolean isInTime(int startHour,int endHour){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >=startHour && hour <= endHour) {
           return true;
        }
        return false;
    }
	public static long getLongFromString(String strTime, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date date;
		try {
			date = sdf.parse(strTime);
			long timeStr = date.getTime();
			return timeStr;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0l;
	} 
	
	public static int getWeekOfDate(Date dt) {
//        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        if(w == 0) w = 7;
        return w;
    }
	
	public static void main(String argv[]){
		System.out.println(Calendar.DAY_OF_WEEK);
		System.out.println(getWeekOfDate(new Date()));
	}
}
