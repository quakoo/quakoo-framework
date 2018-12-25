package com.quakoo.baseFramework.redis;

import redis.clients.jedis.GeoCoordinate;

public class GeoRadiusString {
	private String member;
	private double distance;
	private GeoCoordinate coordinate;
	public String getMember() {
		return member;
	}
	public void setMember(String member) {
		this.member = member;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public GeoCoordinate getCoordinate() {
		return coordinate;
	}
	public void setCoordinate(GeoCoordinate coordinate) {
		this.coordinate = coordinate;
	}
	@Override
	public String toString() {
		return "GeoRadiusString ["
				+ (member != null ? "member=" + member + ", " : "")
				+ "distance=" + distance + ", "
				+ (coordinate != null ? "coordinate=" + coordinate : "") + "]";
	}
	
}
