package network.protocol;

public enum MessageType {
    CONNECT_REQUEST,
    CONNECT_RESPONSE,
    PLAYER_JOINED,
    PLAYER_LEFT,
    ERROR,
    LOBBY_STATE,
    SET_READY_REQUEST,
    START_GAME_REQUEST,
    GAME_STARTED
}
