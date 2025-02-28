package com.minecolonies.api.colony.interactionhandling.registry;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The data manager of the interaction handler.
 */
public interface IInteractionResponseHandlerDataManager
{

    static IInteractionResponseHandlerDataManager getInstance()
    {
        return IMinecoloniesAPI.getInstance().getInteractionResponseHandlerDataManager();
    }

    /**
     * Create an interactionResponseHandler from saved CompoundTag data.
     *
     * @param citizen  The citizen that owns the interaction response handler..
     * @param compound The CompoundTag containing the saved interaction data.
     * @return New InteractionResponseHandler created from the data, or null.
     */
    @Nullable
    IInteractionResponseHandler createFrom(@NotNull final HolderLookup.Provider provider, @NotNull ICitizen citizen, @NotNull CompoundTag compound);
}
