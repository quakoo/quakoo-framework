package com.quakoo.baseFramework.util;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author liyongbiao
 *
 */
public class ScheduleUtils {
    public final static String[] units = new String[] { "月", "天", "小时", "分钟" };

    public final static String[] units2 = new String[] { "月", "日", "点", "分" };

    public static String getSchedule(Date date) {
        int month = date.getMonth() + 1;
        int day = date.getDate();
        int hour = date.getHours();
        int minute = date.getMinutes();
        return getSchedule(month, day, hour, minute, null);
    }

    public static String getSchedule(int month, int day, int hour, int minute, LoopIndex loop) {

        int[] times = new int[] { month, day, hour, minute };

        String exp = "?";

        for (int i = 0; i < times.length; i++) {
            int time = times[i];
            String f = "";

            if (loop != null && loop.getIndex() == i)
                f = "*/";

            if (time <= 0) {
                exp = "* " + exp;
            } else {
                exp = f + time + " " + exp;
            }
        }

        return exp;

    }

    private enum LoopIndex {
        month(0), day(1), hour(2), minute(3);

        private int index;

        public static final Map<Integer, LoopIndex> lookup = new HashMap<Integer, LoopIndex>();

        static {
            for (final LoopIndex i : EnumSet.allOf(LoopIndex.class)) {
                lookup.put(i.getIndex(), i);
            }
        }

        private LoopIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public static LoopIndex getLoopIndex(int index) {
            return lookup.get(index);
        }

    }

    private static int[] getTimes(String schedule) {
        String[] exps = schedule.trim().split(" ");

        int minute = getTime(exps[0]);
        int hour = getTime(exps[1]);
        int day = getTime(exps[2]);
        int month = getTime(exps[3]);
        return new int[] { month, day, hour, minute };
    }

    private static int getTime(String exp) {

        if (exp.length() == 1) {
            if (exp.equals("*")) {
                return 0;
            } else {
                return Integer.parseInt(exp);
            }
        } else {
            if (exp.indexOf("/") > 0) {
                exp = exp.substring(exp.indexOf("/") + 1, exp.length());
                return Integer.parseInt(exp);
            } else {
                return Integer.parseInt(exp);
            }
        }
    }

    private static LoopIndex getLoopIndex(String schedule) {
        LoopIndex loop = null;
        String[] exps = schedule.trim().split(" ");

        for (int i = 0; i < exps.length - 1; i++) {
            if (exps[i].indexOf("*") < 0 || exps[i].indexOf("*/") >= 0) {
                loop = LoopIndex.getLoopIndex(i);
            }
        }
        return loop;
    }

    public static String getExpression(String schedule) {

        String result = "";

        int[] times = getTimes(schedule);
        LoopIndex loop = getLoopIndex(schedule);

        if (loop != null) {
            int flag = 0;
            for (int i = 0; i < times.length; i++) {
                int time = times[i];

                // System.out.println("@@@@" + time);

                if (time != 0) {
                    if (loop.getIndex() == i + 1) {
                        result += "每" + time + units[i];
                    } else {
                        result += time + units[i];
                    }
                } else {
                    flag += 1;
                    if (flag <= 1) {
                        result += "每" + units[i];
                    } else {
                        flag = 0;
                        result = "每" + units[i];
                    }

                }
            }
        } else {
            StringBuilder sb=new StringBuilder(result);
            for (int i = 0; i < times.length; i++) {
                int time = times[i];
                sb.append(time).append(units2[i]);
            }
            result=sb.toString();
        }

        return result;
    }

    public static void main(String args[]) {

        // 当前时间
        System.out.println(ScheduleUtils.getSchedule(new Date()));

        // 4月14日3点13分
        System.out.println(ScheduleUtils.getExpression("13 14 3 4 ?"));

        // 9月3日每2小时13分钟
        System.out.println(ScheduleUtils.getExpression("13 */2 3 9 ?"));

        // 10 */5 * * ?
        System.out.println(ScheduleUtils.getSchedule(0, 0, 5, 10, LoopIndex.hour));

        // 10 5 * * ?
        System.out.println(ScheduleUtils.getSchedule(0, 0, 5, 10, LoopIndex.day));

        // 每5小时10分钟
        System.out.println(ScheduleUtils.getExpression("10 5 * * ?"));

        // 2月每天5小时10分钟
        System.out.println(ScheduleUtils.getExpression("10 5 * 2 ?"));

        // 每小时10分钟
        System.out.println(ScheduleUtils.getExpression("*/10 * * * ?"));

        // */10 * * * ?
        System.out.println(ScheduleUtils.getSchedule(0, 0, 0, 10, LoopIndex.minute));

        // 10 5 * */2 ?
        System.out.println(ScheduleUtils.getSchedule(2, 0, 5, 10, LoopIndex.month));
    }
}
