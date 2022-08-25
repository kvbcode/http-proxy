package com.cyber.http;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class HttpRequest {
    public static final String METHOD_CONNECT = "CONNECT";

    private String method;
    private String path;
    private String httpVersion;
    private Map<String, String> headers;
    private String body;

    public HttpRequest() {
        headers = new LinkedHashMap<>();
    }

    public String getHost() {
        String hostWithPort = METHOD_CONNECT.equals(method)
                ? path
                : headers.getOrDefault("Host", "");
        String[] hostParts = hostWithPort.split(":");
        String host = hostParts[0];
        return host;
    }

    public int getPort() {
        String hostWithPort = METHOD_CONNECT.equals(method)
                ? path
                : headers.getOrDefault("Host", "");
        String[] hostParts = hostWithPort.split(":");
        int port = 80;

        if (hostParts.length > 1) {
            port = Integer.valueOf(hostParts[1]);
        }
        return port;
    }

    public String toFullRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMethod()).append(" ").append(getPath()).append(" ").append(getHttpVersion())
                .append("\r\n");

        headers.forEach((k, v) ->
                sb.append(k).append(": ").append(v).append("\r\n")
        );

        sb.append("\r\n");

        sb.append(getBody());

        return sb.toString();
    }

    public boolean isEmpty() {
        return method == null || "".equals(method) ||
                path == null || "".equals(path) ||
                httpVersion == null || "".equals(httpVersion);
    }
}
