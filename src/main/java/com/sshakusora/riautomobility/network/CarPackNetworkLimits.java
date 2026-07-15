package com.sshakusora.riautomobility.network;

public final class CarPackNetworkLimits {
    // Minecraft 1.20.1 rejects a complete custom payload above 32767 bytes.
    public static final int MAX_CHUNK_DATA_SIZE = 24 * 1024;

    private CarPackNetworkLimits() {
    }
}
