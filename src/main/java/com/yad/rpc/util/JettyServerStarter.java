package com.yad.rpc.util;

import com.yad.rpc.common.Dispacher;
import com.yad.rpc.protocol.YBody;
import com.yad.service.Car;
import com.yad.service.Man;
import com.yad.service.PandaCar;
import com.yad.service.Person;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.net.InetSocketAddress;

public class JettyServerStarter {
    public static void main(String[] args) throws Exception {
        Person person = new Man();
        Car car = new PandaCar();
        Dispacher dispacher = Dispacher.getDispacher();
        dispacher.register(Person.class.getName(),person);
        dispacher.register(Car.class.getName(),car);

        Server server = new Server(new InetSocketAddress("localhost",9090));

        ServletContextHandler handler = new ServletContextHandler(server,"/");

        server.setHandler(handler);

        handler.addServlet(MyHttpRPCHandler.class,"/*");

        server.start();

        server.join();
    }
}


