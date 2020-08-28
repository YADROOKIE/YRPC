package com.yad.rpc;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.handler.ServerResponseHandler;
import com.yad.rpc.handler.YDecoder;
import com.yad.service.Man;
import com.yad.service.Person;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class ServerStarter {
    public static void main(String[] args) throws InterruptedException {
        Person person = new Man();
        Dispacher dispacher = Dispacher.getDispacher();
        dispacher.register(Person.class.getName(),person);

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
                        pipeline.addLast(new YDecoder());
                        pipeline.addLast(new ServerResponseHandler(dispacher));
                    }
                }).bind(new InetSocketAddress("localhost", 9090));
        bind.sync();
        Channel channel = bind.channel().closeFuture().channel();
    }
}
