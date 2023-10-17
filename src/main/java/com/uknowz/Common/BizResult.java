package com.uknowz.Common;

import java.io.Serializable;

/**
 * @Author: tengyun
 * @Date:Create in  2018/8/20 上午11:38
 * @description:
 */
public class BizResult<T> implements Serializable {
    private static final long serialVersionUID = -1L;
    private boolean success = false;
    private Integer code=200;
    private String msg;
    private Integer count;
    private T data;

    public static <T> BizResult<T> create() {
        return new BizResult();
    }

    public static <T> BizResult<T> create(T data) {
        BizResult<T> bizResult = create();
        bizResult.setSuccess(true);
        bizResult.setData(data);
        return bizResult;
    }

    public static <T> BizResult<T> createByCt(T data,int count) {
        BizResult<T> bizResult = create();
        bizResult.setSuccess(true);
        bizResult.setCount(count);
        bizResult.setData(data);
        return bizResult;
    }

    public static <T> BizResult<T> create(T data, Integer code, String message) {
        BizResult<T> result = create();
        result.setSuccess(true);
        result.setData(data);
        result.setCode(code);
        result.setMsg(message);
        return result;
    }

    public static <T> BizResult<T> create(T data, boolean isSuccess, Integer code, String message) {
        BizResult<T> result = create();
        result.setSuccess(isSuccess);
        result.setData(data);
        result.setCode(code);
        result.setMsg(message);
        return result;
    }

    public static <T> BizResult<T> create(Integer code, String message) {
        BizResult<T> bizResult = create();
        if(code==200) {
            bizResult.setSuccess(true);
        }
        else{
            bizResult.setSuccess(false);
        }
        bizResult.setCode(code);
        bizResult.setMsg(message);
        return bizResult;
    }

    public static <T> BizResult<T> create(String message) {
        BizResult<T> bizResult = create();
        bizResult.setSuccess(true);
        bizResult.setCode(200);
        bizResult.setMsg(message);
        return bizResult;
    }

    public BizResult() {
    }

    public boolean isSuccess() {
        return this.success;
    }

    public Integer getCode() {
        return this.code;
    }

    public Integer getCount() {
        return this.count;
    }

    public String getMsg() {
        return this.msg;
    }

    public T getData() {
        return this.data;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void setData(T data) {
        this.data = data;
    }




    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BizResult)) {
            return false;
        } else {
            BizResult<?> other = (BizResult)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.isSuccess() != other.isSuccess()) {
                return false;
            } else {
                label49: {
                    Object this$code = this.getCode();
                    Object other$code = other.getCode();
                    if (this$code == null) {
                        if (other$code == null) {
                            break label49;
                        }
                    } else if (this$code.equals(other$code)) {
                        break label49;
                    }

                    return false;
                }

                Object this$msg = this.getMsg();
                Object other$msg = other.getMsg();
                if (this$msg == null) {
                    if (other$msg != null) {
                        return false;
                    }
                } else if (!this$msg.equals(other$msg)) {
                    return false;
                }

                Object this$data = this.getData();
                Object other$data = other.getData();
                if (this$data == null) {
                    if (other$data != null) {
                        return false;
                    }
                } else if (!this$data.equals(other$data)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof BizResult;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + (this.isSuccess() ? 79 : 97);
        Object $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        Object $msg = this.getMsg();
        result = result * 59 + ($msg == null ? 43 : $msg.hashCode());
        Object $data = this.getData();
        result = result * 59 + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    public String toString() {
        return "BizResult(success=" + this.isSuccess() + ", code=" + this.getCode() + ", msg=" + this.getMsg() + ", data=" + this.getData() + ")";
    }
}