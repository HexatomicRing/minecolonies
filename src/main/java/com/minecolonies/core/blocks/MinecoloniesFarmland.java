package com.minecolonies.core.blocks;

import com.minecolonies.api.blocks.AbstractBlockMinecolonies;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.SpecialPlantable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class MinecoloniesFarmland extends AbstractBlockMinecolonies<MinecoloniesFarmland> implements SimpleWaterloggedBlock
{
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final String FARMLAND         = "farmland";
    public static final String FLOODED_FARMLAND = "floodedfarmland";

    public static final    IntegerProperty MOISTURE     = BlockStateProperties.MOISTURE;
    protected final VoxelShape shape;

    private final ResourceLocation    blockId;

    public MinecoloniesFarmland(@NotNull final String blockName, final boolean waterLogged, final double height)
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).randomTicks().strength(0.6F).sound(SoundType.GRAVEL).isViewBlocking((s,g,p) -> true).isSuffocating((s,g,p) -> true));
        this.registerDefaultState(this.stateDefinition.any().setValue(MOISTURE, 0));
        this.blockId = new ResourceLocation(Constants.MOD_ID, blockName);;
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(waterLogged)));
        this.shape = Block.box(0.0, 0.0, 0.0, 16.0, height, 16.0);
    }

    @NotNull
    @Override
    public BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState newState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos)
    {
        if (direction == Direction.UP && !state.canSurvive(level, pos))
        {
            level.scheduleTick(pos, this, 1);
        }
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, direction, newState, level, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos)
    {
        if (level == null)
        {
            // This is for our solid checks.
            return true;
        }
        BlockState aboveState = level.getBlockState(pos.above());
        return !aboveState.isSolid();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return !this.defaultBlockState().canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? Blocks.DIRT.defaultBlockState() : super.getStateForPlacement(ctx);
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state)
    {
        return true;
    }

    @NotNull
    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx)
    {
        return shape;
    }


    @Override
    public void randomTick(BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource rng)
    {
        if (!state.canSurvive(level, pos))
        {
            turnToDirt(null, state, level, pos);
            return;
        }

        int i = state.getValue(MOISTURE);
        if (!level.isRainingAt(pos.above()) && !isNearWater(level, pos))
        {
            if (i > 0)
            {
                level.setBlock(pos, state.setValue(MOISTURE, i - 1), 2);
            }
            else if (!shouldMaintainFarmland(level, pos))
            {
                turnToDirt( null, state, level, pos);
            }
        }
        else if (i < 7)
        {
            level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
        }

        final BlockState aboveState = level.getBlockState(pos.above());
        if (aboveState.getBlock() instanceof MinecoloniesCropBlock cropBlock && rng.nextInt(25) == 0)
        {
            // todo balance randomness after evaluating crop yield.
            cropBlock.attemptGrow(aboveState, level, pos.above());
            BoneMealItem.addGrowthParticles(level, pos, 1);
        }
    }

    @Override
    public void fallOn(Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Entity entity, float light)
    {
        if (!level.isClientSide && CommonHooks.onFarmlandTrample(level, pos, Blocks.DIRT.defaultBlockState(), light, entity))
        {
            turnToDirt(entity, state, level, pos);
        }

        super.fallOn(level, state, pos, entity, light);
    }

    public static void turnToDirt(@Nullable Entity p_270981_, BlockState p_270402_, Level p_270568_, BlockPos p_270551_)
    {
        BlockState blockstate = pushEntitiesUp(p_270402_, Blocks.DIRT.defaultBlockState(), p_270568_, p_270551_);
        p_270568_.setBlockAndUpdate(p_270551_, blockstate);
        p_270568_.gameEvent(GameEvent.BLOCK_CHANGE, p_270551_, Context.of(p_270981_, blockstate));
    }

    private static boolean shouldMaintainFarmland(BlockGetter p_279219_, BlockPos p_279209_)
    {
        BlockState plant = p_279219_.getBlockState(p_279209_.above());
        BlockState state = p_279219_.getBlockState(p_279209_);
        return plant.getBlock() instanceof SpecialPlantable && state.canSustainPlant(p_279219_, p_279209_, Direction.UP, plant.getBlock().defaultBlockState()).isTrue();
    }

    private static boolean isNearWater(LevelReader level, BlockPos thisPos)
    {
        BlockState state = level.getBlockState(thisPos);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int x = thisPos.getX() -4; x <= thisPos.getX() + 4; x++)
        {
            for (int z = thisPos.getZ() -4; z <= thisPos.getZ() + 4; z++)
            {
                for (int y = thisPos.getY() - 1; y <= thisPos.getY(); y++)
                {
                    blockPos.set(x,y,z);
                    if (state.canBeHydrated(level, thisPos, level.getFluidState(blockPos), blockPos))
                    {
                        return true;
                    }
                }
            }
        }

        return FarmlandWaterManager.hasBlockWaterTicket(level, thisPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> blockStateBuilder)
    {
        blockStateBuilder.add(MOISTURE, WATERLOGGED);
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return blockId;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
