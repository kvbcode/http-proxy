package com.cyber.http;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class HttpRequest {
    public static String METHOD_CONNECT = "CONNECT";

    private String method;
    private String path;
    private String httpVersion;
    private Map<String, String> headers;
    private String body;

    public HttpRequest() {
        headers = new LinkedHashMap<>();
    }

    public String getHost() {
        if (METHOD_CONNECT.equals(method)) {
            return path;
        } else {
            return headers.get("Host");
        }
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
}
