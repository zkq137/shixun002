package com.talent.common.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果包装
 */
public class PageResult<T> implements Serializable {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    public PageResult() {
        this.records = Collections.emptyList();
    }

    public PageResult(long total, long pageNum, long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records == null ? Collections.emptyList() : records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records == null ? Collections.emptyList() : records;
    }
}
