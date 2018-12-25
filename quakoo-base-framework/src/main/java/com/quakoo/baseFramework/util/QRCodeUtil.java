package com.quakoo.baseFramework.util;

//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import javax.imageio.ImageIO;
//
//import com.dnw.framework.statistics.AD;
//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.EncodeHintType;
//import com.google.zxing.MultiFormatWriter;
//import com.google.zxing.WriterException;
//import com.google.zxing.common.BitMatrix;

public class QRCodeUtil {

    //    private static final int BLACK = 0xFF000000;
    //
    //    private static final int WHITE = 0xFFFFFFFF;
    //
    //    private static final String FORMAT_JPG = "jpg";
    //
    //    private static final String FORMAT_PNG = "png";
    //
    //    private QRCodeUtil() {
    //    }
    //
    //    public static void toImageForJPG(String url, int width, int height, String savePath) throws WriterException,
    //            IOException {
    //        toBufferedImage(url, width, height, FORMAT_JPG, savePath);
    //    }
    //
    //    public static void toImageForPNG(String url, int width, int height, String savePath) throws WriterException,
    //            IOException {
    //        toBufferedImage(url, width, height, FORMAT_PNG, savePath);
    //    }
    //
    //    public static void toBufferedImage(String url, int width, int height, String format, String savePath)
    //            throws WriterException, IOException {
    //        BufferedImage image = toBufferedImage(url, width, height);
    //        writeToFile(image, format, savePath);
    //    }
    //
    //    public static BufferedImage toBufferedImage(String url, int width, int height) throws WriterException {
    //        Map params = new HashMap();
    //        params.put(EncodeHintType.CHARACTER_SET, "utf-8");
    //        BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height, params);
    //        return toBufferedImage(bitMatrix);
    //    }
    //
    //    /**
    //     * 生成二维码
    //     * @param matrix
    //     * @return
    //     */
    //    public static BufferedImage toBufferedImage(BitMatrix matrix) {
    //        int width = matrix.getWidth();
    //        int height = matrix.getHeight();
    //
    //        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    //
    //        for (int x = 0; x < width; x++) {
    //            for (int y = 0; y < height; y++) {
    //                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
    //            }
    //        }
    //        return image;
    //    }
    //
    //    private static void writeToFile(BufferedImage image, String format, String savePath) throws IOException {
    //        File file = new File(savePath);
    //        if (!ImageIO.write(image, format, file)) {
    //            throw new IOException("Could not write an image of format " + format + " to " + file);
    //        }
    //    }
    //
    //    public static void main(String[] args) throws Exception {
    //        String path = "d://QRCode//";
    //
    //        int width = 150;
    //        int height = 150;
    //
    //        String format = "http://yehuo.tv/?ad=%s";
    //        //        String format = "http://wildfire.chinaren.com/?ad=%s";
    //
    //        String url = String.format(format, AD.share_1_sohuweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "SOHU_cash.jpg");
    //
    //        url = String.format(format, AD.share_2_sohuweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "SOHU_roll.jpg");
    //
    //        url = String.format(format, AD.share_1_sinaweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "SINA_cash.jpg");
    //
    //        url = String.format(format, AD.share_2_sinaweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "SINA_roll.jpg");
    //
    //        url = String.format(format, AD.share_1_tqqweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "QQ_cash.jpg");
    //
    //        url = String.format(format, AD.share_2_tqqweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "QQ_roll.jpg");
    //
    //        url = String.format(format, AD.share_1_renrenweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "RENREN_cash.jpg");
    //
    //        url = String.format(format, AD.share_2_renrenweibo2.getValue());
    //        QRCodeUtil.toImageForJPG(url, width, height, path + "RENREN_roll.jpg");
    //
    //    }

}
