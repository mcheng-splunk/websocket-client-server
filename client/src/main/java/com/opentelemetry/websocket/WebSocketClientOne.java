package com.opentelemetry.websocket;


import com.opentelemetry.websocket.config.OtelSdkConfiguration;
import com.opentelemetry.websocket.model.IncomingMessage;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class WebSocketClientOne {


//    @Autowired
//    private static SimpMessageSendingOperations messagingTemplate;
//

    private static final Logger logger = LogManager.getLogger(WebSocketClientOne.class);

    private static final OpenTelemetry openTelemetry = OtelSdkConfiguration.initOpenTelemetry();

    private static final Tracer tracer =
            openTelemetry.getTracer("io.opentelemetry.WebSocketClientOne");

    private static final TextMapPropagator textMapPropagator =
            openTelemetry.getPropagators().getTextMapPropagator();


    // Export traces to log
    // Inject the span context into the request
    //private static final TextMapSetter<HttpURLConnection> setter = URLConnection::setRequestProperty;

//    private static final TextMapSetter<HttpURLConnection> setter =
//            new TextMapSetter<HttpURLConnection>() {
//                @Override
//                public void set(HttpURLConnection carrier, String key, String value) {
//                    // Insert the context as Header
//                    carrier.setRequestProperty(key, value);
//                }
//            };
//    private static final TextMapSetter<HttpURLConnection> setter = URLConnection::setRequestProperty;

    private static class MyHeadersAdapter implements TextMapSetter<StompHeaders> {
        @Nullable
        @Override
        public void set(StompHeaders headers,  String key, String value) {
            // Insert the context as Header
            headers.add(key, value);
        }
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {


        WebSocketClient client = new StandardWebSocketClient();


        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // OpenTelemetry instrumentation Code

        Span span =
                tracer.spanBuilder("producer-POST /").setSpanKind(SpanKind.PRODUCER).startSpan();

        try (Scope scope = span.makeCurrent()) {
            // **************
            ClientOneSessionHandler clientOneSessionHandler = new ClientOneSessionHandler();
            ListenableFuture<StompSession> sessionAsync =
                    stompClient.connect("ws://localhost:8080/websocket-server", clientOneSessionHandler);
            StompSession session = sessionAsync.get();

            System.out.println("Client session: " + session.getSessionId());
            session.subscribe("/topic/messages", clientOneSessionHandler);

            UUID uuid = UUID.randomUUID();

            span.setAttribute("UUID", uuid.toString());
            // **************

            StompHeaders headers = new StompHeaders();

            System.out.println("client Context after inject is: " + Context.current());
//            openTelemetry.getPropagators()
//                    .getTextMapPropagator()
//                    .inject(Context.current(), headers, (carrier, key, value) -> {
//                        if(carrier != null){
//                            //carrier.set(key, value);
//                            System.out.println("~~~~carrier type is: " + carrier.getClass().getName());
//                            carrier.add(key, value);
//                        } else {
//                            System.out.println("~~~~carrier type is: " + carrier.getClass().getName());
//                        }
//                    });
            textMapPropagator.inject(Context.current(), headers, new MyHeadersAdapter());

            headers.setDestination("/app/process-message");
            headers.add("marvel", "ironman");

            System.out.println("Headers after inject: " + headers);

            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                System.out.println("@@@@ Key: " + entry.getKey() + " Value: " + entry.getValue());
            }


            while (true) {
                try {

                    IncomingMessage incomingMessage =  new IncomingMessage(uuid.toString() + " " + System.currentTimeMillis());

                    // session.send("/app/process-message", new IncomingMessage(uuid.toString() + " " + System.currentTimeMillis()));

//                    headers.setDestination("/app/process-message");
                    session.send(headers, incomingMessage);

                    Thread.sleep(10000);

                } finally {
                    // Close the span
                    span.end();
                }
            }
        }
    }
}

