package com.yad.rpc.handler;

import com.yad.rpc.common.ResponseMappingCallback;
import com.yad.rpc.protocol.YPack;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientResponseHandler  extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        YPack pack = (YPack) msg;
        //运行回调函数
        if (pack!=null){
            ResponseMappingCallback.runCallback(pack);
        }
    }
}
