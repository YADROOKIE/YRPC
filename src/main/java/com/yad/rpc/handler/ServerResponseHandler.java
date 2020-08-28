package com.yad.rpc.handler;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.protocol.YBody;
import com.yad.rpc.protocol.YHeader;
import com.yad.rpc.protocol.YPack;
import com.yad.rpc.util.SerializeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ServerResponseHandler extends ChannelInboundHandlerAdapter {
    Dispacher dispacher;
    public  ServerResponseHandler(Dispacher dispacher){this.dispacher=dispacher;}
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        YPack pack  = (YPack) msg;
        ctx.executor().parent().next().execute(()->{
            String serviceName = pack.getBody().getName();
            String methodName = pack.getBody().getMethodName();
            System.out.println(pack.getBody().getArgs()[0]);
            //TODO  未对server没有对应服务时的错误处理
            Object o = dispacher.get(serviceName);
            Object res = null;
            try {
                Method method = o.getClass().getMethod(methodName, pack.getBody().getParamsType());
                res = method.invoke(o, pack.getBody().getArgs());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            YBody body = new YBody();
            body.setResult(res);
            byte[] bodyBytes = SerializeUtil.serialize(body);

            YHeader  header = YHeader.createHeaderRes(bodyBytes.length,pack.getHeader().getRequestId());
            byte[] headerBytes = SerializeUtil.serialize(header);

            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(headerBytes.length+bodyBytes.length );
            buf.writeBytes(headerBytes);
            buf.writeBytes(bodyBytes);
            ctx.channel().writeAndFlush(buf);
        });
    }
}
