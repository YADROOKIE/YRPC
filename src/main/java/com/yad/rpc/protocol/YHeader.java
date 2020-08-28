package com.yad.rpc.protocol;

import java.io.Serializable;
import java.util.UUID;

public class YHeader  implements Serializable {
    private  int flag ;
    private  long requestId;
    private  long DataLen;

    public  static  YHeader createHeaderRes(long dataLen,long requestId){
        YHeader header = new YHeader();
        int f = 0x14141412;
        header.setFlag(f);
        header.setRequestId(requestId);
        header.setDataLen(dataLen);
        return  header;
    }

    public  static  YHeader createHeader(long dataLen){
        YHeader header = new YHeader();
        int f = 0x14141414;
        header.setFlag(f);
        long requestId = UUID.randomUUID().getLeastSignificantBits();
        header.setRequestId(requestId);
        header.setDataLen(dataLen);
        return  header;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getDataLen() {
        return DataLen;
    }

    public void setDataLen(long dataLen) {
        DataLen = dataLen;
    }
}
