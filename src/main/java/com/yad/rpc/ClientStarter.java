package com.yad.rpc;

import com.yad.rpc.proxy.YProxy;
import com.yad.service.Person;

import java.util.concurrent.atomic.AtomicInteger;

public class ClientStarter {
    public static void main(String[] args) {
        int size = 20;
        AtomicInteger num = new AtomicInteger(0);
        Thread[] threads = new Thread[size];
        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(()->{
                Person p = YProxy.proxyGet(Person.class);
                String s = "hello" +num.incrementAndGet();
                String say = p.say(s);
                System.out.println("client over result is @ "+say);
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
    }
}
