package com.cyber;

import com.cyber.http.HttpRequest;
import com.cyber.http.HttpRequestParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpProxyServer {
    protected static final int BACKLOG_LENGTH = 50;
    protected static final int SO_TIMEOUT = 60;

    protected final InetAddress addr;
    protected final int port;
    protected final ServerSocket socket;
    protected final AtomicBoolean doWork;

    protected final HttpRequestParser httpRequestParser;

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
            System.out.println("NEW REQUEST from " + socket);

            if (httpRequest.isEmpty()) {
                System.out.println("EMPTY request");
                return;
            }

            System.out.println(httpRequest.toFullRequest());

            if (HttpRequest.METHOD_CONNECT.equals(httpRequest.getMethod())) {
                handleConnectMethod(httpRequest, in, out);
            } else {
                handlePlainMethod(httpRequest, in, out);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("close client socket: " + socket);
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected HttpRequest readHttpRequest(InputStream in) throws IOException {
        int available = in.available();
        byte[] buf = new byte[available];
        in.read(buf);
        String requestStr = new String(buf);
        return httpRequestParser.parse(requestStr);
    }

    protected void handleConnectMethod(HttpRequest httpRequest, InputStream in, OutputStream out) throws IOException {
        if (!httpBasicAuth(httpRequest, in, out)) {
            System.out.println("UNAUTHORIZED!");
            return;
        }

        try (
                Socket remoteSocket = new Socket(httpRequest.getHost(), httpRequest.getPort());
                InputStream remoteInput = remoteSocket.getInputStream();
                OutputStream remoteOut = remoteSocket.getOutputStream()
        ) {
            System.out.println("connected: " + remoteSocket);

            new Thread(new StreamTransfer(() -> !remoteSocket.isClosed(), remoteInput, out)).start();

            String okClientResponse = "HTTP/1.1 200 OK\r\n\r\n";
            out.write(okClientResponse.getBytes());
            System.out.println("FOUND RESPONSE:");
            System.out.println(okClientResponse);

            new StreamTransfer(() -> !remoteSocket.isClosed(), in, remoteOut).run();

            System.out.println("disconnect remote host: " + httpRequest.getHost() + " (" + remoteSocket + ")");
        }
    }

    protected void handlePlainMethod(HttpRequest httpRequest, InputStream in, OutputStream out) throws IOException {
        if (!httpBasicAuth(httpRequest, in, out)) {
            System.out.println("UNAUTHORIZED!");
            return;
        }

        try (
                Socket remoteSocket = new Socket(httpRequest.getHost(), httpRequest.getPort());
                InputStream remoteInput = remoteSocket.getInputStream();
                OutputStream remoteOut = remoteSocket.getOutputStream()
        ) {
            System.out.println("connected to " + remoteSocket);

            new Thread(new StreamTransfer(() -> !remoteSocket.isClosed(), remoteInput, out)).start();
            remoteOut.write(httpRequest.toFullRequest().getBytes());
            new StreamTransfer(() -> !remoteSocket.isClosed(), in, remoteOut).run();

            System.out.println("disconnect remote host: " + httpRequest.getHost() + " (" + remoteSocket + ")");
        }

    }

    protected boolean httpBasicAuth(HttpRequest httpRequest, InputStream in, OutputStream out) throws IOException {
        String authString = httpRequest.getHeaders().getOrDefault("Proxy-Authorization", "");
        String envAuthString = System.getenv("USER_AUTH");

        System.out.flush();

        if (authString.isEmpty()) {
            String unauthorizedResponse = "HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=\"CyberNet\"\r\n\r\n";
            out.write(unauthorizedResponse.getBytes());
            System.out.println("AUTH RESPONSE:");
            System.out.println(unauthorizedResponse);

            in.skip(in.available());
            return false;
        }

        if (!envAuthString.equals(authString)) {
            System.out.println("auth failed with " + authString);
            String unauthorizedResponse = "HTTP/1.1 403 Forbidden\r\n\r\n";
            out.write(unauthorizedResponse.getBytes());
            System.out.println("AUTH RESPONSE:");
            System.out.println(unauthorizedResponse);

            in.skip(in.available());
            return false;
        }

        return true;
    }

}
