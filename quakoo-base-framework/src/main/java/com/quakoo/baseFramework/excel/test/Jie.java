package com.quakoo.baseFramework.excel.test;

import com.quakoo.baseFramework.excel.ExcelInfo;
import com.quakoo.baseFramework.excel.ExcelObj;
import com.quakoo.baseFramework.excel.ExcelReadType;

public class Jie extends ExcelObj {

    @ExcelInfo(readRow = 2, readCol = 1, readType = ExcelReadType.common)
    private String monday;

    @ExcelInfo(readRow = 2, readCol = 2, readType = ExcelReadType.common)
    private String tuesday;

    @ExcelInfo(readRow = 2, readCol = 3, readType = ExcelReadType.common)
    private String wednesday;

    @ExcelInfo(readRow = 2, readCol = 4, readType = ExcelReadType.common)
    private String thursday;

    @ExcelInfo(readRow = 2, readCol = 5, readType = ExcelReadType.common)
    private String friday;

    @ExcelInfo(readRow = 2, readCol = 6, readType = ExcelReadType.common)
    private String saturday;

    @ExcelInfo(readRow = 2, readCol = 7, readType = ExcelReadType.common)
    private String sunday;

    public String getMonday() {
        return monday;
    }

    public void setMonday(String monday) {
        this.monday = monday;
    }

    public String getTuesday() {
        return tuesday;
    }

    public void setTuesday(String tuesday) {
        this.tuesday = tuesday;
    }

    public String getWednesday() {
        return wednesday;
    }

    public void setWednesday(String wednesday) {
        this.wednesday = wednesday;
    }

    public String getThursday() {
        return thursday;
    }

    public void setThursday(String thursday) {
        this.thursday = thursday;
    }

    public String getFriday() {
        return friday;
    }

    public void setFriday(String friday) {
        this.friday = friday;
    }

    public String getSaturday() {
        return saturday;
    }

    public void setSaturday(String saturday) {
        this.saturday = saturday;
    }

    public String getSunday() {
        return sunday;
    }

    public void setSunday(String sunday) {
        this.sunday = sunday;
    }

    @Override
    public String toString() {
        return "Jie{" +
                "monday='" + monday + '\'' +
                ", tuesday='" + tuesday + '\'' +
                ", wednesday='" + wednesday + '\'' +
                ", thursday='" + thursday + '\'' +
                ", friday='" + friday + '\'' +
                ", saturday='" + saturday + '\'' +
                ", sunday='" + sunday + '\'' +
                '}';
    }
}
