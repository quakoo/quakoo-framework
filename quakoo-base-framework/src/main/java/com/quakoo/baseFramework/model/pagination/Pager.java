package com.quakoo.baseFramework.model.pagination;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pager implements Serializable {

	/** */
	private static final long serialVersionUID = -3957020823257107332L;

	// cursor mode
	/** 读取游标 */
	private String cursor = "0";
	/** 返回结果的数量 */
	private int size = 20;

	/** 页号 */
	private int page = 1;
	// 结果
	/** 返回结果集 */
	private List data;

	/** 上一页的游标 */
	private String preCursor = "0";
	/** 下一页的游标 */
	private String nextCursor = "0";
	/** 当前取得的结果集数量 */
	private int count = 0;
	/** 总数 */
	private long totalCount = 0;
	/** 是否还有下一页 */
	private boolean hasnext;
	/** 总页数 */
	private long totalPage;
	
	public Pager(){}
	public Pager(int size){
		this.size=size;
	}
	
	public List getData() {
		return data;
	}

	public void setData(List data) {
		this.data = data;
	}

	

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public boolean isHasnext() {
		return hasnext;
	}

	public void setHasnext(boolean hasnext) {
		this.hasnext = hasnext;
	}
	
	

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}

	public String getPreCursor() {
		return preCursor;
	}

	public void setPreCursor(String preCursor) {
		this.preCursor = preCursor;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}
	
	

	public long getTotalPage() {
		if(getSize()==0){return 0;}
		int pCount = (int) (getTotalCount() / getSize());
		int pageCount=0;
		if(getTotalCount() % getSize() == 0){
			pageCount = pCount;
		}else {
			pageCount = pCount +1;
		}
		return pageCount;
	}
	public void setTotalPage(long totalPage) {
		this.totalPage = totalPage;
	}
	/**
	 * 按照标准格式做输出
	 * 
	 * @return
	 */
	public Map<String, Object> toModelAttribute() {
		Map<String, Object> resulte = new HashMap<String, Object>();
		if (null != this.getData() && this.getData().size() > 0) {
			resulte.put("data", this.getData());
		} else {
            resulte.put("data", Lists.newArrayList());
        }
		int count = 0;
		count = this.getCount();
		resulte.put("count", count);
		resulte.put("preCursor", this.getPreCursor());
		resulte.put("nextCursor", this.getNextCursor());
		resulte.put("hasnext", this.isHasnext());
		if (this.getTotalCount() > 0) {
			resulte.put("totalCount", this.getTotalCount());
		} else {
			resulte.put("totalCount", 0);
		}
		return resulte;
	}
	
	@Override
	public String toString() {
		return "Pager [" + (cursor != null ? "cursor=" + cursor + ", " : "")
				+ "size=" + size + ", page=" + page + ", "
				+ (data != null ? "data=" + data + ", " : "")
				+ (preCursor != null ? "preCursor=" + preCursor + ", " : "")
				+ (nextCursor != null ? "nextCursor=" + nextCursor + ", " : "")
				+ "count=" + count + ", totalCount=" + totalCount
				+ ", hasnext=" + hasnext + "]";
	}

}
