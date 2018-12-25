package com.quakoo.baseFramework.redis;

public class GeoNearString {
	private String member;
	private double distance;
	private double score;
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
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	@Override
	public String toString() {
		return "GeoNearString ["
				+ (member != null ? "member=" + member + ", " : "")
				+ "distance=" + distance + ", score=" + score + "]";
	}

}
