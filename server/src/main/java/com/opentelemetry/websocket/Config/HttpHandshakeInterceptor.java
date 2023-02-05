package com.opentelemetry.websocket.Config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession();
            Enumeration keys = session.getAttributeNames();
            System.out.println("keys have " + keys.hasMoreElements());
//            while (keys.hasMoreElements())
//            {
//                String key = (String)keys.nextElement();
//                System.out.println(key + ": " + session.getAttribute(key) + "<br>");
//            }
//            System.out.println("session information is : " + session.getAttributeNames());
            attributes.put("sessionId", session.getId());
            System.out.println("session information is : " + session.getId());
        }
        return true;
    }

    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception ex) {
    }
}