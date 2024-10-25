package com.minecolonies.core.datalistener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.Network;
import com.minecolonies.core.network.messages.client.colony.GlobalDiseaseSyncMessage;
import com.minecolonies.core.util.RandomCollection;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Loads and listens to diseases data.
 */
public class DiseasesListener extends SimpleJsonResourceReloadListener
{
    /**
     * Gson instance
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Json constants
     */
    private static final String KEY_NAME   = "name";
    private static final String KEY_RARITY = "rarity";
    private static final String KEY_ITEMS  = "items";

    /**
     * The map of diseases.
     */
    private static RandomCollection<ResourceLocation, Disease> DISEASES = new RandomCollection<>();

    /**
     * A possible disease.
     *
     * @param id        the id of the disease.
     * @param name      the name of the disease.
     * @param rarity    the rarity of the disease.
     * @param cureItems the list of items needed to heal.
     */
    public record Disease(ResourceLocation id, Component name, int rarity, List<ItemStorage> cureItems)
    {
        /**
         * Get the cure string containing all items required for the cure.
         *
         * @return the cure string.
         */
        public Component getCureString()
        {
            final MutableComponent cureString = Component.literal("");
            for (int i = 0; i < cureItems.size(); i++)
            {
                final ItemStorage cureStack = cureItems.get(i);
                cureString.append(String.valueOf(cureStack.getItemStack().getCount())).append(" ").append(cureStack.getItemStack().getHoverName());
                if (i != cureItems.size() - 1)
                {
                    cureString.append(" + ");
                }
            }
            return cureString;
        }
    }

    /**
     * Default constructor.
     */
    public DiseasesListener()
    {
        super(GSON, "diseases");
    }

    /**
     * Sync to client.
     *
     * @param player to send it to.
     */
    public static void sendGlobalDiseasesPackets(final ServerPlayer player)
    {
        final FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        byteBuf.writeInt(DISEASES.getAll().size());
        for (final Disease disease : DISEASES.getAll())
        {
            byteBuf.writeResourceLocation(disease.id);
            byteBuf.writeComponent(disease.name);
            byteBuf.writeInt(disease.rarity);
            for (final ItemStorage cureItem : disease.cureItems)
            {
                StandardFactoryController.getInstance().serialize(byteBuf, cureItem);
            }
        }
        Network.getNetwork().sendToPlayer(new GlobalDiseaseSyncMessage(byteBuf), player);
    }

    /**
     * Read the data from the packet and parse it.
     *
     * @param byteBuf pck.
     */
    public static void readGlobalDiseasesPackets(final FriendlyByteBuf byteBuf)
    {
        final RandomCollection<ResourceLocation, Disease> newDiseases = new RandomCollection<>();
        final int size = byteBuf.readInt();
        for (int i = 0; i < size; i++)
        {
            final ResourceLocation id = byteBuf.readResourceLocation();
            final Component name = byteBuf.readComponent();
            final int rarity = byteBuf.readInt();

            final List<ItemStorage> cureItems = new ArrayList<>();
            final int itemCount = byteBuf.readInt();
            for (int j = 0; j < itemCount; j++)
            {
                cureItems.add(StandardFactoryController.getInstance().deserialize(byteBuf));
            }

            newDiseases.add(rarity, id, new Disease(id, name, rarity, cureItems));
        }
        DISEASES = newDiseases;
    }

    /**
     * Get a collection of all possible diseases.
     *
     * @return the collection of diseases.
     */
    @NotNull
    public static Collection<Disease> getDiseases()
    {
        return DISEASES.getAll();
    }

    /**
     * Get a specific disease by id.
     *
     * @param id the disease id.
     * @return the disease instance or null if it does not exist.
     */
    @Nullable
    public static Disease getDisease(final ResourceLocation id)
    {
        return DISEASES.get(id);
    }

    /**
     * Get a random disease from the list of diseases.
     *
     * @param random the random provider.
     * @return the random disease instance or null if no diseases exist.
     */
    @Nullable
    public static Disease getRandomDisease(final RandomSource random)
    {
        return DISEASES.next(random);
    }

    /**
     * Predicate for the different usages to check if inventory contains a cure.
     *
     * @param cure the expected cure item.
     * @return the predicate for checking if the cure exists.
     */
    public static Predicate<ItemStack> hasCureItem(final ItemStorage cure)
    {
        return stack -> isCureItem(stack, cure);
    }

    /**
     * Check if the given item is a cure item.
     *
     * @param stack the input stack.
     * @param cure  the cure item.
     * @return true if so.
     */
    public static boolean isCureItem(final ItemStack stack, final ItemStorage cure)
    {
        return ItemStackUtils.compareItemStacksIgnoreStackSize(stack, cure.getItemStack(), !cure.ignoreDamageValue(), !cure.ignoreNBT());
    }

    @Override
    protected void apply(
      final @NotNull Map<ResourceLocation, JsonElement> jsonElementMap,
      final @NotNull ResourceManager resourceManager,
      final @NotNull ProfilerFiller profiler)
    {
        final RandomCollection<ResourceLocation, Disease> diseases = new RandomCollection<>();
        for (final Map.Entry<ResourceLocation, JsonElement> entry : jsonElementMap.entrySet())
        {
            if (!entry.getValue().isJsonObject())
            {
                return;
            }

            final JsonObject object = entry.getValue().getAsJsonObject();
            final Component name = Component.translatable(GsonHelper.getAsString(object, KEY_NAME));
            final int rarity = GsonHelper.getAsInt(object, KEY_RARITY);
            final List<ItemStorage> cureItems = new ArrayList<>();
            for (final JsonElement jsonElement : object.getAsJsonArray(KEY_ITEMS))
            {
                if (!jsonElement.isJsonObject())
                {
                    continue;
                }

                cureItems.add(new ItemStorage(jsonElement.getAsJsonObject()));
            }

            diseases.add(rarity, entry.getKey(), new Disease(entry.getKey(), name, rarity, cureItems));
        }
        DISEASES = diseases;
    }
}
