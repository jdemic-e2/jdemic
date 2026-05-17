package jdemic.DedicatedServer.network.transport;
/**
 * Defines all allowed packet types used by the network protocol.
 */
public enum PacketType {
    PING,
    PONG,
    GAME_DATA,
    CONNECT,
    DISCONNECT,
    ERROR
}
