package com.quakoo.space.interfaces;

import java.io.Serializable;
import java.util.List;

import com.quakoo.space.model.lbs.Coordinate;
import org.springframework.data.mongodb.core.query.Query;


public interface HLbsService<T> extends Serializable {
	
	public T load(String id) throws Exception;

	public T insert(T model) throws Exception;

	public boolean update(T model) throws Exception;

	public boolean delete(String id) throws Exception;
	
	public List<T> near(Coordinate coordinate, double radius, int num, Query query);
	
	public List<T> box(Coordinate leftLowCoordinate, Coordinate rightUpperCoordinate, Query query);
	
}
