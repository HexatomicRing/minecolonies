package com.minecolonies.api.colony.fields;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.fields.modules.IFieldModule;
import com.minecolonies.api.colony.fields.registry.FieldRegistries;
import com.minecolonies.api.colony.modules.IModuleContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for field instances.
 */
public interface IField extends IModuleContainer<IFieldModule>
{
    /**
     * Return the field type for this field.
     *
     * @return the field registry entry.
     */
    @NotNull FieldRegistries.FieldEntry getFieldType();

    /**
     * Gets the position of the field.
     *
     * @return central location of the field.
     */
    @NotNull BlockPos getPosition();

    /**
     * Getter for the owning building of the field.
     *
     * @return the id or null.
     */
    @Nullable BlockPos getBuildingId();

    /**
     * Sets the owning building of the field.
     *
     * @param buildingId id of the building.
     */
    void setBuilding(final BlockPos buildingId);

    /**
     * Resets the ownership of the field.
     */
    void resetOwningBuilding();

    /**
     * Has the field been taken.
     *
     * @return true if the field is not free to use, false after releasing it.
     */
    boolean isTaken();

    /**
     * Get the distance to a building.
     *
     * @param building the building to get the distance to.
     * @return the distance
     */
    int getSqDistance(IBuildingView building);

    /**
     * Stores the NBT data of the field.
     */
    @NotNull CompoundTag serializeNBT(@NotNull final HolderLookup.Provider provider);

    /**
     * Reconstruct the field from the given NBT data.
     *
     * @param compound the compound to read from.
     */
    void deserializeNBT(@NotNull final HolderLookup.Provider provider, @NotNull CompoundTag compound);

    /**
     * Serialize a field to a buffer.
     *
     * @param buf the buffer to write the field data to.
     */
    void serialize(@NotNull RegistryFriendlyByteBuf buf);

    /**
     * Deserialize a field from a buffer.
     *
     * @param buf the buffer to read the field data from.
     */
    void deserialize(@NotNull RegistryFriendlyByteBuf buf);

    /**
     * Condition to check whether this field instance is currently properly placed down.
     *
     * @param colony the colony this field is in.
     * @return true if the field is correctly placed at the current position.
     */
    boolean isValidPlacement(IColony colony);

    /**
     * Hashcode implementation for this field.
     */
    int hashCode();

    /**
     * Equals implementation for this field.
     */
    boolean equals(Object other);
}
