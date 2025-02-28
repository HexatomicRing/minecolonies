package com.minecolonies.core.entity.pathfinding.navigation;

import com.minecolonies.api.util.ShapeUtil;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Custom movement handler for minecolonies citizens (avoid jumping so much).
 * Note that the "speed" variable of the super is a speedFactor to our attributes base speed.
 */
public class MovementHandler extends MoveControl
{

    /**
     * Speed attribute holder
     */
    final AttributeInstance speedAtr;

    /**
     * Step height
     */
    private float stepHeight;

    /**
     * Speed value
     */
    private float speedValue;

    public MovementHandler(Mob mob)
    {
        super(mob);
        this.speedAtr = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        stepHeight = mob.maxUpStep();
        speedValue = (float) speedAtr.getValue();
    }

    @Override
    public void tick()
    {
        if (mob.tickCount % 20 == 0)
        {
            stepHeight = this.mob.maxUpStep();
            speedValue = (float) speedAtr.getValue();
        }

        if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.STRAFE)
        {
            final float speedAtt = speedValue;
            float speed = (float) this.speedModifier * speedAtt;
            float forward = this.strafeForwards;
            float strafe = this.strafeRight;
            float totalMovement = Mth.sqrt(forward * forward + strafe * strafe);
            if (totalMovement < 1.0F)
            {
                totalMovement = 1.0F;
            }

            totalMovement = speed / totalMovement;
            forward = forward * totalMovement;
            strafe = strafe * totalMovement;
            final float sinRotation = Mth.sin(this.mob.getYRot() * ((float) Math.PI / 180F));
            final float cosRotation = Mth.cos(this.mob.getYRot() * ((float) Math.PI / 180F));
            final float rot1 = forward * cosRotation - strafe * sinRotation;
            final float rot2 = strafe * cosRotation + forward * sinRotation;
            final PathNavigation pathnavigator = this.mob.getNavigation();

            if (!this.isWalkable(rot1, rot2)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }

            this.mob.setSpeed(speed);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
        }
        else if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.MOVE_TO)
        {
            this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
            final double xDif = this.wantedX - this.mob.getX();
            final double zDif = this.wantedZ - this.mob.getZ();
            final double yDif = this.wantedY - this.mob.getY();
            final double dist = xDif * xDif + yDif * yDif + zDif * zDif;
            if (dist < (double) 2.5000003E-7F)
            {
                this.mob.setZza(0.0F);
                return;
            }

            final float range = (float) (Mth.atan2(zDif, xDif) * (double) (180F / (float) Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), range, 90.0F));
            this.mob.setSpeed((float) (this.speedModifier * speedValue));
            final BlockPos blockpos = this.mob.blockPosition();
            final BlockState blockstate = this.mob.level().getBlockState(blockpos);

            if (PathfindingUtils.isWater(mob.level(), mob.blockPosition(), blockstate, blockstate.getFluidState())
                  && PathfindingUtils.isWater(mob.level(), mob.blockPosition().above(), null, null))
            {
                if (yDif != 0.0D)
                {
                    double d3 = Math.sqrt(xDif * xDif + yDif * yDif + zDif * zDif);
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0, (double) this.mob.getSpeed() * ((yDif + 0.3) / d3) * 0.1D, 0));
                }

                return;
            }

            final Block block = blockstate.getBlock();
            final VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            if ((yDif > (double) stepHeight && xDif * xDif + zDif * zDif < (double) Math.max(1.0F, this.mob.getBbWidth()))
                  || (!ShapeUtil.isEmpty(voxelshape) && this.mob.getY() < ShapeUtil.max(voxelshape, Direction.Axis.Y) + (double) blockpos.getY() && !blockstate.is(BlockTags.DOORS)
                        && !blockstate.is(
              BlockTags.FENCES) && !blockstate.is(BlockTags.FENCE_GATES))
                       && !block.isLadder(blockstate, this.mob.level(), blockpos, this.mob))
            {
                this.mob.getJumpControl().jump();
                this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.JUMPING;
            }
        }
        else if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.JUMPING)
        {
            this.mob.setSpeed((float) (this.speedModifier * speedValue));

            // Avoid beeing stuck in jumping while in liquids
            final BlockPos blockpos = this.mob.blockPosition();
            final BlockState blockstate = this.mob.level().getBlockState(blockpos);
            if (this.mob.onGround() || blockstate.liquid())
            {
                this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
            }
        }
        else
        {
            this.mob.setZza(0.0F);
        }
    }

    @Override
    public void setWantedPosition(double x, double y, double z, double speedIn)
    {
        super.setWantedPosition(x, y, z, speedIn);
        this.operation = MoveControl.Operation.MOVE_TO;
    }

    private boolean isWalkable(float p_24997_, float p_24998_)
    {
        PathNavigation pathnavigation = this.mob.getNavigation();
        if (pathnavigation != null)
        {
            NodeEvaluator nodeevaluator = pathnavigation.getNodeEvaluator();
            if (nodeevaluator != null
                  && nodeevaluator.getPathType(
              this.mob, BlockPos.containing(this.mob.getX() + (double) p_24997_, (double) this.mob.getBlockY(), this.mob.getZ() + (double) p_24998_)
            )
                       != PathType.WALKABLE)
            {
                return false;
            }
        }

        return true;
    }
}
