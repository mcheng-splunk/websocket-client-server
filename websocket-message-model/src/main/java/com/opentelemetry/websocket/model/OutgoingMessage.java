package com.opentelemetry.websocket.model;

public class OutgoingMessage {
        private String content;

        public OutgoingMessage(String content) {
            this.content = content;
        }

        public OutgoingMessage() {
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
}
