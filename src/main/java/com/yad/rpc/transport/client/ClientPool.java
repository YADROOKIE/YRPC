package com.yad.rpc.transport.client;

import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientPool {
    NioSocketChannel []  channels;
    Object [] locks ; //伴生锁
    public  ClientPool(int size){
        channels = new NioSocketChannel[size];
        locks = new Object[size];
        for (int i = 0; i < size; i++) {
            locks[i] = new Object();
        }
    }

}
