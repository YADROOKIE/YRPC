package com.yad.rpc;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.handler.ServerResponseHandler;
import com.yad.rpc.handler.YDecoder;
import com.yad.rpc.handler.YHttpRPCHandler;
import com.yad.service.Car;
import com.yad.service.Man;
import com.yad.service.PandaCar;
import com.yad.service.Person;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetSocketAddress;

public class ServerStarter {
    public static void main(String[] args) throws InterruptedException {
        Person person = new Man();
        Car car = new PandaCar();
        Dispacher dispacher = Dispacher.getDispacher();
        dispacher.register(Person.class.getName(),person);
        dispacher.register(Car.class.getName(),car);

        NioEventLoopGroup boss = new NioEventLoopGroup(5);
        NioEventLoopGroup worker = boss;
        ServerBootstrap bootstrap = new ServerBootstrap();
        ChannelFuture bind = bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        System.out.println("server accept port " + channel.remoteAddress().getPort());
                        ChannelPipeline pipeline = channel.pipeline();
                        // 1. 自定义RPC
//                        pipeline.addLast(new YDecoder());
//                        pipeline.addLast(new ServerResponseHandler(dispacher));
                        // 2. http
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(1024*512));
                        pipeline.addLast(new YHttpRPCHandler());
                    }
                }).bind(new InetSocketAddress("localhost", 9090));
        bind.sync();
        Channel channel = bind.channel().closeFuture().channel();
    }
}
