package com.sshakusora.riautomobility.util;

import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RIAutomobileSeatRegistry {
    private static final Map<AutomobileFrame, List<Vec3>> customSeatsPosition = new HashMap<>();

    public static void register(AutomobileFrame frame, List<Vec3> seats) {
        customSeatsPosition.put(frame, seats);
    }

    public static List<Vec3> getSeats(AutomobileFrame frame){
        return customSeatsPosition.getOrDefault(frame, List.of());
    }

    public static Vec3 getSeat(AutomobileFrame frame, int index){
        List<Vec3> seats = getSeats(frame);
        return index >= 0 && index < seats.size() ? seats.get(index) : Vec3.ZERO;
    }
}
