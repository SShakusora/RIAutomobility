package com.sshakusora.riautomobility.mixin;

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
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("deprecation")
@Mixin(AutomobileEntity.class)
public abstract class AutomobileEntityCollisionMixin {
    @Shadow
    private boolean wasOnGround;
    @Shadow
    private boolean automobileOnGround;
    @Shadow
    private boolean isFloorDirectlyBelow;
    @Shadow
    private boolean touchingWall;
    @Shadow
    private Vec3 lastVelocity;

    @Shadow
    public abstract void accumulateCollisionAreas(Collection<CollisionArea> areas);

    @Unique
    private final Set<CollisionArea> riautomobility$collisionScratch = new HashSet<>();

    /**
     * Fixes negative-Y ground checks by flooring world coordinates instead of truncating toward zero.
     * Expands collision detection to include hitboxes.
     *
     * @author Shinonome Shakusora
     * @reason Preserve Automobility's collision behavior while fixing negative coordinates.
     */
    @Overwrite(remap = false)
    public void collisionStateTick() {
        AutomobileEntity self = (AutomobileEntity) (Object) this;
        wasOnGround = automobileOnGround;
        automobileOnGround = false;
        isFloorDirectlyBelow = false;
        touchingWall = false;

        // Attached hitboxes are interaction proxies. Letting them participate in
        // ground/wall checks makes the main automobile float or snag on terrain.
        AABB box = self.getBoundingBox();
        AABB groundBox = new AABB(box.minX, box.minY - 0.04, box.minZ, box.maxX, box.minY, box.maxZ);
        double width = (box.getXsize() + box.getZsize()) * 0.5F;
        AABB floorBox = new AABB(box.minX + width * 0.94, box.minY - 0.05, box.minZ + width * 0.94,
                box.maxX - width * 0.94, box.minY, box.maxZ - width * 0.94);
        AABB wallBox = box.deflate(0.05).move(this.lastVelocity.normalize().scale(0.12));
        BlockPos start = BlockPos.containing(box.minX - 0.1, box.minY - 0.2, box.minZ - 0.1);
        BlockPos end = BlockPos.containing(box.maxX + 0.1, box.maxY + 0.2 + self.maxUpStep(), box.maxZ + 0.1);
        var shapeCtx = CollisionContext.of(self);
        VoxelShape groundCuboid = Shapes.create(groundBox);
        VoxelShape floorCuboid = Shapes.create(floorBox);
        VoxelShape wallCuboid = Shapes.create(wallBox);
        VoxelShape stepWallCuboid = wallCuboid.move(0, self.maxUpStep() - 0.05, 0);
        boolean wallHit = false;
        boolean stepWallHit = false;

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

        riautomobility$collisionScratch.clear();
        this.accumulateCollisionAreas(riautomobility$collisionScratch);
        for (CollisionArea collider : riautomobility$collisionScratch) {
            if (collider.boxIntersects(groundBox)) {
                this.automobileOnGround = true;
                break;
            }
        }
    }
}
