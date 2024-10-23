package com.minecolonies.core.entity.ai.workers.service;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.requestable.Food;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.modules.RestaurantMenuModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIUsesFurnace;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.MovementHandler;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.ItemStackUtils.CAN_EAT;
import static com.minecolonies.api.util.constant.CitizenConstants.AVERAGE_SATURATION;
import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.StatisticsConstants.FOOD_SERVED;
import static com.minecolonies.api.util.constant.TranslationConstants.FURNACE_USER_NO_FOOD;
import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_INFO_CITIZEN_COOK_SERVE_PLAYER;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.RESTAURANT_MENU;

/**
 * Cook AI class.
 */
public class EntityAIWorkCook extends AbstractEntityAIUsesFurnace<JobCook, BuildingCook>
{
    /**
     * The amount of food which should be served to the worker.
     */
    public static final int SATURATION_TO_SERVE = 16;

    /**
     * Delay between each serving.
     */
    private static final int SERVE_DELAY = 30;

    /**
     * Level at which the cook should give some food to the player.
     */
    private static final int LEVEL_TO_FEED_PLAYER = 10;

    /**
     * The citizen the worker is currently trying to serve.
     */
    private final Queue<AbstractEntityCitizen> citizenToServe = new ArrayDeque<>();

    /**
     * The citizen the worker is currently trying to serve.
     */
    private final Queue<Player> playerToServe = new ArrayDeque<>();

    /**
     * Cooking icon
     */
    private final static VisibleCitizenStatus COOK =
      new VisibleCitizenStatus(new ResourceLocation(Constants.MOD_ID, "textures/icons/work/cook.png"), "com.minecolonies.gui.visiblestatus.cook");

    /**
     * The list of items needed for the assistant
     */
    private Set<ItemStorage> reservedItemCache = new HashSet<>();

    /**
     * Constructor for the Cook. Defines the tasks the cook executes.
     *
     * @param job a cook job to use.
     */
    public EntityAIWorkCook(@NotNull final JobCook job)
    {
        super(job);
        super.registerTargets(
          new AITarget(COOK_SERVE_FOOD_TO_CITIZEN, this::serveFoodToCitizen, SERVE_DELAY),
          new AITarget(COOK_SERVE_FOOD_TO_PLAYER, this::serveFoodToPlayer, SERVE_DELAY)
        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    public Class<BuildingCook> getExpectedBuildingClass()
    {
        return BuildingCook.class;
    }

    /**
     * Very simple action, cook straightly extract it from the furnace.
     *
     * @param furnace the furnace to retrieve from.
     */
    @Override
    protected void extractFromFurnace(final FurnaceBlockEntity furnace)
    {
        InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
          new InvWrapper(furnace), RESULT_SLOT,
          worker.getInventoryCitizen());
        worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        this.incrementActionsDoneAndDecSaturation();
    }

    @Override
    public IAIState startWorking()
    {
        reservedItemCache.clear();
        return super.startWorking();
    }

    @Override
    protected boolean isSmeltable(final ItemStack stack)
    {
        //Only return true if the item isn't queued for a recipe.
        return ItemStackUtils.ISCOOKABLE.test(stack) && building.getModule(RESTAURANT_MENU).getMenu().contains(new ItemStorage(MinecoloniesAPIProxy.getInstance().getFurnaceRecipes().getSmeltingResult(stack)));
    }

    @Override
    protected boolean reachedMaxToKeep()
    {
        if (super.reachedMaxToKeep())
        {
            return true;
        }
        final int buildingLimit = Math.max(1, building.getBuildingLevel() * building.getBuildingLevel()) * SLOT_PER_LINE;
        return InventoryUtils.getCountFromBuildingWithLimit(building,
          ItemStackUtils.CAN_EAT.and(stack -> FoodUtils.canEat(stack, building.getBuildingLevel() - 1)),
          stack -> stack.getMaxStackSize() * 6) > buildingLimit;
    }

    @Override
    public void requestSmeltable()
    {
        final RestaurantMenuModule menuModule = building.getModule(RESTAURANT_MENU);
        if (menuModule.getMenu().isEmpty() && worker.getCitizenData() != null)
        {
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(FURNACE_USER_NO_FOOD), ChatPriority.BLOCKING));
        }
    }

    /**
     * Serve food to citizen.
     * @return next IAIState
     */
    private IAIState serveFoodToCitizen()
    {
        if (citizenToServe.isEmpty())
        {
            return START_WORKING;
        }

        worker.getCitizenData().setVisibleStatus(COOK);

        if (!building.isInBuilding(citizenToServe.peek().blockPosition()))
        {
            worker.getNavigation().stop();
            playerToServe.poll();
            return getState();
        }

        if (walkToBlock(citizenToServe.peek().blockPosition()))
        {
            return getState();
        }

        final AbstractEntityCitizen citizen = citizenToServe.poll();
        final IItemHandler handler = citizen.getInventoryCitizen();
        final RestaurantMenuModule module = worker.getCitizenData().getWorkBuilding().getModule(RESTAURANT_MENU);
        final Predicate<ItemStack> canEatPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
        final ICitizenData citizenData = citizen.getCitizenData();

        if (InventoryUtils.isItemHandlerFull(handler))
        {
            for (int feedingAttempts = 0; feedingAttempts < 10; feedingAttempts++)
            {
                final int foodSlot = FoodUtils.getBestFoodForCitizen(worker.getInventoryCitizen(), citizenData, module.getMenu(), true);
                if (foodSlot != -1)
                {
                    final ItemStack stack = worker.getInventoryCitizen().extractItem(foodSlot, 1, false);
                    citizenData.increaseSaturation(FoodUtils.getFoodValue(stack, worker));
                    worker.getCitizenColonyHandler().getColony().getStatisticsManager().increment(FOOD_SERVED, worker.getCitizenColonyHandler().getColony().getDay());
                }
                else
                {
                    break;
                }

                if (citizenData.getSaturation() >= CitizenConstants.FULL_SATURATION)
                {
                    break;
                }
            }

            return getState();
        }
        else if (InventoryUtils.hasItemInItemHandler(handler, canEatPredicate))
        {
            return getState();
        }

        final int foodSlot = FoodUtils.getBestFoodForCitizen(worker.getInventoryCitizen(), citizenData, module.getMenu(), true);
        if (foodSlot == -1)
        {
            if (InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), canEatPredicate) <= 0)
            {
                citizenToServe.clear();
                return START_WORKING;
            }
            return getState();
        }

        final int countInSlot = worker.getInventoryCitizen().getStackInSlot(foodSlot).getCount();
        final int transferCount = Math.min(countInSlot, building.getBuildingLevel());
        if (InventoryUtils.transferXOfItemStackIntoNextFreeSlotInItemHandler(worker.getInventoryCitizen(), foodSlot, transferCount, citizenData.getInventory()))
        {
            worker.getCitizenColonyHandler().getColony().getStatisticsManager().incrementBy(FOOD_SERVED, transferCount, worker.getCitizenColonyHandler().getColony().getDay());
            worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
            this.incrementActionsDoneAndDecSaturation();
        }

        return getState();
    }

    /**
     * Serve food to player.
     * @return next IAIState
     */
    private IAIState serveFoodToPlayer()
    {
        if (playerToServe.isEmpty())
        {
            return START_WORKING;
        }

        worker.getCitizenData().setVisibleStatus(COOK);
        if (!building.isInBuilding(playerToServe.peek().blockPosition()))
        {
            worker.getNavigation().stop();
            playerToServe.poll();
            return START_WORKING;
        }

        if (walkToBlock(playerToServe.peek().blockPosition()))
        {
            return getState();
        }

        final Player player = playerToServe.poll();
        final IItemHandler handler = new InvWrapper(player.getInventory());
        final RestaurantMenuModule module = worker.getCitizenData().getWorkBuilding().getModule(RESTAURANT_MENU);
        final Predicate<ItemStack> canEatPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
        if (InventoryUtils.isItemHandlerFull(handler))
        {
            return getState();
        }
        else if (InventoryUtils.hasItemInItemHandler(handler, canEatPredicate))
        {
            return getState();
        }

        final int count = InventoryUtils.transferFoodUpToSaturation(worker, handler, building.getBuildingLevel() * SATURATION_TO_SERVE, canEatPredicate);
        if (count <= 0)
        {
            playerToServe.clear();
            return START_WORKING;
        }
        worker.getCitizenColonyHandler().getColony().getStatisticsManager().incrementBy(FOOD_SERVED, count, worker.getCitizenColonyHandler().getColony().getDay());
        MessageUtils.format(MESSAGE_INFO_CITIZEN_COOK_SERVE_PLAYER, worker.getName().getString()).sendTo(player);

        worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        this.incrementActionsDoneAndDecSaturation();
        return START_WORKING;
    }

    /**
     * Check if the entity to serve can eat the given stack
     *
     * @param stack   the stack to check
     * @param citizen the citizen to check for.
     * @return true if the stack can be eaten
     */
    private boolean canEat(final ItemStack stack, final AbstractEntityCitizen citizen)
    {
        final RestaurantMenuModule module = worker.getCitizenData().getWorkBuilding().getModule(RESTAURANT_MENU);
        if (!module.getMenu().contains(new ItemStorage(stack)))
        {
            return false;
        }
        if (citizen != null)
        {
            final IBuilding building = citizen.getCitizenData().getHomeBuilding();
            if (building != null)
            {
                return building.canEat(stack);
            }
        }
        return true;
    }

    //todo adjust happiness to care about set diversity
    //todo adjust happiness messaging around this

    // Mininmal Happiness:
    //1: 1 Different types of food, e.g. baked potato
    //2: 2 Types of food baked potato + mushroom soup
    //3: 3 Types food baked potato + mushroom soup + 1 Minecolonies food
    //4: 4 Types food baked potato + mushroom soup + 2 Minecolonies food
    //5: 5 Types food baked potato + mushroom soup + 3 Minecolonies food
    //
    //Optimal Happiness:
    //1: 2 Different types of food, e.g. baked potato
    //2: 4 Types of food baked potato + mushroom soup
    //3: 6 Types food 4x whatever + 2 Minecolonies food
    //4: 8 Types food 4x whatever + 4 Minecolonies food
    //5: 10 Types food 4x whatever + 6 Minecolonies food

    //todo temporary happiness bonus after eating a level 3 tier food.

    //todo citizen when eating has to do more tests before eating any food! (If it would reach complaint status, then don't)

    /**
     * Checks if the cook has anything important to do before going to the default furnace user jobs. First calculate the building range if not cached yet. Then check for citizens
     * around the building. If no citizen around switch to default jobs. If citizens around check if food in inventory, if not, switch to gather job. If food in inventory switch to
     * serve job.
     *
     * @return the next IAIState to transfer to.
     */
    @Override
    protected IAIState checkForImportantJobs()
    {
        this.reservedItemCache.clear(); //Clear the cache of current pending work

        citizenToServe.clear();
        final List<? extends Player> playerList = WorldUtil.getEntitiesWithinBuilding(world, Player.class,
          building, player -> player != null
                                && player.getFoodData().getFoodLevel() < LEVEL_TO_FEED_PLAYER
                                && building.getColony().getPermissions().hasPermission(player, Action.MANAGE_HUTS)
        );

        playerToServe.addAll(playerList);
        final RestaurantMenuModule module = worker.getCitizenData().getWorkBuilding().getModule(RESTAURANT_MENU);

        boolean hasFoodInBuilding = false;
        for (final EntityCitizen citizen : WorldUtil.getEntitiesWithinBuilding(world, EntityCitizen.class, building, null))
        {
            if (citizen.getCitizenJobHandler().getColonyJob() instanceof JobCook
                  || !shouldBeFed(citizen)
                  || InventoryUtils.hasItemInItemHandler(citizen.getItemHandlerCitizen(), stack -> canEat(stack, citizen)))
            {
                continue;
            }

            if (FoodUtils.getBestFoodForCitizen(worker.getInventoryCitizen(), citizen.getCitizenData(), module.getMenu(), true) >= 0)
            {
                citizenToServe.add(citizen);
            }
            else
            {
                if (checkForFood(citizen.getCitizenData(), module.getMenu()))
                {
                    hasFoodInBuilding = true;
                }
            }
        }

        if (!citizenToServe.isEmpty())
        {
            return COOK_SERVE_FOOD_TO_CITIZEN;
        }

        if (hasFoodInBuilding)
        {
            return GATHERING_REQUIRED_MATERIALS;
        }

        if (!playerToServe.isEmpty())
        {
            final Predicate<ItemStack> foodPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
            if (!InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), foodPredicate))
            {
                if (InventoryUtils.hasItemInProvider(building, foodPredicate))
                {
                    needsCurrently = new Tuple<>(foodPredicate, STACKSIZE);
                    return GATHERING_REQUIRED_MATERIALS;
                }
            }
            return COOK_SERVE_FOOD_TO_PLAYER;
        }

        return START_WORKING;
    }

    /**
     * Get the best food for a given citizen from a given inventory and return the index where it is.
     * @param citizenData the citizen data the food is for.
     * @param menu the menu that has to be matched.
     * @return the matching inv slot, or -1.
     */
    public boolean checkForFood(final ICitizenData citizenData, final Set<ItemStorage> menu)
    {
        // Smaller score is better.
        int bestScore = Integer.MAX_VALUE;
        ItemStorage bestStorage = null;

        final Level world = building.getColony().getWorld();
        final int homeBuildingLevel = citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevel();

        final ICitizenData.CitizenFoodStats foodStats = citizenData.getFoodHappinessStats();
        final int diversityRequirement = FoodUtils.getMinFoodDiversityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevel());
        final int qualityRequirement = FoodUtils.getMinFoodQualityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevel());

        final boolean criticalDiversity = foodStats.diversity() <= diversityRequirement;
        final boolean criticalQuality = foodStats.quality() <= qualityRequirement;

        containerloop: for (final BlockPos pos : building.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack rackEntity)
                {
                    for (final ItemStorage storage : rackEntity.getAllContent().keySet())
                    {
                        if (menu.contains(storage) && FoodUtils.canEat(storage.getItemStack(), homeBuildingLevel))
                        {
                            final boolean isMinecolfood = storage.getItem() instanceof IMinecoloniesFoodItem;
                            final int localScore = citizenData.checkLastEaten(storage.getItem());

                            // If this is great food and we're at critical levels, go with it!
                           if ((localScore < 0 && isMinecolfood) && (criticalDiversity || criticalQuality))
                           {
                               bestStorage = storage;
                               break containerloop;
                           }

                           if (localScore > bestScore)
                           {
                               continue;
                           }

                           if (isMinecolfood && !criticalQuality && MathUtils.RANDOM.nextInt(((IMinecoloniesFoodItem) storage.getItem()).getTier() + 2 - homeBuildingLevel) <= 0)
                           {
                               bestScore = localScore;
                               bestStorage = storage;
                               continue;
                           }

                            bestScore = localScore * (isMinecolfood ? 2 : 1);
                            bestStorage = storage;

                            // If the quality and diversity requirement would be fulfilled, already go ahead with this food. Don't need to check others.
                            if ((localScore < 0 && isMinecolfood)
                                  || (localScore < 0 && foodStats.quality() > qualityRequirement * 2)
                                  || (isMinecolfood && foodStats.diversity() > diversityRequirement * 2))
                            {
                                break containerloop;
                            }
                        }
                    }
                }
            }
        }

        if (bestStorage == null)
        {
            return false;
        }

        final ItemStorage resultStorage = new ItemStorage(bestStorage.getItemStack().copy());
        needsCurrently = new Tuple<>(stack -> new ItemStorage(stack).equals(resultStorage), STACKSIZE);
        return true;
    }

    /**
     * Check if the citizen can be fed.
     *
     * @return true if so.
     */
    private boolean shouldBeFed(AbstractEntityCitizen citizen)
    {
        return citizen.getCitizenData() != null
                 && !citizen.getCitizenData().isWorking()
                 && citizen.getCitizenData().getSaturation() <= AVERAGE_SATURATION
                 && !citizen.getCitizenData().justAte();
    }

    @Override
    protected int getActionsDoneUntilDumping()
    {
        return 1;
    }

    @Override
    protected IRequestable getSmeltAbleClass()
    {
        return new Food(STACKSIZE, building.getBuildingLevel());
    }
}
