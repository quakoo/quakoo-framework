package com.quakoo.baseFramework.model.pagination;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * jquery FlexiGrid列表PAGE对象
 * 
 * @Filename: FlexGridPager.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public class FlexGridPager<T> implements Serializable {

    private static final long serialVersionUID = 7278434009439871618L;

    private int page;

    private int rp;

    private int from;

    private int to;

    private List<FlexGridCell<T>> rows;

    private int total;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getRp() {
        return rp;
    }

    public void setRp(int rp) {
        this.rp = rp;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public List<FlexGridCell<T>> getRows() {
        return rows;
    }

    public void setRows(List<FlexGridCell<T>> rows) {
        this.rows = rows;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    private static class FlexGridCell<T> {
        private long id;

        private T cell;

        public FlexGridCell(long id, T object) {
            this.id = id;
            this.cell = object;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public T getCell() {
            return cell;
        }

        public void setCell(T cell) {
            this.cell = cell;
        }

    }

    public interface IDomain<T> {
        public long getPrimaryKey(T model);
    }

    public static <T> FlexGridPager<T> doPager(int page, int pageSize, int total, List<T> datas, IDomain<T> domain) {
        FlexGridPager<T> pager = new FlexGridPager<T>();

        pager.setPage(page);
        pager.setRp(pageSize);
        pager.setFrom((page - 1) * pageSize);
        pager.setTo(page * pageSize);
        pager.setTotal(total);

        List<FlexGridCell<T>> rows = new ArrayList<FlexGridPager.FlexGridCell<T>>();
        if (datas != null && datas.size() > 0) {
            for (T data : datas) {
                long id = domain.getPrimaryKey(data);
                FlexGridCell<T> cell = new FlexGridPager.FlexGridCell<T>(id, data);
                rows.add(cell);
            }
        }

        pager.setRows(rows);

        return pager;
    }

    @Override
    public String toString() {
        return "FlexGridPager [page=" + page + ", rp=" + rp + ", from=" + from + ", to=" + to + ", rows=" + rows
                + ", total=" + total + "]";
    }

}
