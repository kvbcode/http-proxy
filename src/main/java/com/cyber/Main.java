package com.cyber;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = Integer.valueOf(System.getenv().getOrDefault("PORT", "5000"));

        System.out.println("Starting... at port: " + port);

        HttpProxyServer httpProxyServer = new HttpProxyServer(port);
        httpProxyServer.start();

    }


}
