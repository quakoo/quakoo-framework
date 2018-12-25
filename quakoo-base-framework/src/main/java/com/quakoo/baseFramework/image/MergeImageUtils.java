package com.quakoo.baseFramework.image;



import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MergeImageUtils {

    /**
     * 图片的间隙
     */
    private static final int SIDE = 6;
    /**
     * 画板尺寸
     */
    private static final int CANVANS_W = 600;
    private static final int CANVANS_H = 600;

    /**
     * 尺寸1 （小）
     */
    private static final int ONE_IMAGE_SIZE = CANVANS_H - (2 * SIDE);
    /**
     * 尺寸2 （中）
     */
    private static final int TWO_IMAGE_SIZE = (CANVANS_H - (3 * SIDE)) / 2;
    /**
     * 尺寸3 （大）
     */
    private static final int FIVE_IMAGE_SIZE = (CANVANS_H - (4 * SIDE)) / 3;

    public static byte[] merge(List<String> paths)
            throws IOException {
        List<BufferedImage> bufferedImages = new ArrayList<>();

        int imageSize = 0;
        if (paths.size() <= 1) {
            //若为一张图片
            imageSize = ONE_IMAGE_SIZE;
        } else if (paths.size() > 1 && paths.size() < 5) {
            //若为2-4张图片

            imageSize = TWO_IMAGE_SIZE;
        } else {
            //若>=5张图片
            imageSize = FIVE_IMAGE_SIZE;
        }

        for (int i = 0; i < paths.size(); i++) {
            BufferedImage resize2 = resize2(paths.get(i), imageSize, imageSize, true);
            if(null != resize2) bufferedImages.add(resize2);
        }

        BufferedImage outImage = new BufferedImage(CANVANS_W, CANVANS_H, BufferedImage.TYPE_INT_RGB);

        // 生成画布
        Graphics g = outImage.getGraphics();

        Graphics2D g2d = (Graphics2D) g;

        // 设置背景色
        g2d.setBackground(new Color(231, 231, 231));

        // 通过使用当前绘图表面的背景色进行填充来清除指定的矩形。
        g2d.clearRect(0, 0, CANVANS_W, CANVANS_H);

        // 开始拼凑 根据图片的数量判断该生成那种样式的组合头像目前为九种

        for (int i = 1; i <= bufferedImages.size(); i++) {
            Integer size = bufferedImages.size();
            switch (size) {
                case 1:
                    g2d.drawImage(bufferedImages.get(i - 1), SIDE, SIDE, null);
                    break;
                case 2:
                    if (i == 1) {
                        g2d.drawImage(bufferedImages.get(i - 1), SIDE, (CANVANS_W - imageSize) / 2, null);
                    } else {
                        g2d.drawImage(bufferedImages.get(i - 1), 2 * SIDE + imageSize, (CANVANS_W - imageSize) / 2, null);
                    }
                    break;
                case 3:
                    if (i == 1) {
                        g2d.drawImage(bufferedImages.get(i - 1), (CANVANS_W - imageSize) / 2, SIDE, null);
                    } else {
                        g2d.drawImage(bufferedImages.get(i - 1), (i - 1) * SIDE + (i - 2) * imageSize, imageSize + (2 * SIDE), null);

                    }
                    break;
                case 4:

                    if (i <= 2) {
                        g2d.drawImage(bufferedImages.get(i - 1), i * SIDE + (i - 1) * imageSize, SIDE, null);

                    } else {
                        g2d.drawImage(bufferedImages.get(i - 1), (i - 2) * SIDE + (i - 3) * imageSize, imageSize + 2 * SIDE, null);

                    }
                    break;
                case 5:
                    if (i <= 2) {
                        g2d.drawImage(bufferedImages.get(i - 1), (CANVANS_W - 2 * imageSize - SIDE) / 2 + (i - 1) * imageSize + (i - 1) * SIDE, (CANVANS_W - 2 * imageSize - SIDE) / 2, null);

                    } else {
                        g2d.drawImage(bufferedImages.get(i - 1), (i - 2) * SIDE + (i - 3) * imageSize, ((CANVANS_W - 2 * imageSize - SIDE) / 2) + imageSize + SIDE, null);

                    }
//
                    break;
                case 6:
                    if (i <= 3) {
                        g2d.drawImage(bufferedImages.get(i - 1), SIDE * i + imageSize * (i - 1), (CANVANS_W - 2 * imageSize - SIDE) / 2, null);
                    } else {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 3) * SIDE) + ((i - 4) * imageSize), ((CANVANS_W - 2 * imageSize - SIDE) / 2) + imageSize + SIDE, null);
                    }
                    break;
                case 7:
                    if (i <= 1) {
                        g2d.drawImage(bufferedImages.get(i - 1), 2 * SIDE + imageSize, SIDE, null);
                    }
                    if (i <= 4 && i > 1) {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 1) * SIDE) + ((i - 2) * imageSize), 2 * SIDE + imageSize, null);
                    }
                    if (i <= 7 && i > 4) {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 4) * SIDE) + ((i - 5) * imageSize), 3 * SIDE + 2 * imageSize, null);
                    }
                    break;
                case 8:
                    if (i <= 2) {
                        g2d.drawImage(bufferedImages.get(i - 1), (CANVANS_W - 2 * imageSize - SIDE) / 2 + (i - 1) * imageSize + (i - 1) * SIDE, SIDE, null);
                    }
                    if (i <= 5 && i > 2) {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 2) * SIDE) + ((i - 3) * imageSize), 2 * SIDE + imageSize, null);
                    }
                    if (i <= 8 && i > 5) {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 5) * SIDE) + ((i - 6) * imageSize), 3 * SIDE + 2 * imageSize, null);
                    }
                    break;
                case 9:
                    if (i <= 3) {
                        g2d.drawImage(bufferedImages.get(i - 1), (i * SIDE) + ((i - 1) * imageSize), SIDE, null);
                    }
                    if (i <= 6 && i > 3) {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 3) * SIDE) + ((i - 4) * imageSize), 2 * SIDE + imageSize, null);
                    }
                    if (i <= 9 && i > 6) {
                        g2d.drawImage(bufferedImages.get(i - 1), ((i - 6) * SIDE) + ((i - 7) * imageSize), 3 * SIDE + 2 * imageSize, null);
                    }
                    break;
                default:
                    break;
            }
        }
        String format = "png";
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        ImageIO.write(outImage, format, swapStream);
        return swapStream.toByteArray();
    }

    private static BufferedImage resize2(String filePath, int height, int width,
                                        boolean bb) {
        try {
            double ratio = 0; // 缩放比例
//            System.out.println("图片缩放"+filePath);
            BufferedImage bi = null;
            if (filePath.indexOf("http://") == 0) {
                bi = ImageIO.read(new URL(filePath));
            } else {
                bi = ImageIO.read(new File(filePath));
            }
            Image itemp = bi.getScaledInstance(width, height,
                    Image.SCALE_SMOOTH);
            // 计算比例
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = (new Integer(height)).doubleValue()
                            / bi.getHeight();
                } else {
                    ratio = (new Integer(width)).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(
                        AffineTransform.getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (bb) {
                BufferedImage image = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null)) {
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                } else {
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                }
                g.dispose();
                itemp = image;
            }
            return (BufferedImage) itemp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Exception{
        List<String> list = Lists.newArrayList();
        list.add("http://39.107.247.82:10004/storage/293*220*870049c878ccd9dc5f6b0fdfebbce494.jpeg");
        list.add("http://39.107.247.82:10004/storage/500*333*9fc5f9194a6952d2170b1fe3b33ea36d.jpeg");
        list.add("http://39.107.247.82:10004/storage/450*300*903b777507980a29bb0167bfe564fe9c.jpeg");
//        list.add("http://39.107.247.82:10004/storage/293*220*870049c878ccd9dc5f6b0fdfebbce494.jpeg");
//        list.add("http://39.107.247.82:10004/storage/500*333*9fc5f9194a6952d2170b1fe3b33ea36d.jpeg");
//        list.add("http://39.107.247.82:10004/storage/450*300*903b777507980a29bb0167bfe564fe9c.jpeg");
//        list.add("http://39.107.247.82:10004/storage/293*220*870049c878ccd9dc5f6b0fdfebbce494.jpeg");
//        list.add("http://39.107.247.82:10004/storage/500*333*9fc5f9194a6952d2170b1fe3b33ea36d.jpeg");
//        list.add("http://39.107.247.82:10004/storage/450*300*903b777507980a29bb0167bfe564fe9c.jpeg");
        File target = new File("/Users/lihao/Desktop/new.png");
        byte[] data = merge(list);
        FileUtils.writeByteArrayToFile(target, data);
    }


}
