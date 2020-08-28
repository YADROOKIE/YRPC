package com.yad.rpc.transport.client;

import com.yad.rpc.common.ResponseMappingCallback;
import com.yad.rpc.handler.ClientResponseHandler;
import com.yad.rpc.handler.YDecoder;
import com.yad.rpc.protocol.YBody;
import com.yad.rpc.protocol.YHeader;
import com.yad.rpc.util.SerializeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFactory {
    private ClientFactory (){}
    private  static final   ClientFactory factory = new ClientFactory();
    public   static  ClientFactory getFactory(){ return  factory; }

    private NioEventLoopGroup clientWorker = new NioEventLoopGroup(4);

    private  int poolSize = 1;

    private  Random random = new Random();
    //每一个服务的地址都有丽对应的连接池
    private ConcurrentHashMap<InetSocketAddress,ClientPool> outBoxes = new ConcurrentHashMap<>();


    //获取服务地址的连接 ， 对没有初始化的连接进行初始化
    public NioSocketChannel getClient(InetSocketAddress address){
        //1 获取连接
        ClientPool clientPool = outBoxes.get(address);
        // 如果没有初始化连接池 就进行初始化
        if (clientPool==null){
            synchronized (outBoxes){
                if (clientPool==null){
                    outBoxes.put(address,new ClientPool(poolSize));
                    clientPool = outBoxes.get(address);
                }
            }
        }
        //TODO 这里可以抽出接口更换负载均衡的方式
        //这个是随机的方式
        int i =  random.nextInt(poolSize);
        //连接已经初始化并且是活动的就是一个有效连接直接返回
        if (clientPool.channels[i]!=null && clientPool.channels[i].isActive()){
            return  clientPool.channels[i];
        }else {
            //没有初始化就进行初始化  双重检查 避免并发问题
            synchronized (clientPool.locks[i]){
                if (clientPool.channels[i]==null ||  !clientPool.channels[i].isActive()){
                    clientPool.channels[i]=createConnect(address);
                }
            }
        }
        return  clientPool.channels[i];
    }

    private NioSocketChannel createConnect(InetSocketAddress address) {
        Bootstrap bootstrap = new Bootstrap() ;
        ChannelFuture connect = bootstrap.group(clientWorker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new YDecoder());
                        pipeline.addLast(new ClientResponseHandler());
                    }
                })
                .connect(address);
        try {
            NioSocketChannel channel = (NioSocketChannel) connect.sync().channel();
            return  channel;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  null;
    }

    public CompletableFuture transport(YBody body) {
        //序列化
        byte[] bodyBytes = SerializeUtil.serialize(body);
        YHeader header = YHeader.createHeader(bodyBytes.length);
        byte[] headerBytes = SerializeUtil.serialize(header);
        System.out.println(headerBytes.length);

        //TODO 这里的连接地址是写死的 ，后续要加上服务发现和服务注册根据服务名来进行连接
        //获取连接
        NioSocketChannel channel = factory.getClient(new InetSocketAddress("localhost",9090));

        //注册 返回值
        CompletableFuture res = new CompletableFuture();
        ResponseMappingCallback.addCallback(header.getRequestId(),res);

        //写出数据
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(headerBytes.length+bodyBytes.length );
        byteBuf.writeBytes(headerBytes);
        byteBuf.writeBytes(bodyBytes);
        channel.writeAndFlush(byteBuf);

        return  res;
    }
}
