package com.minecolonies.api.entity.visitor;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for extra visitor data.
 */
public interface IVisitorExtraData<S> extends INBTSerializable<CompoundTag>
{
    /**
     * The unique key which the data gets stored under.
     *
     * @return the key.
     */
    @NotNull
    String getKey();

    /**
     * Get the visitor data value.
     *
     * @return the value.
     */
    @NotNull
    S getValue();

    /**
     * Set the new value for this extra data key.
     *
     * @param value the new value.
     */
    void setValue(S value);

    /**
     * Get the default value in case no value was explicitly set yet.
     *
     * @return the value.
     */
    @NotNull
    S getDefaultValue();
}