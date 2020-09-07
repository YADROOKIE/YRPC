package com.yad.rpc.transport.client;

import com.yad.rpc.protocol.YBody;
import com.yad.rpc.util.SerializeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
http 调用的有状态实现 ， 为每个请求分配ID 对应
 */
public class HttpNettyClient {
    static  NioEventLoopGroup group = new NioEventLoopGroup(1);
    static Map<Long, CompletableFuture>   IdToComplete = new ConcurrentHashMap<>();
    static AtomicInteger count = new AtomicInteger(0);

    Bootstrap bootstrap = new Bootstrap();
    Channel channel  = null  ;

    public   HttpNettyClient (){
        ChannelFuture connect = bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        ChannelPipeline p = channel.pipeline();
                        p.addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(1024*512))
                                .addLast(new ChannelInboundHandlerAdapter(){
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        FullHttpResponse response  = (FullHttpResponse) msg;
                                        ByteBuf content = response.content();
                                        byte[] data = new byte[content.readableBytes()];
                                        content.readBytes(data);
                                        YBody o = (YBody) SerializeUtil.unSerialize(data);
                                        CompletableFuture comple = IdToComplete.get(o.getStateId());
                                        System.out.println(o.getStateId());
                                        IdToComplete.remove(o.getStateId());
                                        comple.complete(o.getResult());
                                    }

                                });
                    }
                }).connect(new InetSocketAddress("localhost", 9090));
        try {
            ChannelFuture sync = connect.sync();
            this.channel = sync.channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public  void  transport(YBody body, CompletableFuture res){
        Channel channel = this.channel;
        body.setStateId(UUID.randomUUID().getLeastSignificantBits());
        this.IdToComplete.put(body.getStateId(),res);

        byte[] data = SerializeUtil.serialize(body);

        //注意！！！ Jetty是 HTTP  1.0 不是 1.1
        FullHttpRequest request = new
                DefaultFullHttpRequest(HttpVersion.HTTP_1_0,HttpMethod.POST,"/", Unpooled.copiedBuffer(data));

        request.headers().set(HttpHeaderNames.CONTENT_LENGTH,data.length);

        channel.writeAndFlush(request);

    }
}


