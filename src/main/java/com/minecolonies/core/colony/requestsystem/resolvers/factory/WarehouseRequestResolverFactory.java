package com.minecolonies.core.colony.requestsystem.resolvers.factory;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolverFactory;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.constant.SerializationIdentifierConstants;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.core.colony.requestsystem.resolvers.WarehouseRequestResolver;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class WarehouseRequestResolverFactory implements IRequestResolverFactory<WarehouseRequestResolver>
{
    ////// --------------------------- NBTConstants --------------------------- \\\\\\
    private static final String NBT_TOKEN    = "Token";
    private static final String NBT_LOCATION = "Location";
    ////// --------------------------- NBTConstants --------------------------- \\\\\\

    @NotNull
    @Override
    public TypeToken<? extends WarehouseRequestResolver> getFactoryOutputType()
    {
        return TypeToken.of(WarehouseRequestResolver.class);
    }

    @NotNull
    @Override
    public TypeToken<? extends ILocation> getFactoryInputType()
    {
        return TypeConstants.ILOCATION;
    }

    @NotNull
    @Override
    public WarehouseRequestResolver getNewInstance(
      @NotNull final IFactoryController factoryController,
      @NotNull final ILocation iLocation,
      @NotNull final Object... context)
      throws IllegalArgumentException
    {
        return new WarehouseRequestResolver(iLocation, factoryController.getNewInstance(TypeConstants.ITOKEN));
    }

    @NotNull
    @Override
    public CompoundTag serialize(@NotNull final HolderLookup.Provider provider,
      @NotNull final IFactoryController controller, @NotNull final WarehouseRequestResolver warehouseRequestResolver)
    {
        final CompoundTag compound = new CompoundTag();
        compound.put(NBT_TOKEN, controller.serializeTag(provider, warehouseRequestResolver.getId()));
        compound.put(NBT_LOCATION, controller.serializeTag(provider, warehouseRequestResolver.getLocation()));
        return compound;
    }

    @NotNull
    @Override
    public WarehouseRequestResolver deserialize(@NotNull final HolderLookup.Provider provider, @NotNull final IFactoryController controller, @NotNull final CompoundTag nbt)
    {
        final IToken<?> token = controller.deserializeTag(provider, nbt.getCompound(NBT_TOKEN));
        final ILocation location = controller.deserializeTag(provider, nbt.getCompound(NBT_LOCATION));

        return new WarehouseRequestResolver(location, token);
    }

    @Override
    public void serialize(IFactoryController controller, WarehouseRequestResolver input, RegistryFriendlyByteBuf packetBuffer)
    {
        controller.serialize(packetBuffer, input.getId());
        controller.serialize(packetBuffer, input.getLocation());
    }

    @Override
    public WarehouseRequestResolver deserialize(IFactoryController controller, RegistryFriendlyByteBuf buffer) throws Throwable
    {
        final IToken<?> token = controller.deserialize(buffer);
        final ILocation location = controller.deserialize(buffer);

        return new WarehouseRequestResolver(location, token);
    }

    @Override
    public short getSerializationId()
    {
        return SerializationIdentifierConstants.WAREHOUSE_REQUEST_RESOLVER_ID;
    }
}
