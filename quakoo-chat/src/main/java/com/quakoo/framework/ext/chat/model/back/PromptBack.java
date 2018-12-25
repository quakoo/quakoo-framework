package com.quakoo.framework.ext.chat.model.back;

import java.util.List;


public class PromptBack {
	
    private int type;
	
	private List<PromptItemBack> items;
	
	public PromptBack() {
		super();
	}

	public PromptBack(int type, List<PromptItemBack> items) {
		super();
		this.type = type;
		this.items = items;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public List<PromptItemBack> getItems() {
		return items;
	}

	public void setItems(List<PromptItemBack> items) {
		this.items = items;
	}

	@Override
	public String toString() {
		return "PromptBack [type=" + type + ", "
				+ (items != null ? "items=" + items : "") + "]";
	}



}
