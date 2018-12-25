package com.quakoo.baseFramework.image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.simpleimage.ImageFormat;
import com.alibaba.simpleimage.ImageRender;
import com.alibaba.simpleimage.SimpleImageException;
import com.alibaba.simpleimage.render.ReadRender;
import com.alibaba.simpleimage.render.ScaleParameter;
import com.alibaba.simpleimage.render.ScaleRender;
import com.alibaba.simpleimage.render.WriteParameter;
import com.alibaba.simpleimage.render.WriteRender;
import com.alibaba.simpleimage.render.ScaleParameter.Algorithm;

public class ImageUtils {
	
	public static final int type_jpg = 1;
	public static final int type_png = 2;

	static Logger logger = LoggerFactory.getLogger(ImageUtils.class);

	public static byte[] scaleImage(int width, int height, float quality, int type, byte[] src) {
		if (width <= 0) {
			width = Integer.MAX_VALUE;
		}
		if (height <= 0) {
			height = Integer.MAX_VALUE;
		}
		ScaleParameter scaleParam = new ScaleParameter(width, height); // 将图像缩略到1024x1024以内，不足1024x1024则不做任何处理
		scaleParam.setAlgorithm(Algorithm.LANCZOS);
		WriteParameter writeParam = new WriteParameter(); // 输出参数，默认输出格式为JPEG
		if (quality > 0 && quality < 1) {
			if (quality > 0.8f) {
				quality = 0.8f;
			}
			writeParam.setDefaultQuality(quality);
		}
		ByteArrayInputStream inStream = null;
		ByteArrayOutputStream outStream = null;
		ImageRender wr = null;
		try {
			inStream = new ByteArrayInputStream(src);
			outStream = new ByteArrayOutputStream();
			ImageRender rr = new ReadRender(inStream);

			ImageRender sr = new ScaleRender(rr, scaleParam);
			if(type == type_jpg) {
				wr = new WriteRender(sr, outStream, ImageFormat.JPEG, writeParam);
			} else if (type == type_png) {
				wr = new WriteRender(sr, outStream, ImageFormat.PNG, writeParam);
			} else 
				wr = new WriteRender(sr, outStream, ImageFormat.JPEG, writeParam);
			wr.render(); // 触发图像处理
			return outStream.toByteArray();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(inStream); // 图片文件输入输出流必须记得关闭
			IOUtils.closeQuietly(outStream);
			if (wr != null) {
				try {
					wr.dispose(); // 释放simpleImage的内部资源
				} catch (SimpleImageException ignore) {
					// skip ...
				}
			}
		}
		return null;
	}

	public static void main(String[] fewf) throws Exception {
		byte[] bytes = scaleImage(Integer.MAX_VALUE, Integer.MAX_VALUE, 0.50f,type_jpg,
				org.apache.commons.io.FileUtils.readFileToByteArray(new File(
						"/opt/1.png")));
		File file = new File("/opt/1.small.jpg");
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write(bytes);
		fileOutputStream.close();

		bytes = scaleImage(92, Integer.MAX_VALUE, 0f, type_jpg, 
				org.apache.commons.io.FileUtils.readFileToByteArray(new File(
						"/opt/1.png")));
		file = new File("/opt/1.small.1.jpg");
		fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write(bytes);
		fileOutputStream.close();
	}
}
