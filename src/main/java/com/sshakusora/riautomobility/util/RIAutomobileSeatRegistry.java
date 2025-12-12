package com.sshakusora.riautomobility.util;

import io.github.foundationgames.automobility.automobile.AutomobileFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RIAutomobileSeatRegistry {
    private static final Map<AutomobileFrame, List<SeatPos>> customSeatsPosition = new HashMap<>();

    public static void register(AutomobileFrame frame, List<SeatPos> seats) {
        customSeatsPosition.put(frame, seats);
    }

    public static List<SeatPos> getSeats(AutomobileFrame frame){
        return customSeatsPosition.getOrDefault(frame, List.of());
    }

    public static SeatPos getSeat(AutomobileFrame frame, int index){
        List<SeatPos> seats = getSeats(frame);
        return index >= 0 && index < seats.size() ? seats.get(index) : SeatPos.zero();
    }

    public static class SeatPos {
        public double x;
        public double y;
        public double z;

        public SeatPos(double x, double z) {
            this.x = x;
            this.y = 0;
            this.z = z;
        }
        public SeatPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public static SeatPos zero() {
            return new SeatPos(0, 0);
        }
    }
}
