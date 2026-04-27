package com.sshakusora.riautomobility.definition;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public record RIAutomobileDefinition(
        List<SeatPos> seats,
        EntityDimensions dimensions,
        List<Vec3> cameraPositions,
        List<Hitbox> hitboxes
) {
    public static final RIAutomobileDefinition DEFAULT = builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public record SeatPos(Vec3 pos) {
        public SeatPos(double x, double z) {
            this(new Vec3(x, 0, z));
        }

        public SeatPos(double x, double y, double z) {
            this(new Vec3(x, y, z));
        }

        public static SeatPos zero() {
            return new SeatPos(0, 0);
        }
    }

    public record Hitbox(Vec3 origin, float width, float height, boolean hasContainer) {
        public static final Hitbox DEFAULT = new Hitbox(Vec3.ZERO, 1.0f, 0.66f, false);
    }

    public static final class Builder {
        private List<SeatPos> seats = List.of();
        private EntityDimensions dimensions = EntityDimensions.scalable(1.0F, 0.66F);
        private List<Vec3> cameraPositions = List.of(Vec3.ZERO);
        private List<Hitbox> hitboxes = List.of();

        public Builder seats(List<SeatPos> seats) {
            this.seats = List.copyOf(seats);
            return this;
        }

        public Builder seats(SeatPos... seats) {
            return this.seats(Arrays.asList(seats));
        }

        public Builder dimensions(EntityDimensions dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder cameraPositions(List<Vec3> cameraPositions) {
            this.cameraPositions = List.copyOf(cameraPositions);
            return this;
        }

        public Builder cameraPositions(Vec3... cameraPositions) {
            return this.cameraPositions(Arrays.asList(cameraPositions));
        }

        public Builder hitboxes(List<Hitbox> hitboxes) {
            this.hitboxes = List.copyOf(hitboxes);
            return this;
        }

        public Builder hitboxes(Hitbox... hitboxes) {
            return this.hitboxes(Arrays.asList(hitboxes));
        }

        public RIAutomobileDefinition build() {
            return new RIAutomobileDefinition(this.seats, this.dimensions, this.cameraPositions, this.hitboxes);
        }
    }
}
