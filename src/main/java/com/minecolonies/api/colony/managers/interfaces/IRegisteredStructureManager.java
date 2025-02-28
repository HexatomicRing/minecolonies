package com.minecolonies.api.colony.managers.interfaces;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IMysticalSite;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.fields.IField;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Interface for the managers for registered structures.
 * Buildings, Fields, Decorations, etc.
 */
public interface IRegisteredStructureManager
{
    /**
     * Read the buildings from NBT.
     *
     * @param compound the compound.
     */
    void read(@NotNull final HolderLookup.Provider provider, @NotNull final CompoundTag compound);

    /**
     * Write the buildings to NBT.
     *
     * @param compound the compound.
     */
    void write(@NotNull final HolderLookup.Provider provider, @NotNull final CompoundTag compound);

    /**
     * Clear the isDirty of the buildings.
     */
    void clearDirty();

    /**
     * Send packets of the buildings to the subscribers.
     *
     * @param closeSubscribers the old subs.
     * @param newSubscribers   new subs.
     */
    void sendPackets(Set<ServerPlayer> closeSubscribers, final Set<ServerPlayer> newSubscribers);

    /**
     * Tick the buildings on colony tick.
     *
     * @param colony the event.
     */
    void onColonyTick(IColony colony);

    /**
     * Clean up the buildings.
     *
     * @param colony at the worldTick event.
     */
    void cleanUpBuildings(final IColony colony);

    /**
     * Get a certain building.
     *
     * @param pos the id of the building.
     * @return the building.
     */
    IBuilding getBuilding(BlockPos pos);

    /**
     * Get the leisure site positions.
     *
     * @return the list.
     */
    List<BlockPos> getLeisureSites();

    /**
     * Get the first building matching the conditions.
     *
     * @param predicate the predicate matching the building.
     * @return the building or null.
     */
    @Nullable
    IBuilding getFirstBuildingMatching(final Predicate<IBuilding> predicate);

    /**
     * Register a new leisure site.
     *
     * @param pos the position of it.
     */
    void addLeisureSite(BlockPos pos);

    /**
     * Remove a leisure site.
     *
     * @param pos the position of it.
     */
    void removeLeisureSite(BlockPos pos);

    /**
     * Get the closest warehouse relative to a position.
     *
     * @param pos the position,.
     * @return the closest warehouse.
     */
    @Nullable
    IWareHouse getClosestWarehouseInColony(BlockPos pos);

    /**
     * Returns a map with all buildings within the colony. Key is ID (Coordinates), value is building object.
     *
     * @return Map with ID (coordinates) as key, and buildings as value.
     */
    @NotNull
    Map<BlockPos, IBuilding> getBuildings();

    /**
     * Get the townhall from the colony.
     *
     * @return the townhall building.
     */
    ITownHall getTownHall();

    /**
     * Get the maximum level among built mystical sites
     *
     * @return the max level among all mystical sites or zero if no mystical site built
     */
    int getMysticalSiteMaxBuildingLevel();

    /**
     * Check if the colony has a placed warehouse.
     *
     * @return true if so.
     */
    boolean hasWarehouse();

    /**
     * Check if the colony has a placed mystical site.
     *
     * @return true if so.
     */
    boolean hasMysticalSite();

    /**
     * Check if the colony has a placed townhall.
     *
     * @return true if so.
     */
    boolean hasTownHall();

    /**
     * Get building in Colony by ID. The building will be casted to the provided type.
     *
     * @param buildingId ID (coordinates) of the building to get.
     * @param type       Type of building.
     * @param <B>        Building class.
     * @return the building with the specified id.
     */
    @Nullable <B extends IBuilding> B getBuilding(final BlockPos buildingId, @NotNull final Class<B> type);

    /**
     * Remove a IBuilding from the Colony (when it is destroyed).
     *
     * @param subscribers the subscribers of the colony to message.
     * @param building    IBuilding to remove.
     */
    void removeBuilding(@NotNull final IBuilding building, final Set<ServerPlayer> subscribers);

    /**
     * Marks building data dirty.
     */
    void markBuildingsDirty();

    /**
     * Marks fields data dirty.
     */
    void markFieldsDirty();

    /**
     * Creates a building from a tile entity and adds it to the colony.
     *
     * @param tileEntity Tile entity to build a building from.
     * @param world      the world to add it to.
     * @return IBuilding that was created and added.
     */
    @Nullable
    IBuilding addNewBuilding(@NotNull final AbstractTileEntityColonyBuilding tileEntity, final Level world);

    /**
     * Calculate a good cook for a certain citizen.
     *
     * @param citizen  the citizen.
     * @param building the type of building.
     * @return the Position of it.
     */
    BlockPos getBestBuilding(final AbstractEntityCitizen citizen, final Class<? extends IBuilding> building);

    /**
     * Calculate a good building for a certain pos.
     *
     * @param pos      the pos.
     * @param building the building class type.
     * @return the Position of it.
     */
    BlockPos getBestBuilding(final BlockPos pos, final Class<? extends IBuilding> building);

    /**
     * Returns a random building in the colony, matching the filter predicate.
     *
     * @param filterPredicate the filter to apply.
     * @return the random building. Returns null if no building matching the predicate was found.
     */
    BlockPos getRandomBuilding(Predicate<IBuilding> filterPredicate);

    /**
     * Finds whether there is a guard building close to the given building
     *
     * @param building the building to check for.
     * @return false if no guard tower close, true in other cases
     */
    boolean hasGuardBuildingNear(IBuilding building);

    /**
     * Event once a guard building changed at a certain level.
     *
     * @param guardBuilding the guard building.
     * @param newLevel      the level of it.
     */
    void guardBuildingChangedAt(IBuilding guardBuilding, int newLevel);

    /**
     * Set the townhall building.
     *
     * @param building the building to set.
     */
    void setTownHall(@Nullable final ITownHall building);

    /**
     * Removes a warehouse from the BuildingManager
     *
     * @param wareHouse the warehouse to remove.
     */
    void removeWareHouse(final IWareHouse wareHouse);

    /**
     * Get a list of the warehouses in this colony.
     *
     * @return the warehouse.
     */
    List<IWareHouse> getWareHouses();

    /**
     * Removes a warehouse from the BuildingManager
     *
     * @param mysticalSite the warehouse to remove.
     */
    void removeMysticalSite(final IMysticalSite mysticalSite);

    /**
     * Get a list of the mystical sites in this colony.
     *
     * @return the list of mistical sites.
     */
    List<IMysticalSite> getMysticalSites();

    /**
     * Checks whether we're allowed to place the block for a new building
     *
     * @param block  Block to check
     * @param pos    position
     * @param player the player trying to place
     * @return true if placement allowed
     */
    boolean canPlaceAt(Block block, BlockPos pos, Player player);

    /**
     * Is this chunk claimed by enough buildings to keep it loaded.
     *
     * @param chunk the chunk to check
     * @return true if within.
     */
    boolean keepChunkColonyLoaded(final LevelChunk chunk);

    /**
     * Get a house with a spare bed.
     *
     * @return the house or null.
     */
    IBuilding getHouseWithSpareBed();

    /**
     * Performed when a building of this colony finished his upgrade state.
     *
     * @param building The upgraded building.
     * @param level    The new level.
     */
    void onBuildingUpgradeComplete(@Nullable IBuilding building, int level);

    /**
     * Get a random leisure site to go to.
     *
     * @return the position of it.
     */
    BlockPos getRandomLeisureSite();

    /**
     * Get all the fields
     *
     * @param matcher the field matcher predicate.
     * @return an unmodifiable collection of all fields.
     */
    @NotNull List<IField> getFields(Predicate<IField> matcher);

    /**
     * Get a specific field on the given location.
     *
     * @param matcher the field matcher predicate.
     * @return the field, if any.
     */
    Optional<IField> getField(Predicate<IField> matcher);

    /**
     * Add a new field to the building manager.
     * If an identical field already exists, this field won't be added.
     *
     * @param field the new field to add.
     * @return true if the field was added.
     */
    boolean addField(IField field);

    /**
     * Remove a field from the field collection.
     *
     * @param matcher the field matcher predicate.
     */
    void removeField(Predicate<IField> matcher);
}