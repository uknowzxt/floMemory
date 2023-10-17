package com.uknowz.Common;

public class ExceptionHlz extends RuntimeException {

    protected int code;

    protected String desc;

    protected String message;

    public ExceptionHlz(ExcsEnum hlzExcs, Throwable cause) {
        super(hlzExcs.getCode() + " : " + hlzExcs.getMessage() + ", " + hlzExcs.getDesc(), cause);
        this.code = hlzExcs.getCode();
        this.desc = hlzExcs.getDesc();
        this.message = hlzExcs.getMessage();
    }

    public ExceptionHlz(ExcsEnum hlzExcs) {
        super(hlzExcs.getCode() + " : " + hlzExcs.getMessage() + ", " + hlzExcs.getDesc());
        this.code = hlzExcs.getCode();
        this.desc = hlzExcs.getDesc();
        this.message = hlzExcs.getMessage();
    }

    public ExceptionHlz(int code, String desc, String message) {
        super(code + " : " + desc + ", " + message);
        this.code = code;
        this.desc = desc;
        this.message = message;
    }

    public ExceptionHlz(String msg) {
        super(501 + " : " + msg + ", " + msg);
        this.code = 501;
        this.desc = msg;
        this.message = msg;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
