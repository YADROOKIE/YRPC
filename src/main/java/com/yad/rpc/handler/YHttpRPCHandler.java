package com.yad.rpc.handler;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.protocol.YBody;
import com.yad.rpc.util.SerializeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.lang.reflect.Method;

public class YHttpRPCHandler  extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;

        ByteBuf buf = request.content();
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        YBody o = (YBody) SerializeUtil.unSerialize(data);

        Dispacher dispacher = Dispacher.getDispacher();
        Object invoker = dispacher.get(o.getName());

        Method method = invoker.getClass().getMethod(o.getMethodName(), o.getParamsType());
        Object res = method.invoke(invoker, o.getArgs());
        System.out.println(res);

        YBody body = new YBody();
        body.setResult(res);

        byte[] bytes = SerializeUtil.serialize(body);

        FullHttpResponse response =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(bytes));

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH,bytes.length);

        ctx.writeAndFlush(response);
    }
}
