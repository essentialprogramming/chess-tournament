package com.api.output;

import lombok.Builder;

@Builder
public class WebSocketMessage {
        private String type;
        private String content;

    @Override
    public String toString() {
        return "notification_type : " + type + "\n" + "content : " + content;
    }
}
