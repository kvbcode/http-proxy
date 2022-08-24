package com.cyber;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("Starting...");

        HttpProxyServer httpProxyServer = new HttpProxyServer(5000);
        httpProxyServer.start();

    }


}
