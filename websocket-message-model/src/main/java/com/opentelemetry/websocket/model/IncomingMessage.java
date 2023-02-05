package com.opentelemetry.websocket.model;

public class IncomingMessage {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public IncomingMessage(String name) {
            this.name = name;
        }

        public IncomingMessage() {
        }

    }
