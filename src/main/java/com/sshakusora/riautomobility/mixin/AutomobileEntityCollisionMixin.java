package com.sshakusora.riautomobility.mixin;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.util.duck.CollisionArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.HashSet;

@SuppressWarnings("deprecation")
@Mixin(AutomobileEntity.class)
public abstract class AutomobileEntityCollisionMixin {
    @Shadow private boolean wasOnGround;
    @Shadow private boolean automobileOnGround;
    @Shadow private boolean isFloorDirectlyBelow;
    @Shadow private boolean touchingWall;
    @Shadow private Vec3 lastVelocity;
    @Shadow public abstract void accumulateCollisionAreas(Collection<CollisionArea> areas);

    /**
     * Fixes negative-Y ground checks by flooring world coordinates instead of truncating toward zero.
     */
    @Overwrite(remap = false)
    public void collisionStateTick() {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        wasOnGround = automobileOnGround;
        automobileOnGround = false;
        isFloorDirectlyBelow = false;
        touchingWall = false;

        var box = self.getBoundingBox();
        var groundBox = new AABB(box.minX, box.minY - 0.04, box.minZ, box.maxX, box.minY, box.maxZ);
        var width = (box.getXsize() + box.getZsize()) * 0.5f;
        var floorBox = new AABB(box.minX + (width * 0.94), box.minY - 0.05, box.minZ + (width * 0.94), box.maxX - (width * 0.94), box.minY, box.maxZ - (width * 0.94));
        var wallBox = box.deflate(0.05).move(this.lastVelocity.normalize().scale(0.12));
        var start = BlockPos.containing(box.minX - 0.1, box.minY - 0.2, box.minZ - 0.1);
        var end = BlockPos.containing(box.maxX + 0.1, box.maxY + 0.2 + self.maxUpStep(), box.maxZ + 0.1);
        var groundCuboid = Shapes.create(groundBox);
        var floorCuboid = Shapes.create(floorBox);
        var wallCuboid = Shapes.create(wallBox);
        var stepWallCuboid = wallCuboid.move(0, self.maxUpStep() - 0.05, 0);
        boolean wallHit = false;
        boolean stepWallHit = false;
        var shapeCtx = CollisionContext.of(self);

        if (self.level().hasChunksAt(start, end)) {
            var pos = new BlockPos.MutableBlockPos();
            for (int x = start.getX(); x <= end.getX(); ++x) {
                for (int y = start.getY(); y <= end.getY(); ++y) {
                    for (int z = start.getZ(); z <= end.getZ(); ++z) {
                        pos.set(x, y, z);
                        var state = self.level().getBlockState(pos);
                        var blockShape = state.getCollisionShape(self.level(), pos, shapeCtx).move(pos.getX(), pos.getY(), pos.getZ());
                        this.automobileOnGround |= Shapes.joinIsNotEmpty(blockShape, groundCuboid, BooleanOp.AND);
                        this.isFloorDirectlyBelow |= Shapes.joinIsNotEmpty(blockShape, floorCuboid, BooleanOp.AND);
                        wallHit |= Shapes.joinIsNotEmpty(blockShape, wallCuboid, BooleanOp.AND);
                        stepWallHit |= Shapes.joinIsNotEmpty(blockShape, stepWallCuboid, BooleanOp.AND);
                    }
                }
            }
        }

        this.touchingWall = wallHit && stepWallHit;

        var otherColliders = new HashSet<CollisionArea>();
        this.accumulateCollisionAreas(otherColliders);
        this.automobileOnGround |= otherColliders.stream().anyMatch(col -> col.boxIntersects(groundBox));
    }
}
