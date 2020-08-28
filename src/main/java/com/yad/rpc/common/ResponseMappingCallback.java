package com.yad.rpc.common;

import com.yad.rpc.protocol.YPack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/*
用来保存每个requestId 对应的 CompletableFuture
首先注册
然后当获得远程调用用的结果时，进行返回
 */
public class ResponseMappingCallback {
    static ConcurrentHashMap<Long, CompletableFuture> mapping = new ConcurrentHashMap<>();
    public  static  void  addCallback(Long requestId , CompletableFuture future){
        mapping.put(requestId,future);
    }

    public  static  void  removeCb(Long requestId){
        mapping.remove(requestId);
    }



    public static void runCallback(YPack pack) {
        CompletableFuture completableFuture = mapping.get(pack.getHeader().getRequestId());
        completableFuture.complete(pack.getBody().getResult());
        removeCb(pack.getHeader().getRequestId());
    }
}
