package com.minecolonies.api.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loot condition that checks whether the provided building has the correct level.
 */
public class BuildingLevel implements LootItemCondition
{
    private final Integer minBuildingLevel;
    private final Integer maxBuildingLevel;

    private BuildingLevel(@Nullable final Integer minBuildingLevel, @Nullable final Integer maxBuildingLevel)
    {
        this.minBuildingLevel = minBuildingLevel;
        this.maxBuildingLevel = maxBuildingLevel;
    }

    /**
     * Creates a loot condition that is true when a building is at least of the provided level or above.
     *
     * @param minimum the minimum building level.
     * @return the condition
     */
    public static Builder minimum(final int minimum)
    {
        return () -> new BuildingLevel(minimum, null);
    }

    /**
     * Creates a loot condition that is true when a building is at most of the provided level or below.
     *
     * @param maximum the maximum building level.
     * @return the condition
     */
    public static Builder maximum(final int maximum)
    {
        return () -> new BuildingLevel(null, maximum);
    }

    /**
     * Creates a loot condition that is true when a building is between the minimum and maximum levels, inclusive.
     *
     * @param minimum the minimum building level.
     * @param maximum the maximum building level.
     * @return the condition
     */
    public static Builder between(final int minimum, final int maximum)
    {
        return () -> new BuildingLevel(minimum, maximum);
    }

    @NotNull
    @Override
    public LootItemConditionType getType()
    {
        return ModLootConditions.researchUnlocked.get();
    }

    @Override
    public boolean test(@NotNull final LootContext lootContext)
    {
        final int buildingLevel = lootContext.getParam(ModLootConditions.BUILDING_LEVEL);
        return (minBuildingLevel == null || buildingLevel >= minBuildingLevel) && (maxBuildingLevel == null || buildingLevel <= maxBuildingLevel);
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<BuildingLevel>
    {
        @Override
        public void serialize(@NotNull final JsonObject json, @NotNull final BuildingLevel condition, @NotNull final JsonSerializationContext context)
        {
            if (condition.minBuildingLevel != null)
            {
                json.addProperty("min", condition.minBuildingLevel);
            }

            if (condition.maxBuildingLevel != null)
            {
                json.addProperty("max", condition.maxBuildingLevel);
            }
        }

        @NotNull
        @Override
        public BuildingLevel deserialize(@NotNull final JsonObject json, @NotNull final JsonDeserializationContext context)
        {
            final Integer minBuildingLevel = json.has("min") ? GsonHelper.getAsInt(json, "min") : null;
            final Integer maxBuildingLevel = json.has("max") ? GsonHelper.getAsInt(json, "max") : null;
            return new BuildingLevel(minBuildingLevel, maxBuildingLevel);
        }
    }
}
