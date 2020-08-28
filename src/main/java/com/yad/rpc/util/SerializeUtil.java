package com.yad.rpc.util;

import java.io.*;

public class SerializeUtil {
    static ByteArrayOutputStream baos = new ByteArrayOutputStream();
    public synchronized   static  byte[] serialize(Object o){
        baos.reset();
        byte[] bytes = null;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  bytes;
    }

    public  static  Object  unSerialize(byte[] bytes){
        ByteArrayInputStream bain = new ByteArrayInputStream(bytes);
        Object o = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(bain);
             o = ois.readObject();
            return  o;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return  o;
    }
}
