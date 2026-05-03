/**
 * This test verifies that the PacketType enum defines the expected packet
 * types used by the network protocol and that their names match the values
 * expected during packet parsing and processing.
 */

package jdemic.DedicatedServer.network.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketTypeTest {

    @Test
    void shouldContainExpectedPacketTypes() {
        // This test verifies that the protocol enum contains the expected packet types.
        PacketType[] values = PacketType.values();

        assertNotNull(values);
        assertEquals(6, values.length);
        assertTrue(contains(PacketType.PING, values));
        assertTrue(contains(PacketType.PONG, values));
        assertTrue(contains(PacketType.GAME_DATA, values));
        assertTrue(contains(PacketType.CONNECT, values));
        assertTrue(contains(PacketType.DISCONNECT, values));
        assertTrue(contains(PacketType.ERROR, values));
    }

    @Test
    void shouldExposeCorrectEnumNames() {
        // This test verifies that enum constants keep the exact names used by the network protocol.
        assertEquals("PING", PacketType.PING.name());
        assertEquals("PONG", PacketType.PONG.name());
        assertEquals("GAME_DATA", PacketType.GAME_DATA.name());
        assertEquals("CONNECT", PacketType.CONNECT.name());
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