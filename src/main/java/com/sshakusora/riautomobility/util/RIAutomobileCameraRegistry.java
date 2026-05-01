package com.sshakusora.riautomobility.util;

import com.sshakusora.riautomobility.definition.RIAutomobileRegistry;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RIAutomobileCameraRegistry {
    public static Vec3 getCameraPos(AutomobileFrame frame, int index){
        List<Vec3> cameraPoses = getCameraPoses(frame);
        return index >= 0 && index < cameraPoses.size() ? cameraPoses.get(index) : Vec3.ZERO;
    }

    public static List<Vec3> getCameraPoses(AutomobileFrame frame) {
        return RIAutomobileRegistry.get(frame).cameraPositions();
    }

    public static Vec3 getCameraPos(AutomobileEntity auto, Entity passenger){
        List<Vec3> cameraPoses = getCameraPoses(auto.getFrame());
        int idx = auto instanceof RIAutomobileEntity riautomobile ? riautomobile.getVisualSeatIndex(passenger) : auto.getPassengers().indexOf(passenger);
        return idx >= 0 && idx < cameraPoses.size() ? cameraPoses.get(idx) : Vec3.ZERO;
    }

    public static Vec3 getCameraPos(Entity e, Entity passenger){
        if(e instanceof AutomobileEntity){
            return getCameraPos((AutomobileEntity) e, passenger);
        } else {
            return Vec3.ZERO;
        }
    }
}
