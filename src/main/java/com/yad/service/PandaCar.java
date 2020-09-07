package com.yad.service;

public class PandaCar  implements  Car{
    @Override
    public Person owner(String msg) {
        Person  p = new Man();
        return  p;
    }
}
