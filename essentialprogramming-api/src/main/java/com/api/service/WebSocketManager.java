package com.api.service;


import com.api.output.WebSocketMessage;
import com.api.entities.Tournament;
import com.api.mapper.ChessTournamentMapper;
import com.google.common.collect.Multimap;
import io.undertow.websockets.core.*;


public class WebSocketManager {
    public static Multimap<String, WebSocketChannel> userKeysWithChannel;

    public static AbstractReceiveListener getListener(Multimap<String, WebSocketChannel> channelMap) {
        userKeysWithChannel = channelMap;
        return new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel,
                                             BufferedTextMessage message) {
            }
        };
    }

    public static void sendMessage(String message) {
        if (userKeysWithChannel != null) {
            for (WebSocketChannel session : userKeysWithChannel.values()) {
                WebSockets.sendText(message, session, null);
            }
        } else {
            System.out.println("No WebSocket connection available!");
        }
    }

    public static void sendMessage(WebSocketMessage message) {
        if (userKeysWithChannel != null) {
            for (WebSocketChannel session : userKeysWithChannel.values()) {
                WebSockets.sendText(message.toString(), session, null);
            }
        }
    }

    public static void sendMessageByUserKey(WebSocketMessage message, String userKey) {
        if (userKeysWithChannel != null && userKeysWithChannel.containsKey(userKey)) {
            for (WebSocketChannel session : userKeysWithChannel.get(userKey)
            ) {
                WebSockets.sendText(message.toString(), session, null);
            }
        }
    }



    public static void sendTournamentInformation(Tournament tournament) {
        if (userKeysWithChannel != null) {
            for (WebSocketChannel session : userKeysWithChannel.values()) {
                WebSockets.sendText(ChessTournamentMapper.tournamentJSONToString(tournament), session,null);
            }
        } else {
            System.out.println("No WebSocket connection available!");
        }
    }
}

