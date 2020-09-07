package com.yad.rpc;

import com.yad.rpc.proxy.YProxy;
import com.yad.service.Car;
import com.yad.service.Person;

import java.util.concurrent.atomic.AtomicInteger;

public class ClientStarter {
    public static void main(String[] args) {
        int size = 20;
        AtomicInteger num = new AtomicInteger(0);
        Thread[] threads = new Thread[size];
        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(()->{
                Car p = YProxy.proxyGet(Car.class);
                String s = "hello" +num.incrementAndGet();
                Person person = p.owner(s);
                System.out.println("client over result is @ "+person.say(s));
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        System.out.println(Thread.currentThread().getName());
    }
}
