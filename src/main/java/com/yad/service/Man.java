package com.yad.service;

public class Man implements  Person {
    @Override
    public String say(String msg) {
        return  "service impl  result is @: "+msg;
    }
}
