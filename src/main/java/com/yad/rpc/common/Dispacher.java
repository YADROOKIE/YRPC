package com.yad.rpc.common;

import java.util.concurrent.ConcurrentHashMap;

public class Dispacher {
    private  Dispacher(){}
    private  static  final  Dispacher dispacher = new Dispacher();
    public   static  Dispacher getDispacher(){ return  dispacher; }
    //类名 对应
    private ConcurrentHashMap<String,Object>  invokeMapping = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Object> getInvokeMapping() {
        return invokeMapping;
    }

    public void  register(String k , Object v ){ invokeMapping.put(k, v); }

    public Object get(String k ){ return invokeMapping.get(k); }

}
