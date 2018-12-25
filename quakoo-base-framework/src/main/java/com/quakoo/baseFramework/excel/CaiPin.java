package com.quakoo.baseFramework.excel;

import org.apache.poi.ss.usermodel.PictureData;

import java.util.List;

public class CaiPin extends ExcelObj {

    @ExcelInfo(readType = ExcelReadType.common, readRow = 2, readCol = 3)
    private String name;

    @ExcelList(step = 1, size = 4, clazz = ShiCai.class)
    private List<ShiCai> shiCais;

    @ExcelInfo(readType = ExcelReadType.common, readRow = 2, readCol = 7)
    private String desc;

    @ExcelInfo(readType = ExcelReadType.picture, readRow = 2, readCol = 8)
    private PictureData picOne;

    @ExcelInfo(readType = ExcelReadType.picture, readRow = 2, readCol = 9)
    private PictureData picTwo;

    @ExcelInfo(readType = ExcelReadType.picture, readRow = 2, readCol = 10)
    private PictureData picThree;

    @ExcelInfo(readType = ExcelReadType.picture, readRow = 2, readCol = 11)
    private PictureData picFour;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ShiCai> getShiCais() {
        return shiCais;
    }

    public void setShiCais(List<ShiCai> shiCais) {
        this.shiCais = shiCais;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public PictureData getPicOne() {
        return picOne;
    }

    public void setPicOne(PictureData picOne) {
        this.picOne = picOne;
    }

    public PictureData getPicTwo() {
        return picTwo;
    }

    public void setPicTwo(PictureData picTwo) {
        this.picTwo = picTwo;
    }

    public PictureData getPicThree() {
        return picThree;
    }

    public void setPicThree(PictureData picThree) {
        this.picThree = picThree;
    }

    public PictureData getPicFour() {
        return picFour;
    }

    public void setPicFour(PictureData picFour) {
        this.picFour = picFour;
    }

    @Override
    public String toString() {
        return "CaiPin{" +
                "name='" + name + '\'' +
                ", shiCais=" + shiCais +
                ", desc='" + desc + '\'' +
                ", picOne=" + picOne +
                ", picTwo=" + picTwo +
                ", picThree=" + picThree +
                ", picFour=" + picFour +
                ", step=" + step +
                '}';
    }
}
