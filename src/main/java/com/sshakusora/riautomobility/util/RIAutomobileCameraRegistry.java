package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.entity.DriverSeatEntity;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RIAutomobileCameraRegistry {
    private static final Map<AutomobileFrame, List<Vec3>> CUSTOM_CAMERA_POSITION = new HashMap<>();

    public static void register(AutomobileFrame frame, List<Vec3> cameraPoses) {
        CUSTOM_CAMERA_POSITION.put(frame, cameraPoses);
    }

    public static Vec3 getCameraPos(AutomobileFrame frame, int index){
        List<Vec3> cameraPoses = getCameraPoses(frame);
        return index >= 0 && index < cameraPoses.size() ? cameraPoses.get(index) : Vec3.ZERO;
    }

    public static List<Vec3> getCameraPoses(AutomobileFrame frame) {
        return CUSTOM_CAMERA_POSITION.getOrDefault(frame, List.of(Vec3.ZERO));
    }

    public static Vec3 getCameraPos(AutomobileEntity auto, Entity passenger){
        List<Vec3> cameraPoses = getCameraPoses(auto.getFrame());
        int idx = auto.getPassengers().indexOf(passenger);
        return idx >= 0 && idx < cameraPoses.size() ? cameraPoses.get(idx) : Vec3.ZERO;
    }

    public static Vec3 getCameraPos(DriverSeatEntity seat, Entity passenger) {
        AutomobileEntity auto = (AutomobileEntity) seat.getVehicle();
        if (auto != null) {
            return getCameraPos(auto.getFrame(), 0);
        } else {
            return Vec3.ZERO;
        }
    }

    public static Vec3 getCameraPos(Entity e, Entity passenger){
        if(e instanceof AutomobileEntity){
            return getCameraPos((AutomobileEntity) e, passenger);
        } else if(e instanceof DriverSeatEntity){
            return getCameraPos((DriverSeatEntity) e, passenger);
        } else {
            return Vec3.ZERO;
        }
    }
}
