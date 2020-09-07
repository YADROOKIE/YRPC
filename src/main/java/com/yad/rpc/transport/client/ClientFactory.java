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
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.net.*;
import java.util.Random;
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

    //传输
    public CompletableFuture transport(YBody body) {
        String type ="http";
        CompletableFuture res = new CompletableFuture();
        if (type.equals("rpc")){
            //序列化
            byte[] bodyBytes = SerializeUtil.serialize(body);
            YHeader header = YHeader.createHeader(bodyBytes.length);
            byte[] headerBytes = SerializeUtil.serialize(header);
//        System.out.println(headerBytes.length);

            //TODO 这里的连接地址是写死的 ，后续要加上服务发现和服务注册根据服务名来进行连接
            //获取连接
            NioSocketChannel channel = factory.getClient(new InetSocketAddress("localhost",9090));
            //注册 返回值
            ResponseMappingCallback.addCallback(header.getRequestId(),res);
            //写出数据
            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer(headerBytes.length+bodyBytes.length );
            byteBuf.writeBytes(headerBytes);
            byteBuf.writeBytes(bodyBytes);
            channel.writeAndFlush(byteBuf);
        }else {
            //1 使用 URL 发送请求 BIO
//            urlTransport(body,res);
            //2 netty创建客户端发送
//            nettyTransport(body,res);
            HttpNettyClient client = new HttpNettyClient();
            client.transport(body,res);
        }


        return  res;
    }

    private void nettyTransport(YBody body, CompletableFuture res) {
        try {
            //1 建立连接
            NioEventLoopGroup group = new NioEventLoopGroup(1);
            Bootstrap bootstrap = new Bootstrap();
            ChannelFuture connect = bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel channel) throws Exception {
                            ChannelPipeline ch = channel.pipeline();
                            ch.addLast(new HttpClientCodec())
                                    .addLast(new HttpObjectAggregator(1024*512))
                                    .addLast(new ChannelInboundHandlerAdapter(){
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            FullHttpResponse response  = (FullHttpResponse) msg;
                                            ByteBuf content = response.content();
                                            byte[] data = new byte[content.readableBytes()];
                                            content.readBytes(data);
                                            YBody o = (YBody) SerializeUtil.unSerialize(data);
                                            res.complete(o.getResult());
                                        }
                                    });
                        }
                    }).connect(new InetSocketAddress("localhost", 9090));
            Channel channel = connect.sync().channel();

            byte[] data = SerializeUtil.serialize(body);

            //注意！！！ Jetty是 HTTP  1.0 不是 1.1
            FullHttpRequest request = new
                    DefaultFullHttpRequest(HttpVersion.HTTP_1_0,HttpMethod.POST,"/",Unpooled.copiedBuffer(data));

            request.headers().set(HttpHeaderNames.CONTENT_LENGTH,data.length);

            channel.writeAndFlush(request);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void urlTransport(YBody body, CompletableFuture res) {
        try {
            URL url = new URL("http://localhost:9090");
            HttpURLConnection hc = (HttpURLConnection) url.openConnection();
            //post
            hc.setRequestMethod("POST");
            hc.setDoOutput(true);
            hc.setDoInput(true);

            OutputStream out = hc.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(body);

            int code = hc.getResponseCode();//在这里才发送

            Object o = null;
            if (code==200){
                InputStream in = hc.getInputStream();
                ObjectInputStream ins = new ObjectInputStream(in);
                YBody obj = (YBody) ins.readObject();
                o = obj.getResult();
            }

            res.complete(o);
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}
