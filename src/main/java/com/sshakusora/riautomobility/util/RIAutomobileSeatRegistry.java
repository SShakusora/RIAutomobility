package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RIAutomobileSeatRegistry {
    private static final Map<AutomobileFrame, List<SeatPos>> CUSTOM_SEATS_POSITION = new HashMap<>();

    public static void register(AutomobileFrame frame, List<SeatPos> seats) {
        CUSTOM_SEATS_POSITION.put(frame, seats);
    }

    public static List<SeatPos> getSeats(AutomobileFrame frame){
        return CUSTOM_SEATS_POSITION.getOrDefault(frame, List.of());
    }

    public static SeatPos getSeat(AutomobileFrame frame, int index){
        List<SeatPos> seats = getSeats(frame);
        return index >= 0 && index < seats.size() ? seats.get(index) : SeatPos.zero();
    }

    public static SeatPos getSeat(AutomobileEntity auto, Entity passenger){
        List<SeatPos> seats = getSeats(auto.getFrame());
        int idx = auto.getPassengers().indexOf(passenger);
        return idx >= 0 && idx < seats.size() ? seats.get(idx) : SeatPos.zero();
    }

    public static SeatPos getSeat(DriverSeatEntity seat, Entity passenger) {
        AutomobileEntity auto = (AutomobileEntity) seat.getVehicle();
        if (auto != null) {
            return getSeat(auto.getFrame(), 0);
        } else {
            return SeatPos.zero();
        }
    }

    public static SeatPos getSeat(Entity e, Entity passenger){
        if(e instanceof AutomobileEntity){
            return getSeat((AutomobileEntity) e, passenger);
        } else if(e instanceof DriverSeatEntity){
            return getSeat((DriverSeatEntity) e, passenger);
        } else {
            return SeatPos.zero();
        }
    }

    public static class SeatPos {
        public Vec3 pos;

        public SeatPos(double x, double z) {
            this.pos = new Vec3(x, 0, z);
        }
        public SeatPos(double x, double y, double z) {
            this.pos = new Vec3(x, y, z);
        }
        public static SeatPos zero() {
            return new SeatPos(0, 0);
        }
    }
}
