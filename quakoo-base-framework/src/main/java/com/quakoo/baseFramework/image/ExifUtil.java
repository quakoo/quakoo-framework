package com.quakoo.baseFramework.image;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.google.common.io.Files;

public class ExifUtil {
	
	public static Exif exif(byte[] data){
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(data);
			Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(bais), true);
			Directory dir = metadata.getDirectory(ExifIFD0Directory.class);
			Exif exif = new Exif();
			
			if(null!=dir){
				if(dir.containsTag(ExifIFD0Directory.TAG_MODEL)) exif.setModel(dir.getString(ExifIFD0Directory.TAG_MODEL));
				if(dir.containsTag(ExifIFD0Directory.TAG_MAKE)) exif.setMake(dir.getString(ExifIFD0Directory.TAG_MAKE));
				if(dir.containsTag(ExifIFD0Directory.TAG_DATETIME)) exif.setDateTime(dir.getString(ExifIFD0Directory.TAG_DATETIME));
				if(dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) exif.setOrientation(dir.getInt(ExifIFD0Directory.TAG_ORIENTATION));
			}
		
		    dir = metadata.getDirectory(ExifSubIFDDirectory.class);
		    if(null!=dir){
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)) exif.setImageLength(dir.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)) exif.setImageWidth(dir.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_WHITE_BALANCE)) exif.setWhiteBalance(dir.getInt(ExifSubIFDDirectory.TAG_WHITE_BALANCE));
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) exif.setFocalLength(dir.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_FLASH)) exif.setFlash(dir.getInt(ExifSubIFDDirectory.TAG_FLASH));
		    	
				if (dir.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)){
					Object o = dir.getObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
			        if (o != null && o instanceof Rational && !Double.isInfinite(((Rational)o).doubleValue())){
			        	exif.setExposureTime(((Rational)o).toSimpleString(true));
			        }
				}
		    	
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) exif.setIsoSpeedRatings(dir.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
		    	if(dir.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) exif.setfNumber(dir.getString(ExifSubIFDDirectory.TAG_FNUMBER));
		    }
	        
		    dir = metadata.getDirectory(GpsDirectory.class);
		    if(null!=dir){
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_PROCESSING_METHOD)) exif.setGpsProcessingMethod(dir.getString(GpsDirectory.TAG_GPS_PROCESSING_METHOD));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_DATE_STAMP)) exif.setGpsDateStamp(dir.getString(GpsDirectory.TAG_GPS_DATE_STAMP));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_TIME_STAMP)) exif.setGpsTimeStamp(dir.getString(GpsDirectory.TAG_GPS_TIME_STAMP));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_LONGITUDE)) exif.setGpsLongitude(dir.getString(GpsDirectory.TAG_GPS_LONGITUDE));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_LONGITUDE_REF)) exif.setGpsLongitudeRef(dir.getString(GpsDirectory.TAG_GPS_LONGITUDE_REF));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_LATITUDE)) exif.setGpsLatitude(dir.getString(GpsDirectory.TAG_GPS_LATITUDE));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_LATITUDE_REF)) exif.setGpsLatitudeRef(dir.getString(GpsDirectory.TAG_GPS_LATITUDE_REF));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_ALTITUDE)) exif.setGpsAltitude(dir.getString(GpsDirectory.TAG_GPS_ALTITUDE));
		    	if(dir.containsTag(GpsDirectory.TAG_GPS_ALTITUDE_REF)) exif.setGpsAltitudeRef(dir.getInt(GpsDirectory.TAG_GPS_ALTITUDE_REF));
		    }
		    
		    return exif.checkNULL();
		} catch (Exception e) {
			return null;
		} finally{
			if(null!=bais) {
				try {
					bais.close();
				} 
				catch (IOException e) {}
			}
		}
	}
	public static byte[] thumbnail(byte[] data){
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(data);
			Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(bais), true);
			ExifThumbnailDirectory dir = metadata.getDirectory(ExifThumbnailDirectory.class);
			
			byte[] thumbnail = null;
		    if(null!=dir){
		    	thumbnail = dir.getThumbnailData();
		    }
		    
		    return thumbnail;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally{
			if(null!=bais) {
				try {
					bais.close();
				} 
				catch (IOException e) {}
			}
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		File file = new File("E://exif.jpg");

		byte[] b = Files.asByteSource(file).read();
		System.out.println(exif(b));
	}
}
