package com.yad.rpc.proxy;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.protocol.YBody;
import com.yad.rpc.transport.client.ClientFactory;
import com.yad.rpc.util.SerializeUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

public class YProxy {
    public  static  <T> T proxyGet(Class<T> interfaceInfo){
        ClassLoader loader =  interfaceInfo.getClassLoader();
        Class<?>[] methodInfo = {interfaceInfo};
        Dispacher dispacher = Dispacher.getDispacher();
        // 生产代理对象
        return (T) Proxy.newProxyInstance(loader, methodInfo, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //
                Object o = dispacher.get(interfaceInfo.getName());
                Object res = null;
                //如果不是本地服务就进行远程调用
                if (o==null){
                    String name   = interfaceInfo.getName();
                    String methodName = method.getName();
                    Class<?>[] paramTypes = method.getParameterTypes();
                    //封装消息体
                    YBody body = new YBody();
                    body.setName(name);
                    body.setMethodName(methodName);
                    body.setParamsType(paramTypes);
                    body.setArgs(args);

                    CompletableFuture resF = ClientFactory.getFactory().transport(body);
                    res = resF.get();//这里会阻塞等待返回值
                }else {
                    System.out.println("本地调用");
                    Class<?> clazz = o.getClass();
                    Method m = clazz.getMethod(method.getName(), method.getParameterTypes());
                    res = m.invoke(o, args);
                }
                return res;
            }
        });
    }
}
