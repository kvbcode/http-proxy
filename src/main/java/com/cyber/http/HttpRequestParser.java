package com.cyber.http;

public class HttpRequestParser {

    public HttpRequest parse(String httpRequestLines) {
        HttpRequest httpRequest = new HttpRequest();
        String[] lines = httpRequestLines.split("\n");

        String[] connectionParts = lines[0].split(" ");

        httpRequest.setMethod(connectionParts[0].trim());
        httpRequest.setPath(connectionParts[1].trim());
        httpRequest.setHttpVersion(connectionParts[2].trim());

        int i;
        for (i = 1; i < lines.length; i++) {
            String[] headerKeyValue = lines[i].trim().split(": ");

            if (headerKeyValue.length == 1 && lines[i].trim().isEmpty()) break;

            httpRequest.getHeaders().put(headerKeyValue[0], headerKeyValue[1]);
        }

        StringBuilder sb = new StringBuilder();
        for (i += 1; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        httpRequest.setBody(sb.toString());

        return httpRequest;
    }

}
