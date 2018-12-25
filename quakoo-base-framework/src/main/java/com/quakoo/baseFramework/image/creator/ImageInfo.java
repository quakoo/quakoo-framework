package com.quakoo.baseFramework.image.creator;

import java.awt.*;

/**
 * Created by 136249 on 2015/5/9.
 */
public class ImageInfo {
    //图片宽度
    private int width = 200;

    //图片高度
    private int height = 80;

    //外框颜色
    private Color rectColor=new Color(0, 0, 0);

    //背景色
    private Color bgColor=new Color(240, 251, 200);

    //干扰线数目
    private int lineNum = 0;

    //图片格式
    private String formatName = "JPG";

    //字体颜色
    private Color fontColor = new Color(0, 0, 0);

    //字体名称
    private String fontName = "宋体";

    //字体大小
    private int fontSize = 15;

    //文字旋转的弧度数
    private double radian = 0;
    private double rotateX = 0;
    private double rotateY = 0;

    //缩放
    private double scale = 1;

    public ImageInfo(int width, int height, int fontSize) {
        this.width = width;
        this.height = height;
        this.fontSize = fontSize;
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Color getRectColor() {
        return rectColor;
    }

    public void setRectColor(Color rectColor) {
        this.rectColor = rectColor;
    }

    public Color getBgColor() {
        return bgColor;
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public Color getFontColor() {
        return fontColor;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public double getRadian() {
        return radian;
    }

    public void setRadian(double radian) {
        this.radian = radian;
    }

    public double getRotateX() {
        return rotateX;
    }

    public void setRotateX(double rotateX) {
        this.rotateX = rotateX;
    }

    public double getRotateY() {
        return rotateY;
    }

    public void setRotateY(double rotateY) {
        this.rotateY = rotateY;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }
}
