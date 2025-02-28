package com.minecolonies.core.colony.fields;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.fields.registry.FieldRegistries;
import com.minecolonies.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Field class implementation for the plantation
 */
public class FarmField extends AbstractField
{
    /**
     * The max width/length of a field.
     */
    private static final int MAX_RANGE = 5;

    private static final String TAG_SEED      = "seed";
    private static final String TAG_RADIUS    = "radius";
    private static final String TAG_MAX_RANGE = "maxRange";
    private static final String TAG_STAGE     = "stage";

    /**
     * The currently selected seed on the field, if any.
     */
    private ItemStack seed = ItemStack.EMPTY;

    /**
     * The size of the field in all four directions
     * in the same order as {@link Direction}:
     * S, W, N, E
     */
    private int[] radii = {MAX_RANGE, MAX_RANGE, MAX_RANGE, MAX_RANGE};

    /**
     * The maximum radius for this field.
     */
    private int maxRadius;

    /**
     * Has the field been planted
     */
    private Stage fieldStage = Stage.EMPTY;

    /**
     * Constructor used in NBT deserialization.
     *
     * @param fieldType the type of field.
     * @param position  the position of the field.
     */
    public FarmField(final FieldRegistries.FieldEntry fieldType, final BlockPos position)
    {
        super(fieldType, position);
        this.maxRadius = MAX_RANGE;
    }

    /**
     * Constructor to create new instances
     *
     * @param position the position it is placed in.
     */
    public static FarmField create(final BlockPos position)
    {
        return (FarmField) FieldRegistries.farmField.get().produceField(position);
    }

    @Override
    public boolean isValidPlacement(final IColony colony)
    {
        BlockState blockState = colony.getWorld().getBlockState(getPosition());
        return blockState.is(ModBlocks.blockScarecrow);
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull final HolderLookup.Provider provider)
    {
        CompoundTag compound = super.serializeNBT(provider);
        compound.put(TAG_SEED, seed.saveOptional(provider));
        compound.putIntArray(TAG_RADIUS, radii);
        compound.putInt(TAG_MAX_RANGE, maxRadius);
        compound.putString(TAG_STAGE, fieldStage.name());
        return compound;
    }

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final @NotNull CompoundTag compound)
    {
        super.deserializeNBT(provider, compound);
        setSeed(ItemStack.parseOptional(provider, compound.getCompound(TAG_SEED)));
        radii = compound.getIntArray(TAG_RADIUS);
        maxRadius = compound.getInt(TAG_MAX_RANGE);
        fieldStage = Stage.valueOf(compound.getString(TAG_STAGE));
    }

    @Override
    public void serialize(final @NotNull RegistryFriendlyByteBuf buf)
    {
        super.serialize(buf);
        Utils.serializeCodecMess(buf, getSeed());
        buf.writeVarIntArray(radii);
        buf.writeInt(maxRadius);
        buf.writeEnum(fieldStage);
    }

    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.deserialize(buf);
        setSeed(Utils.deserializeCodecMess(buf));
        radii = buf.readVarIntArray();
        maxRadius = buf.readInt();
        fieldStage = buf.readEnum(Stage.class);
    }

    /**
     * Get the current seed on the field.
     *
     * @return the current seed.
     */
    @NotNull
    public ItemStack getSeed()
    {
        seed.setCount(1);
        return seed;
    }

    /**
     * Updates the seed in the field.
     *
     * @param seed the new seed
     */
    public void setSeed(final ItemStack seed)
    {
        this.seed = seed.copy();
        this.seed.setCount(1);
    }

    /**
     * Move the field into the new state.
     */
    public void nextState()
    {
        if (getFieldStage().ordinal() + 1 >= Stage.values().length)
        {
            setFieldStage(Stage.values()[0]);
            return;
        }
        setFieldStage(Stage.values()[getFieldStage().ordinal() + 1]);
    }

    /**
     * Get the current stage the field is in.
     *
     * @return the stage of the field.
     */
    public Stage getFieldStage()
    {
        return this.fieldStage;
    }

    /**
     * Sets the current stage of the field.
     *
     * @param fieldStage the stage of the field.
     */
    public void setFieldStage(final Stage fieldStage)
    {
        this.fieldStage = fieldStage;
    }

    /**
     * Get the max range for this field.
     *
     * @return the maximum range.
     */
    public int getMaxRadius()
    {
        return maxRadius;
    }

    /**
     * @param direction the direction to get the range for
     * @return the radius
     */
    public int getRadius(Direction direction)
    {
        return radii[direction.get2DDataValue()];
    }

    /**
     * @param direction the direction for the radius
     * @param radius    the number of blocks from the scarecrow that the farmer will work with
     */
    public void setRadius(Direction direction, int radius)
    {
        this.radii[direction.get2DDataValue()] = Math.min(radius, maxRadius);
    }

    /**
     * Checks if a certain position is part of the field. Complies with the definition of field block.
     *
     * @param world    the world object.
     * @param position the position.
     * @return true if it is.
     */
    public boolean isNoPartOfField(@NotNull final Level world, @NotNull final BlockPos position)
    {
        return world.isEmptyBlock(position) || isValidDelimiter(world.getBlockState(position.above()).getBlock());
    }

    /**
     * Check if a block is a valid delimiter of the field.
     *
     * @param block the block to analyze.
     * @return true if so.
     */
    private static boolean isValidDelimiter(final Block block)
    {
        return block instanceof FenceBlock || block instanceof FenceGateBlock || block instanceof WallBlock;
    }

    /**
     * Describes the stage the field is in. Like if it has been hoed, planted or is empty.
     */
    public enum Stage
    {
        EMPTY,
        HOED,
        PLANTED
    }
}