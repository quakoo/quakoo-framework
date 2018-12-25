package com.quakoo.space.timeline;

import java.io.Serializable;
import java.util.List;

import com.quakoo.baseFramework.json.JsonUtils;

public class RepetObjectContainer implements Serializable{
	 /**
	 *
	 */
	private static final long serialVersionUID = -1704922250002436169L;

	private  List<Object> objets;
	public List<Object> getObjets() {
		return objets;
	}
	public void setObjets(List<Object> objets) {
		this.objets = objets;
	}



	public RepetObjectContainer(List<Object> objets) {
		super();
		this.objets = objets;
	}
	@Override
	public String toString() {
		try {
			return JsonUtils.format(this);
		}catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("",e);
		}
	}






}
