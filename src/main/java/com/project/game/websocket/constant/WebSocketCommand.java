package com.project.game.websocket.constant;

import java.util.Arrays;

public enum WebSocketCommand {
    GREETING("GREETING"),
    READY("READY"),
    START("START"),
    TURN_GAME("TURN_GAME"),
    END_GAME("END_GAME"),
    SURRENDER_GAME("SURRENDER_GAME"),
    QUIT_GAME("QUIT_GAME"),
    EMPTY("EMPTY");

    private final String command;

    WebSocketCommand(String command) {
        this.command = command;
    }

    public static WebSocketCommand findWebSocketCommand(String command) {
        return Arrays.stream(WebSocketCommand.values())
            .filter(webSocketCommand -> command.equals(webSocketCommand.command))
            .findAny()
            .orElse(EMPTY);
    }
}
