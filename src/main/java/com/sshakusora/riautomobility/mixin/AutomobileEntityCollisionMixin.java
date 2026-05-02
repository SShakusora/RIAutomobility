package com.sshakusora.riautomobility.mixin;

import com.sshakusora.riautomobility.entity.HitboxEntity;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.util.duck.CollisionArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
     * Expands collision detection to include hitboxes.
     */
    @Overwrite(remap = false)
    public void collisionStateTick() {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        wasOnGround = automobileOnGround;
        automobileOnGround = false;
        isFloorDirectlyBelow = false;
        touchingWall = false;

        List<AABB> allBoxes = new ArrayList<>();
        allBoxes.add(self.getBoundingBox());
        
        if (self instanceof RIAutomobileEntity riAuto) {
            for (HitboxEntity hitbox : riAuto.getHitboxEntities()) {
                if (hitbox.isAlive()) {
                    allBoxes.add(hitbox.getBoundingBox());
                }
            }
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        
        for (AABB box : allBoxes) {
            minX = Math.min(minX, box.minX);
            minY = Math.min(minY, box.minY);
            minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX);
            maxY = Math.max(maxY, box.maxY);
            maxZ = Math.max(maxZ, box.maxZ);
        }
        
        var start = BlockPos.containing(minX - 0.1, minY - 0.2, minZ - 0.1);
        var end = BlockPos.containing(maxX + 0.1, maxY + 0.2 + self.maxUpStep(), maxZ + 0.1);
        var shapeCtx = CollisionContext.of(self);
        
        boolean wallHit = false;
        boolean stepWallHit = false;
        
        List<VoxelShape> groundCuboids = new ArrayList<>();
        List<VoxelShape> floorCuboids = new ArrayList<>();
        List<VoxelShape> wallCuboids = new ArrayList<>();
        List<VoxelShape> stepWallCuboids = new ArrayList<>();
        
        Vec3 wallOffset = this.lastVelocity.normalize().scale(0.12);
        
        for (AABB box : allBoxes) {
            var groundBox = new AABB(box.minX, box.minY - 0.04, box.minZ, box.maxX, box.minY, box.maxZ);
            var width = (box.getXsize() + box.getZsize()) * 0.5f;
            var floorBox = new AABB(box.minX + (width * 0.94), box.minY - 0.05, box.minZ + (width * 0.94), box.maxX - (width * 0.94), box.minY, box.maxZ - (width * 0.94));
            var wallBox = box.deflate(0.05).move(wallOffset);
            
            groundCuboids.add(Shapes.create(groundBox));
            floorCuboids.add(Shapes.create(floorBox));
            wallCuboids.add(Shapes.create(wallBox));
            stepWallCuboids.add(Shapes.create(wallBox).move(0, self.maxUpStep() - 0.05, 0));
        }

        if (self.level().hasChunksAt(start, end)) {
            var pos = new BlockPos.MutableBlockPos();
            for (int x = start.getX(); x <= end.getX(); ++x) {
                for (int y = start.getY(); y <= end.getY(); ++y) {
                    for (int z = start.getZ(); z <= end.getZ(); ++z) {
                        pos.set(x, y, z);
                        var state = self.level().getBlockState(pos);
                        var blockShape = state.getCollisionShape(self.level(), pos, shapeCtx).move(pos.getX(), pos.getY(), pos.getZ());
                        
                        for (int i = 0; i < allBoxes.size(); i++) {
                            this.automobileOnGround |= Shapes.joinIsNotEmpty(blockShape, groundCuboids.get(i), BooleanOp.AND);
                            this.isFloorDirectlyBelow |= Shapes.joinIsNotEmpty(blockShape, floorCuboids.get(i), BooleanOp.AND);
                        }
                        for (int i = 0; i < allBoxes.size(); i++) {
                            wallHit |= Shapes.joinIsNotEmpty(blockShape, wallCuboids.get(i), BooleanOp.AND);
                            stepWallHit |= Shapes.joinIsNotEmpty(blockShape, stepWallCuboids.get(i), BooleanOp.AND);
                        }
                    }
                }
            }
        }

        this.touchingWall = wallHit && stepWallHit;

        var otherColliders = new HashSet<CollisionArea>();
        this.accumulateCollisionAreas(otherColliders);
        for (AABB box : allBoxes) {
            var groundBox = new AABB(box.minX, box.minY - 0.04, box.minZ, box.maxX, box.minY, box.maxZ);
            this.automobileOnGround |= otherColliders.stream().anyMatch(col -> col.boxIntersects(groundBox));
        }
    }
}
