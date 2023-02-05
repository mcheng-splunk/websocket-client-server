package com.opentelemetry.websocket.controller;

import com.google.gson.Gson;
import com.opentelemetry.websocket.Config.OtelSdkConfiguration;
import com.opentelemetry.websocket.model.IncomingMessage;
import com.opentelemetry.websocket.model.OutgoingMessage;
import com.sun.net.httpserver.HttpExchange;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


@Controller
public class ServerController {

//        @Autowired
//        private static SimpMessageSendingOperations messagingTemplate;

    private static final OpenTelemetry openTelemetry = OtelSdkConfiguration.initOpenTelemetry();

    private static final Tracer tracer =
            openTelemetry.getTracer("io.opentelemetry.example.websocket-server");


    public static final TextMapPropagator TEXT_MAP_PROPAGATOR =
            openTelemetry.getPropagators().getTextMapPropagator();


    // Extract the context from http headers (does not work in websocket because the header is StompHeaders)
//    private static final TextMapGetter<HttpExchange> getter =
//            new TextMapGetter<HttpExchange>() {
//                @Override
//                public Iterable<String> keys(HttpExchange carrier) {
//
//                    return carrier.getRequestHeaders().keySet();
//                }
//
//                @Override
//                public String get(HttpExchange carrier, String key) {
//                    if (carrier.getRequestHeaders().containsKey(key)) {
//                        System.out.println("carrier.getRequestHeaders().get(key).get(0) :" + carrier.getRequestHeaders().get(key).get(0));
//                        System.out.println("carrier.getRequestHeaders().keySet() :" + carrier.getRequestHeaders().keySet());
//                        System.out.println("carrier.getRequestHeaders().values() :" + carrier.getRequestHeaders().values());
//                        return carrier.getRequestHeaders().get(key).get(0);
//                    }
//                    return "";
//                }
//            };
//
//    public void handle(HttpExchange headers, IncomingMessage incomingMessage)  throws IOException{
//        Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
//                .extract(Context.current(), headers, getter);
//        System.out.println("client Context after inject is: " + Context.current());
//    }


    private static class HeadersAdapter implements TextMapGetter<SimpMessageHeaderAccessor> {
        @Nullable
        @Override
        public String get(@Nullable SimpMessageHeaderAccessor carrier, String key) {
            return carrier.getFirstNativeHeader(key);
        }

        @Override
        public Iterable<String> keys(SimpMessageHeaderAccessor carrier) {
            return carrier.toMap().keySet();
        }
    }


    @MessageMapping("/process-message")
    @SendTo("/topic/messages")
//    public OutgoingMessage processMessage(IncomingMessage incomingMessage) throws Exception{
//        Thread.sleep(1000);
//        return new OutgoingMessage("Hello " + incomingMessage.getName());
//    }

    public OutgoingMessage processMessage(@Payload IncomingMessage incomingMessage, MessageHeaders msgHeaders, SimpMessageHeaderAccessor headerAccessor) throws Exception {
//
//    public void processMessage(@Payload IncomingMessage incomingMessage, MessageHeaders msgHeaders, SimpMessageHeaderAccessor headerAccessor) throws Exception {


        Map<String, Object> map = headerAccessor.getMessageHeaders();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println("---- Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }

        System.out.println("@@@@ msgHeaders is " + msgHeaders);
//        Context context = TEXT_MAP_PROPAGATOR.extract(Context.current(), headerAccessor, getter);

        Context traceContext = TEXT_MAP_PROPAGATOR.extract(Context.current(), headerAccessor, new HeadersAdapter());

        System.out.println("$$$$ TEXT_MAP_PROPAGATOR Headers: " + traceContext);


        Span span =
                tracer.spanBuilder("GET /").setParent(traceContext).setSpanKind(SpanKind.SERVER).startSpan();
        try (Scope scope = span.makeCurrent()) {
            OutgoingMessage outgoingMessage = new OutgoingMessage("Hello " + incomingMessage.getName());


            var headers = new HashMap<>(headerAccessor.toMap());
            GlobalOpenTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), headers, (carrier, key, value) -> {
                        if(carrier != null){
                            carrier.put(key, value);
                        }
                    });

            //  System.out.println("Received message is : " + message);
//            messagingTemplate.convertAndSend("/topic/messages", outgoingMessage, headers );
            return outgoingMessage;
        } finally {
            // Close the span
            span.end();
        }
    }
}
