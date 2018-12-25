package com.quakoo.baseFramework.image;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageMetadataUtil {
	
	private static final int jpg_magic_number = 0xFFD8;
	private static final int gif_magic_number = 0x4749;
	private static final int bmp_magic_number = 0x424D;
	private static final int png_magic_number = 0x8950;
	
	public static Exif encode(byte[] data){
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		try {
			int magicNumber = bais.read() << 8 | bais.read();
			if(magicNumber == jpg_magic_number){
				return JpgMetadataEncode.encode(bais);
			}else if(magicNumber == gif_magic_number){
				return GifMetadataEncode.encode(bais);
			}else if (magicNumber == bmp_magic_number){
				return BmpMetadataEncode.encode(bais);
			}else if (magicNumber == png_magic_number){
				return PngMetadataEncode.encode(bais);
			}else{
				return null;
			}
		} finally{
			try {
				bais.close();
			} catch (IOException e) {
			}
		}
	}
	
	static class PngMetadataEncode{
		
		static Exif encode(InputStream in){
			try {
				in.skip(14);
				int w =in.read() << 32| in.read() << 16 |in.read() << 8 | in.read();//FIXME why move 32
				int h =in.read() << 32| in.read() << 16 |in.read() << 8 | in.read();
				return new Exif(h, w);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
	}
	
	static class BmpMetadataEncode{
		
		static Exif encode(InputStream in){
			try {
				in.skip(16);
				int w = in.read() | in.read() << 8 | in.read() << 16 | in.read() << 32;
				int h = in.read() | in.read() << 8 | in.read() << 16 | in.read() << 32;
				return new Exif(h, w);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
	}
	
	static class GifMetadataEncode{
		
		static Exif encode(InputStream in){
			try {
				in.skip(4);
				int w = in.read() | in.read() << 8;
				int h = in.read() | in.read() << 8;
				return new Exif(h, w);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
	}
	
	static class JpgMetadataEncode{
	    private static final int tag_start = 0xff;
	    private static final int end_of_image = 0xd9;
	    private static final int start_of_frame = 0xc0;
	    private static final int restart_modulo_start = 0xd0;
	    private static final int restart_modulo_end = 0xd7;
	    private static final int start_of_scan = 0xda;
		
	    static Exif encode(InputStream in){
			try {
				while (true) {
					 int start_sign = in.read();
					 if (start_sign != tag_start) return null;
			         int sign = in.read();
			         if (sign >= restart_modulo_start && sign <= restart_modulo_end) continue;
			         if (sign == start_of_scan) return null;
			         if (sign == end_of_image) return null;
			         int dataLen = in.read() << 8 | in.read();
			         if (sign == start_of_frame) break;
			         in.skip(dataLen - 2);
			    }
			    in.skip(1);
			    int h = in.read() << 8 | in.read();
			    int w = in.read() << 8 | in.read();
			    return new Exif(h, w);
			} catch (Exception e) {
				e.printStackTrace();
                return null;
			}
		}
	}
  
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//JPG GIF BMP PNG
		InputStream inputStream = new FileInputStream("/Users/lihao/Desktop/4.png");
		byte[] data = new byte[20000];
		inputStream.read(data);
        System.out.println(ImageMetadataUtil.encode(data));
	}

}
