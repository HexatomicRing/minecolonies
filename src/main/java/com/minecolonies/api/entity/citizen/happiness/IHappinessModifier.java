package com.minecolonies.api.entity.citizen.happiness;

import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface describing possible happiness factors.
 */
public interface IHappinessModifier
{
    /**
     * Get the unique happiness id.
     *
     * @return the string id.
     */
    String getId();

    /**
     * Get the factor of the happiness. value between 0 and 1 if negative. value above 1 if positive.
     * @param data the citizen the factor is for.
     * @return the value of the factor.
     */
    double getFactor(@Nullable final ICitizenData data);

    /**
     * Get the weight of the happiness.
     *
     * @return the weight.
     */
    double getWeight();

    /**
     * Read the modifier from nbt.
     *
     * @param compoundNBT the compound to read it from.
     * @param persist     whether we're reading from persisted data or from networking.
     */
    void read(@NotNull final HolderLookup.Provider provider, final CompoundTag compoundNBT, final boolean persist);

    /**
     * Write it to NBT.
     *
     * @param compoundNBT the compound to write it to.
     * @param persist     whether we're reading from persisted data or from networking.
     */
    void write(@NotNull final HolderLookup.Provider provider, final CompoundTag compoundNBT, final boolean persist);
}
