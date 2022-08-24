package com.cyber;

import com.cyber.http.HttpRequest;
import com.cyber.http.HttpRequestParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class HttpProxyServer {
    final static int BACKLOG_LENGTH = 50;
    final static int SO_TIMEOUT = 60;

    final InetAddress addr;
    final int port;
    final ServerSocket socket;
    final AtomicBoolean doWork;

    final HttpRequestParser httpRequestParser;

    public HttpProxyServer(InetAddress addr, int port) throws IOException {
        this.addr = addr;
        this.port = port;
        this.socket = new ServerSocket(port, BACKLOG_LENGTH, this.addr);
        this.socket.setSoTimeout(SO_TIMEOUT);
        this.doWork = new AtomicBoolean(false);
        this.httpRequestParser = new HttpRequestParser();
    }

    public HttpProxyServer(int port) throws IOException {
        this(null, port);
    }

    public void stop() {
        doWork.set(false);
    }

    public void start() throws IOException {
        doWork.set(true);

        while (doWork.get()) {
            try {
                Socket clientSocket = socket.accept();
                new Thread(() -> onClientConnection(clientSocket)).start();
            } catch (SocketTimeoutException ex) {
                continue;
            }
        }
    }

    protected void onClientConnection(Socket socket) {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()
        ) {
            HttpRequest httpRequest = readHttpRequest(in);
            System.out.println("NEW REQUEST:");
            System.out.println(httpRequest.toFullRequest());


            if (HttpRequest.METHOD_CONNECT.equals(httpRequest.getMethod())) {
                String response = "HTTP/1.1 404 Not Found\r\n\r\n";
                in.skip(in.available());
                //handleConnectMethod(httpRequest, in, out);
            } else {
                handlePlainMethod(httpRequest, in, out);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("close client socket: " + socket);
    }

    protected HttpRequest readHttpRequest(InputStream in) throws IOException {
        int available = in.available();
        byte[] buf = new byte[available];
        in.read(buf);
        String requestStr = new String(buf);
        return httpRequestParser.parse(requestStr);
    }

    protected void handleConnectMethod(HttpRequest httpRequest, InputStream in, OutputStream out) throws IOException {
        String[] hostParts = httpRequest.getHost().split(":");
        String host = hostParts[0];
        int port = Integer.valueOf(hostParts[1]);
        System.out.println("CONNECT to " + host + ", port: " + port);

        try (
                Socket remoteSocket = new Socket(host, port);
                InputStream remoteInput = remoteSocket.getInputStream();
                OutputStream remoteOut = remoteSocket.getOutputStream()
        ) {
            System.out.println("connected: " + remoteSocket);

            if (!httpBasicAuth(httpRequest, in, out)) {
                System.out.println("UNAUTHORIZED!");
                return;
            }

            new Thread(new StreamTransfer(() -> remoteSocket.isConnected(), remoteInput, out)).start();

            String okClientResponse = "HTTP/1.1 200 OK\r\n\r\n";
            out.write(okClientResponse.getBytes());
            System.out.println("RESPONSE:");
            System.out.println(okClientResponse);

            new StreamTransfer(() -> remoteSocket.isConnected(), in, remoteOut).run();

            System.out.println("disconnect remote host: " + httpRequest.getHost() + " (" + remoteSocket + ")");
        }

    }

    protected void handlePlainMethod(HttpRequest httpRequest, InputStream in, OutputStream out) throws IOException {
        URL url = new URL(httpRequest.getPath());
        httpRequest.setPath(url.getPath());

        try (
                Socket remoteSocket = new Socket(httpRequest.getHost(), 80);
                InputStream remoteInput = remoteSocket.getInputStream();
                OutputStream remoteOut = remoteSocket.getOutputStream()
        ) {
            System.out.println("connected to " + remoteSocket);

            remoteOut.write(httpRequest.toFullRequest().getBytes());

            while (remoteSocket.isConnected()) {
                int inputSize = remoteInput.available();

                if (inputSize == 0) {
                    LockSupport.parkNanos(1000);
                    continue;
                }

                byte[] buf = new byte[inputSize];
                remoteInput.read(buf);
                out.write(buf);
            }

            System.out.println("disconnect remote host: " + httpRequest.getHost() + " (" + remoteSocket + ")");
        }

    }

    protected boolean httpBasicAuth(HttpRequest httpRequest, InputStream in, OutputStream out) throws IOException {
        String authString = httpRequest.getHeaders().getOrDefault("Proxy-Authorization", "");
        String envAuthString = System.getenv("USER_AUTH");

        System.out.println("start httpBasicAuth() with " + authString + " vs " + envAuthString);
        System.out.flush();

        if (authString.isEmpty()) {
            String unauthorizedResponse = "HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=\"CyberNet\"\r\n\r\n";
            out.write(unauthorizedResponse.getBytes());
            System.out.println("RESPONSE:");
            System.out.println(unauthorizedResponse);

            in.skip(in.available());
            return false;
        }

        if (!envAuthString.equals(authString)) {
            String unauthorizedResponse = "HTTP/1.1 403 Forbidden\r\n\r\n";
            out.write(unauthorizedResponse.getBytes());
            System.out.println("RESPONSE:");
            System.out.println(unauthorizedResponse);

            in.skip(in.available());
            return false;
        }

        return true;
    }

}
