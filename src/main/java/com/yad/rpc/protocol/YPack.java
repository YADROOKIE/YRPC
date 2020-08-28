package com.yad.rpc.protocol;

public class YPack {
    private  YHeader header;
    private  YBody   body;
    public  YPack(YHeader header ,YBody body){
        this.header = header;
        this.body  = body;
    }
    public YHeader getHeader() {
        return header;
    }

    public void setHeader(YHeader header) {
        this.header = header;
    }

    public YBody getBody() {
        return body;
    }

    public void setBody(YBody body) {
        this.body = body;
    }
}
