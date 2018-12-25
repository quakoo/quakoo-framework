package com.quakoo.space.model.lbs;

import java.io.Serializable;

//坐标 经度（正：东经　负：西经） 纬度（正：北纬　负：南纬）
public class Coordinate implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private double longitude; //经度 0-180
	
	private double latitude; //维度 0-90
	
	public double getLongitude() { 
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	@Override
	public String toString() {
		return "Coordinate [longitude=" + longitude + ", latitude=" + latitude
				+ "]";
	}
    
    

}
