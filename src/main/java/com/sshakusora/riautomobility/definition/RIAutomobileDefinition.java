package com.sshakusora.riautomobility.definition;

import io.github.foundationgames.automobility.automobile.attachment.FrontAttachmentType;
import io.github.foundationgames.automobility.automobile.attachment.RearAttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public record RIAutomobileDefinition(
        List<SeatPos> seats,
        EntityDimensions dimensions,
        List<Vec3> cameraPositions,
        List<Hitbox> hitboxes,
        boolean hideEngine,
        boolean frontAttachmentEnabled,
        boolean rearAttachmentEnabled,
        List<ResourceLocation> frontAttachmentWhitelist,
        List<ResourceLocation> frontAttachmentBlacklist,
        List<ResourceLocation> rearAttachmentWhitelist,
        List<ResourceLocation> rearAttachmentBlacklist
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

    public boolean allowsFrontAttachment(FrontAttachmentType<?> type) {
        return isAttachmentAllowed(type.getId(), this.frontAttachmentEnabled, this.frontAttachmentWhitelist, this.frontAttachmentBlacklist);
    }

    public boolean allowsRearAttachment(RearAttachmentType<?> type) {
        return isAttachmentAllowed(type.getId(), this.rearAttachmentEnabled, this.rearAttachmentWhitelist, this.rearAttachmentBlacklist);
    }

    private boolean isAttachmentAllowed(ResourceLocation id, boolean enabled, List<ResourceLocation> whitelist, List<ResourceLocation> blacklist) {
        if (!enabled || id == null) {
            return false;
        }
        if (blacklist.contains(id)) {
            return false;
        }
        return whitelist.isEmpty() || whitelist.contains(id);
    }

    public static final class Builder {
        private List<SeatPos> seats = List.of();
        private EntityDimensions dimensions = EntityDimensions.scalable(1.0F, 0.66F);
        private List<Vec3> cameraPositions = List.of(Vec3.ZERO);
        private List<Hitbox> hitboxes = List.of();
        private boolean hideEngine = false;
        private boolean frontAttachmentEnabled = true;
        private boolean rearAttachmentEnabled = true;
        private List<ResourceLocation> frontAttachmentWhitelist = List.of();
        private List<ResourceLocation> frontAttachmentBlacklist = List.of();
        private List<ResourceLocation> rearAttachmentWhitelist = List.of();
        private List<ResourceLocation> rearAttachmentBlacklist = List.of();

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

        public Builder hideEngine(boolean hideEngine) {
            this.hideEngine = hideEngine;
            return this;
        }

        public Builder frontAttachmentEnabled(boolean enabled) {
            this.frontAttachmentEnabled = enabled;
            return this;
        }

        public Builder rearAttachmentEnabled(boolean enabled) {
            this.rearAttachmentEnabled = enabled;
            return this;
        }

        public Builder frontAttachmentWhitelist(List<ResourceLocation> ids) {
            this.frontAttachmentWhitelist = List.copyOf(ids);
            return this;
        }

        public Builder frontAttachmentWhitelist(ResourceLocation... ids) {
            return this.frontAttachmentWhitelist(Arrays.asList(ids));
        }

        public Builder frontAttachmentWhitelist(FrontAttachmentType<?>... types) {
            return this.frontAttachmentWhitelist(Arrays.stream(types).map(FrontAttachmentType::getId).toList());
        }

        public Builder frontAttachmentBlacklist(List<ResourceLocation> ids) {
            this.frontAttachmentBlacklist = List.copyOf(ids);
            return this;
        }

        public Builder frontAttachmentBlacklist(ResourceLocation... ids) {
            return this.frontAttachmentBlacklist(Arrays.asList(ids));
        }

        public Builder frontAttachmentBlacklist(FrontAttachmentType<?>... types) {
            return this.frontAttachmentBlacklist(Arrays.stream(types).map(FrontAttachmentType::getId).toList());
        }

        public Builder rearAttachmentWhitelist(List<ResourceLocation> ids) {
            this.rearAttachmentWhitelist = List.copyOf(ids);
            return this;
        }

        public Builder rearAttachmentWhitelist(ResourceLocation... ids) {
            return this.rearAttachmentWhitelist(Arrays.asList(ids));
        }

        public Builder rearAttachmentWhitelist(RearAttachmentType<?>... types) {
            return this.rearAttachmentWhitelist(Arrays.stream(types).map(RearAttachmentType::getId).toList());
        }

        public Builder rearAttachmentBlacklist(List<ResourceLocation> ids) {
            this.rearAttachmentBlacklist = List.copyOf(ids);
            return this;
        }

        public Builder rearAttachmentBlacklist(ResourceLocation... ids) {
            return this.rearAttachmentBlacklist(Arrays.asList(ids));
        }

        public Builder rearAttachmentBlacklist(RearAttachmentType<?>... types) {
            return this.rearAttachmentBlacklist(Arrays.stream(types).map(RearAttachmentType::getId).toList());
        }

        public RIAutomobileDefinition build() {
            return new RIAutomobileDefinition(
                    this.seats,
                    this.dimensions,
                    this.cameraPositions,
                    this.hitboxes,
                    this.hideEngine,
                    this.frontAttachmentEnabled,
                    this.rearAttachmentEnabled,
                    this.frontAttachmentWhitelist,
                    this.frontAttachmentBlacklist,
                    this.rearAttachmentWhitelist,
                    this.rearAttachmentBlacklist
            );
        }
    }
}
