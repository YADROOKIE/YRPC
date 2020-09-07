package com.yad.rpc.util;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.protocol.YBody;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

public class  MyHttpRPCHandler extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("revice request ");
        ServletInputStream in = req.getInputStream();
        ObjectInputStream oin = new ObjectInputStream(in);

        try {
            YBody o = (YBody) oin.readObject();
            Dispacher dispacher = Dispacher.getDispacher();
            Object   invoker = dispacher.get(o.getName());
            // 判空 ！！ invoker

            Method method = invoker.getClass().getMethod(o.getMethodName(), o.getParamsType());
            Object res = method.invoke(invoker, o.getArgs());

            YBody body = new YBody();
            body.setResult(res);
            body.setStateId(o.getStateId());

            ServletOutputStream outputStream = resp.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(body);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
