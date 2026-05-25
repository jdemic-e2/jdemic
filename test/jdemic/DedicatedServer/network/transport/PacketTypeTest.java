package jdemic.DedicatedServer.network.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketTypeTest {

    @Test
    void shouldContainExpectedPacketTypes() {
        PacketType[] values = PacketType.values();

        assertNotNull(values);
        assertEquals(9, values.length);
        assertTrue(contains(PacketType.PING, values));
        assertTrue(contains(PacketType.PONG, values));
        assertTrue(contains(PacketType.GAME_DATA, values));
        assertTrue(contains(PacketType.CONNECT, values));
        assertTrue(contains(PacketType.LOBBY_CHAT, values));
        assertTrue(contains(PacketType.LOBBY_READY, values));
        assertTrue(contains(PacketType.DISCONNECT, values));
        assertTrue(contains(PacketType.ERROR, values));
    }

    @Test
    void shouldExposeCorrectEnumNames() {
        assertEquals("PING", PacketType.PING.name());
        assertEquals("PONG", PacketType.PONG.name());
        assertEquals("GAME_DATA", PacketType.GAME_DATA.name());
        assertEquals("CONNECT", PacketType.CONNECT.name());
        assertEquals("LOBBY_CHAT", PacketType.LOBBY_CHAT.name());
        assertEquals("LOBBY_READY", PacketType.LOBBY_READY.name());
        assertEquals("DISCONNECT", PacketType.DISCONNECT.name());
        assertEquals("ERROR", PacketType.ERROR.name());
    }

    private boolean contains(PacketType target, PacketType[] values) {
        for (PacketType value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}