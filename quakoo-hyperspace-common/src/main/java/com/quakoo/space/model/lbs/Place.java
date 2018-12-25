package com.quakoo.space.model.lbs;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import com.quakoo.baseFramework.model.pagination.PagerCursor;

public class Place {
	
	@Id
	protected String id;
	
	protected String address = ""; //默认为空
	
	protected Coordinate coordinate;
	
	@Transient
	@PagerCursor
	protected double distance;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return "Place [" + (id != null ? "id=" + id + ", " : "")
				+ (address != null ? "address=" + address + ", " : "")
				+ (coordinate != null ? "coordinate=" + coordinate + ", " : "")
				+ "distance=" + distance + "]";
	}

	

	

}
