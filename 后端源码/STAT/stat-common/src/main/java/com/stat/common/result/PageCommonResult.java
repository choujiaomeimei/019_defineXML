package com.stat.common.result;

import com.stat.common.base.RdfaObject;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * @description:
 * @author: yujq
 **/
public class PageCommonResult<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 3411472428234824701L;
    private boolean success;
    private String code;
    private String message;
    private int pageNum;
    private int pageSize;
    private long totalPages;
    private long totalCount;
    private List<T> data;

    public PageCommonResult() {
        this.success = true;
        this.pageNum = 0;
        this.pageSize = 0;
        this.totalPages = 0L;
        this.totalCount = 0L;
    }

    public PageCommonResult(boolean success, String code, String message) {
        this(success, code, message, (List) null);
    }

    public PageCommonResult(boolean success, String code, String message, List<T> data) {
        this.success = true;
        this.pageNum = 0;
        this.pageSize = 0;
        this.totalPages = 0L;
        this.totalCount = 0L;
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T extends Serializable> PageCommonResult<T> success(List<T> data) {
        PageCommonResult rest = new PageCommonResult();
        rest.setCode("0");
        rest.setMessage("");
        rest.setData(data);
        rest.setSuccess(true);
        return rest;
    }

    public static <T extends Serializable> PageCommonResult<T> success(int pageNum, int pageSize, long totalCount, List<T> data) {
        PageCommonResult rest = new PageCommonResult();
        rest.setCode("0");
        rest.setMessage("");
        rest.setData(data);
        rest.setPageSize(pageSize);
        rest.setTotalCount(totalCount);
        rest.setPageNum(pageNum);
        rest.setSuccess(true);
        return rest;
    }
    public static <T extends Serializable> PageCommonResult<T> success(long pageNum, long pageSize, long totalCount, List<T> data) {
        PageCommonResult rest = new PageCommonResult();
        rest.setCode("0");
        rest.setMessage("");
        rest.setData(data);
        rest.setPageSize(Integer.parseInt(pageSize+""));
        rest.setTotalCount(totalCount);
        rest.setPageNum(Integer.parseInt(pageNum+""));
        rest.setSuccess(true);
        return rest;
    }

    public static <T extends Serializable> PageCommonResult<T> fail(String code, String message) {
        PageCommonResult<T> rest = new PageCommonResult();
        rest.setCode(code);
        rest.setMessage(message);
        rest.setSuccess(false);
        return rest;
    }

    public int getPageNum() {
        return this.pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
        if (pageNum < 1) {
            this.pageNum = 1;
        } else if ((long) pageNum > this.getTotalPages()) {
            this.pageNum = (int) this.getTotalPages();
        }

    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalPages() {
        return this.totalPages;
    }

    private void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalCount() {
        return this.totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
        long result = totalCount / (long) this.getPageSize();
        if (totalCount % (long) this.pageSize != 0L) {
            ++result;
        }

        this.setTotalPages(result);
    }

    public List<T> getData() {
        return this.data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
    
    // 兼容方法，用于现有代码
    public void setList(List<T> list) {
        this.data = list;
    }
    
    public List<T> getList() {
        return this.data;
    }
    
    public void setTotal(Integer total) {
        this.setTotalCount(total.longValue());
    }
    
    public Integer getTotal() {
        return (int) this.totalCount;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toLog() {
        int length = 20;
        String data_str = null;
        if (this.code != null) {
            length += this.code.length();
        }

        if (this.message != null) {
            length += this.message.length();
        }

        if (this.data != null) {
            Iterator var3 = this.data.iterator();

            while (var3.hasNext()) {
                T o = (T) var3.next();
                if (o != null) {
                    if (o instanceof RdfaObject) {
                        data_str = ((RdfaObject) o).toLog();
                    } else {
                        data_str = o.toString();
                    }

                    length += data_str.length();
                }
            }
        }

        StringBuilder sb = new StringBuilder(length);
        sb.append("code:");
        sb.append(this.code);
        sb.append(" message:");
        sb.append(this.message);
        sb.append(" data:");
        sb.append(data_str);
        return sb.toString();
    }
}
