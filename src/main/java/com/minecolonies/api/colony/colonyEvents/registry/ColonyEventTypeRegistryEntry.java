package com.minecolonies.api.colony.colonyEvents.registry;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.colonyEvents.IColonyEvent;
import com.minecolonies.api.util.Log;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * This is the colonies event registry entry class, used for registering any colony related events. Takes a function of colony, nbt to create the right event object.
 */
public class ColonyEventTypeRegistryEntry
{
    /**
     * Function for creating the event objects.
     */
    private final TriFunction<IColony, CompoundTag, HolderLookup.Provider, IColonyEvent> eventCreator;

    /**
     * The registry id.
     */
    private final ResourceLocation registryName;

    /**
     * Creates a new registry entry for the given function and registry name
     *
     * @param eventCreator the event creator.
     * @param registryID   the registry id.
     */
    public ColonyEventTypeRegistryEntry(@NotNull final TriFunction<IColony, CompoundTag, HolderLookup.Provider, IColonyEvent> eventCreator, @NotNull final ResourceLocation registryID)
    {
        if (registryID.getPath().isEmpty())
        {
            Log.getLogger().warn("Created empty registry empty for event, supply a name for it!");
        }

        this.eventCreator = eventCreator;
        this.registryName = registryID;
    }

    /**
     * Deserializes the event from nbt.
     * 
     * @param colony   the colony this event is part of.
     * @param compound the nbt to deserialize the event from.
     * @return the deserialized event.
     */
    public IColonyEvent deserializeEvent(@Nonnull final IColony colony, @NotNull final HolderLookup.Provider provider, @Nonnull final CompoundTag compound)
    {
        return eventCreator.apply(colony, compound, provider);
    }

    /**
     * Get the fitting registry name.
     * @return the name.
     */
    public ResourceLocation getRegistryName()
    {
        return registryName;
    }
}
