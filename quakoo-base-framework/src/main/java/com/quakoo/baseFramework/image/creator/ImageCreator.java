package com.quakoo.baseFramework.image.creator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Random;

/**
 * Created by 136249 on 2015/5/9.
 */
public class ImageCreator {

    private static Random rnd = new Random(new Date().getTime());

    public static void draw(ImageInfo imageInfo, Graphics2D g, String text) {
        //文字旋转
        g.rotate(Math.toRadians(imageInfo.getRadian()), imageInfo.getRotateX(), imageInfo.getRotateY());
        g.scale(imageInfo.getScale(), imageInfo.getScale());

        g.setColor(imageInfo.getFontColor());
        Font font = new Font(imageInfo.getFontName(), Font.PLAIN, imageInfo.getFontSize());
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics(font);
        int fontHeight = fm.getHeight(); //字符的高度

        int offsetLeft = 0;
        int rowIndex = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int charWidth = fm.charWidth(c); //字符的宽度

            //另起一行
            if (Character.isISOControl(c) || offsetLeft >= (imageInfo.getWidth() - charWidth)) {
                rowIndex++;
                offsetLeft = 0;
            }

            g.drawString(String.valueOf(c), offsetLeft, rowIndex * fontHeight);
            offsetLeft += charWidth;
        }
    }

    /**
     * 画干扰线
     */
    private static void drawRandomLine(Graphics graph, ImageInfo imageInfo) {
        for (int i = 0; i < imageInfo.getLineNum(); i++) {
            //线条的颜色
            graph.setColor(getRandomColor(100, 155));

            //线条两端坐标值
            int x1 = rnd.nextInt(imageInfo.getWidth());
            int y1 = rnd.nextInt(imageInfo.getHeight());

            int x2 = rnd.nextInt(imageInfo.getWidth());
            int y2 = rnd.nextInt(imageInfo.getHeight());

            //画线条
            graph.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 随机获取颜色对象
     */
    private static Color getRandomColor(int base, int range) {
        if ((base + range) > 255) range = 255 - base;
        int red = base + rnd.nextInt(range);
        int green = base + rnd.nextInt(range);
        int blue = base + rnd.nextInt(range);
        return new Color(red, green, blue);
    }


    public static byte[] createImage(String text, ImageInfo imageInfo) throws Exception {
        BufferedImage image = new BufferedImage(imageInfo.getWidth(), imageInfo.getHeight(), BufferedImage.TYPE_INT_RGB);
        //获取画布
        Graphics2D g = (Graphics2D) image.getGraphics();
        //画长方形
        g.setColor(imageInfo.getBgColor());
        g.fillRect(0, 0, imageInfo.getWidth(), imageInfo.getHeight());
        //外框
        g.setColor(imageInfo.getRectColor());
        g.drawRect(0, 0, imageInfo.getWidth() - 1, imageInfo.getHeight() - 1);
        //画干扰线
        drawRandomLine(g, imageInfo);
        //画字符串
        draw(imageInfo, g, text);
        //执行
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "JPG", out);
        byte[] b = out.toByteArray();
        return b;
    }
}
