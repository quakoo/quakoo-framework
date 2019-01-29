package com.quakoo.framework.ext.chat.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.quakoo.baseFramework.model.pagination.PagerCursor;
import org.apache.commons.collections.ComparatorUtils;

import javax.jws.soap.SOAPBinding;


public class UserStream implements Serializable, Comparable<UserStream> {
	
	private static final long serialVersionUID = -1956412922309271443L;

	private long uid;
	
	private int type;
	
	private long thirdId;
	
	private long mid;
	
	private long authorId;
	
	@PagerCursor
	private double sort;

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

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

	public double getSort() {
		return sort;
	}

	public void setSort(double sort) {
		this.sort = sort;
	}

	public long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(long authorId) {
		this.authorId = authorId;
	}

	@Override
	public String toString() {
		return "UserStream [uid=" + uid + ", type=" + type + ", thirdId="
				+ thirdId + ", mid=" + mid + ", authorId=" + authorId
				+ ", sort=" + sort + "]";
	}

    @Override
    public int compareTo(UserStream other) {
	    if(other.getSort() > this.getSort()) return 1;
	    else if(other.getSort() < this.getSort()) return -1;
        else {
            if(other.getMid() > this.getMid()) return 1;
            else return -1;
        }
    }

    public static void main(String[] args) {
        UserStream a = new UserStream();
        a.setUid(1);
        a.setMid(10);
        a.setSort(2);

        UserStream b = new UserStream();
        b.setUid(2);
        b.setMid(1);
        b.setSort(2);

        List<UserStream> list = Lists.newArrayList(a, b);
        Collections.sort(list);
        System.out.println(list.toString());
    }
}
