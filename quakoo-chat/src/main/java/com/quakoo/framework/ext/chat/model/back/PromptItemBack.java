package com.quakoo.framework.ext.chat.model.back;

public class PromptItemBack {
	
    private long id;
	
	private int num;
	
	public PromptItemBack() {
		super();
	}

	public PromptItemBack(long id, int num) {
		super();
		this.id = id;
		this.num = num;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	@Override
	public String toString() {
		return "PromptItemBack [id=" + id + ", num=" + num + "]";
	}     
	
	

}
