package com.minecolonies.core.colony.fields;

import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IBuildingModuleView;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.fields.IField;
import com.minecolonies.api.colony.fields.modules.IFieldModule;
import com.minecolonies.api.colony.fields.registry.FieldRegistries;
import com.minecolonies.api.colony.modules.ModuleContainerUtils;
import com.minecolonies.api.util.BlockPosUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_OWNER;

/**
 * Abstract implementation for field instances.
 * Contains some basic mandatory logic for fields.
 */
public abstract class AbstractField implements IField
{
    /**
     * Set of field modules this field has.
     */
    private final List<IFieldModule> modules = new ArrayList<>();

    /**
     * The type of the field.
     */
    private final FieldRegistries.FieldEntry fieldType;

    /**
     * The position of the field.
     */
    private final BlockPos position;

    /**
     * Building id of the building owning the field.
     */
    @Nullable
    private BlockPos buildingId = null;

    /**
     * Constructor used in NBT deserialization.
     *
     * @param fieldType the type of field.
     * @param position  the position of the field.
     */
    protected AbstractField(final @NotNull FieldRegistries.FieldEntry fieldType, final @NotNull BlockPos position)
    {
        this.fieldType = fieldType;
        this.position = position;
    }

    @Override
    public boolean hasModule(final Class<? extends IFieldModule> clazz)
    {
        return ModuleContainerUtils.hasModule(modules, clazz);
    }

    @Override
    public boolean hasModule(final BuildingEntry.ModuleProducer producer)
    {
        throw new RuntimeException("Not implemented");
    }

    @NotNull
    @Override
    public <T extends IFieldModule> T getFirstModuleOccurance(final Class<T> clazz)
    {
        return ModuleContainerUtils.getFirstModuleOccurance(modules,
          clazz,
          "The module of class: " + clazz.toString() + "should never be null! Field:" + getFieldType().getRegistryName() + " pos:" + getPosition());
    }

    @Override
    public <M extends IBuildingModule, V extends IBuildingModuleView> @NotNull M getModule(
      final BuildingEntry.ModuleProducer<M, V> producer)
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IBuildingModule getModule(final int id)
    {
        throw new RuntimeException("Not implemented");
    }

    @NotNull
    @Override
    public <T extends IFieldModule> List<T> getModulesByType(final Class<T> clazz)
    {
        return ModuleContainerUtils.getModules(modules, clazz);
    }

    @NotNull
    @Override
    public <T extends IFieldModule> T getModuleMatching(final Class<T> clazz, final Predicate<? super T> modulePredicate)
    {
        return ModuleContainerUtils.getModuleMatching(modules,
          clazz,
          modulePredicate,
          "no matching module for Field:" + getFieldType().getRegistryName() + " pos:" + getPosition().toShortString());
    }

    @Override
    public void registerModule(@NotNull final IFieldModule module)
    {
        this.modules.add(module);
    }

    @Override
    public final @NotNull FieldRegistries.FieldEntry getFieldType()
    {
        return fieldType;
    }

    @Override
    @NotNull
    public final BlockPos getPosition()
    {
        return position;
    }

    @Override
    @Nullable
    public final BlockPos getBuildingId()
    {
        return buildingId;
    }

    @Override
    public final void setBuilding(final BlockPos buildingId)
    {
        this.buildingId = buildingId;
    }

    @Override
    public final void resetOwningBuilding()
    {
        buildingId = null;
    }

    @Override
    public final boolean isTaken()
    {
        return buildingId != null;
    }

    @Override
    public final int getSqDistance(final IBuildingView building)
    {
        return (int) Math.sqrt(BlockPosUtil.getDistanceSquared(position, building.getPosition()));
    }

    @Override
    public @NotNull CompoundTag serializeNBT(@NotNull final HolderLookup.Provider provider)
    {
        CompoundTag compound = new CompoundTag();
        if (buildingId != null)
        {
            BlockPosUtil.write(compound, TAG_OWNER, buildingId);
        }
        return compound;
    }

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final @NotNull CompoundTag compound)
    {
        if (compound.contains(TAG_OWNER))
        {
            buildingId = BlockPosUtil.read(compound, TAG_OWNER);
        }
    }

    @Override
    public void serialize(final @NotNull RegistryFriendlyByteBuf buf)
    {
        buf.writeBoolean(buildingId != null);
        if (buildingId != null)
        {
            buf.writeBlockPos(buildingId);
        }
    }

    @Override
    public void deserialize(final @NotNull RegistryFriendlyByteBuf buf)
    {
        if (buf.readBoolean())
        {
            buildingId = buf.readBlockPos();
        }
    }

    @Override
    public int hashCode()
    {
        int result = position.hashCode();
        result = 31 * result + fieldType.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final AbstractField that = (AbstractField) o;

        if (!position.equals(that.position))
        {
            return false;
        }
        return fieldType.equals(that.fieldType);
    }
}
