package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.definition.RIAutomobileDefinition;
import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RIAutomobileSeatRegistry {
    public static List<SeatPos> getSeats(AutomobileFrame frame){
        return RIAutomobileRegistry.get(frame).seats().stream()
                .map(seat -> new SeatPos(seat.pos()))
                .toList();
    }

    public static SeatPos getSeat(AutomobileFrame frame, int index){
        List<RIAutomobileDefinition.SeatPos> seats = RIAutomobileRegistry.get(frame).seats();
        return index >= 0 && index < seats.size() ? new SeatPos(seats.get(index).pos()) : SeatPos.zero();
    }

    public static SeatPos getSeat(AutomobileEntity auto, Entity passenger){
        if (auto instanceof RIAutomobileEntity riautomobile) {
            return getSeat(auto.getFrame(), riautomobile.getSeatIndex(passenger));
        }
        List<SeatPos> seats = getSeats(auto.getFrame());
        int idx = auto.getPassengers().indexOf(passenger);
        return idx >= 0 && idx < seats.size() ? seats.get(idx) : SeatPos.zero();
    }

    public static SeatPos getSeat(Entity e, Entity passenger){
        if(e instanceof AutomobileEntity){
            return getSeat((AutomobileEntity) e, passenger);
        } else {
            return SeatPos.zero();
        }
    }

    public static class SeatPos {
        public Vec3 pos;

        public SeatPos(Vec3 pos) {
            this.pos = pos;
        }

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
