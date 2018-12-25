package com.quakoo.space.helper;

public class BaseJdbcHelper {

	private String columns; // 数据库的列

	private String insert_columns;

	private String insert_bean_columns;// 对应数据库列的类的属性

	private String update_bean_columns; // 更新数据库列

	private String where_columns_identity; // 根据自增类型的属性

	private String where_columns_combination; // 根据组合类型的属性

	public BaseJdbcHelper() {
		super();
	}

	public BaseJdbcHelper(String columns, String insert_columns,
			String insert_bean_columns, String update_bean_columns) {
		super();
		this.columns = columns;
		this.insert_columns = insert_columns;
		this.insert_bean_columns = insert_bean_columns;
		this.update_bean_columns = update_bean_columns;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getInsert_bean_columns() {
		return insert_bean_columns;
	}

	public void setInsert_bean_columns(String insert_bean_columns) {
		this.insert_bean_columns = insert_bean_columns;
	}

	public String getUpdate_bean_columns() {
		return update_bean_columns;
	}

	public void setUpdate_bean_columns(String update_bean_columns) {
		this.update_bean_columns = update_bean_columns;
	}

	public String getWhere_columns_identity() {
		return where_columns_identity;
	}

	public void setWhere_columns_identity(String where_columns_identity) {
		this.where_columns_identity = where_columns_identity;
	}

	public String getWhere_columns_combination() {
		return where_columns_combination;
	}

	public void setWhere_columns_combination(String where_columns_combination) {
		this.where_columns_combination = where_columns_combination;
	}

	@Override
	public String toString() {
		return "BaseJdbcHelper [columns=" + columns + ", insert_columns="
				+ insert_columns + ", insert_bean_columns="
				+ insert_bean_columns + ", update_bean_columns="
				+ update_bean_columns + ", where_columns_identity="
				+ where_columns_identity + ", where_columns_combination="
				+ where_columns_combination + "]";
	}

	public String getInsert_columns() {
		return insert_columns;
	}

	public void setInsert_columns(String insert_columns) {
		this.insert_columns = insert_columns;
	}

}
