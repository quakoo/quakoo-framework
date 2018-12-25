package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.Sets;

public class UserDirectory implements Serializable {

	private static final long serialVersionUID = -7763305576755772543L;

	private long uid;
	
	private int type;
	
	private long thirdId;
	
	private long ctime;

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getThirdId() {
		return thirdId;
	}

	public void setThirdId(long thirdId) {
		this.thirdId = thirdId;
	}


	public long getCtime() {
		return ctime;
	}

	public void setCtime(long ctime) {
		this.ctime = ctime;
	}


	@Override
	public int hashCode() {
		return (String.valueOf(this.uid) + String.valueOf(this.type)
				+ String.valueOf(this.thirdId)).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(this == other) return true;
	    if(other == null) return false;
	    if(!(other instanceof UserDirectory)) return false;
	    final UserDirectory directory = (UserDirectory) other;
	    if(this.getUid() != directory.getUid()) return false;
		if(this.getType() != directory.getType()) return false;
		if(this.getThirdId() != directory.getThirdId()) return false;
	    return true;
	}
	
	public static void main(String[] args) {
		Set<UserDirectory> set = Sets.newHashSet();
		
		UserDirectory d1 = new UserDirectory();
		d1.setUid(1);
		d1.setType(1);
		d1.setThirdId(2);
		set.add(d1);
		UserDirectory d2 = new UserDirectory();
		d2.setUid(1);
		d2.setType(1);
		d2.setThirdId(3);
		set.add(d2);
		System.out.println(set.toString());
	}

	@Override
	public String toString() {
		return "UserDirectory [uid=" + uid + ", type=" + type + ", thirdId="
				+ thirdId + ", ctime=" + ctime + "]";
	}

}
