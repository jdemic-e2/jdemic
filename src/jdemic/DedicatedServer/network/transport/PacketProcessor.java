package jdemic.DedicatedServer.network.transport;

/**
 * PacketProcessor handles packets that have already passed validation and
 * converts them into the appropriate server-side actions. It checks the
 * packet type and routes each packet to the matching handler method, keeping
 * protocol logic separate from raw socket communication. This makes the
 * networking flow easier to maintain, extend, and integrate with other
 * modules such as session management, heartbeat handling, and game logic.
 */

public class PacketProcessor {

    public void process(Packet packet) {
        if (packet == null) {
            System.err.println("[PacketProcessor] Cannot process a null packet.");
            return;
        }

        if (packet.getType() == PacketType.PING) {
            handlePing(packet);
        } else if (packet.getType() == PacketType.PONG) {
            handlePong(packet);
        } else if (packet.getType() == PacketType.GAME_DATA) {
            handleGameData(packet);
        } else if (packet.getType() == PacketType.CONNECT) {
            handleConnect(packet);
        } else if (packet.getType() == PacketType.DISCONNECT) {
            handleDisconnect(packet);
        } else if (packet.getType() == PacketType.ERROR) {
            handleError(packet);
        } else {
            System.err.println("[PacketProcessor] Unsupported packet type: " + packet.getType());
        }
    }

    private void handlePing(Packet packet) {
        System.out.println("[PacketProcessor] Received PING packet: " + packet);
    }

    private void handlePong(Packet packet) {
        System.out.println("[PacketProcessor] Received PONG packet: " + packet);
    }

    private void handleGameData(Packet packet) {
        System.out.println("[PacketProcessor] Received GAME_DATA packet: " + packet);
    }

    private void handleConnect(Packet packet) {
        System.out.println("[PacketProcessor] Received CONNECT packet: " + packet);
    }

    private void handleDisconnect(Packet packet) {
        System.out.println("[PacketProcessor] Received DISCONNECT packet: " + packet);
    }

    private void handleError(Packet packet) {
        System.err.println("[PacketProcessor] Received ERROR packet: " + packet);
    }
}