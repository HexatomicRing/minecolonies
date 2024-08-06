package com.minecolonies.api.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.minecolonies.api.util.constant.NbtTagConstants.*;

/**
 * Utility class for summoning in fireworks.
 */

public final class FireworkUtils
{
    /**
     * Private constructor to hide the public one
     */
    private FireworkUtils()
    {

    }

    /**
     * Spawns in a given number of fireworks at the corners of a given AABB in a given world
     *
     * @param realaabb       AABB of the building
     * @param world          which world to spawn it in from
     * @param explosionLevel how many fireworks to spawn in each corner
     */
    public static void spawnFireworksAtAABBCorners(final Tuple<BlockPos, BlockPos> realaabb, final Level world, final int explosionLevel)
    {
        fireRocket(world, new BlockPos(realaabb.getB().getX(), realaabb.getB().getY(), realaabb.getB().getZ()), explosionLevel);
        fireRocket(world, new BlockPos(realaabb.getB().getX(), realaabb.getB().getY(), realaabb.getA().getZ()), explosionLevel);
        fireRocket(world, new BlockPos(realaabb.getA().getX(), realaabb.getB().getY(), realaabb.getB().getZ()), explosionLevel);
        fireRocket(world, new BlockPos(realaabb.getA().getX(), realaabb.getB().getY(), realaabb.getA().getZ()), explosionLevel);
    }

    /**
     * Fires a rocket at the given position, only if the sky is visible.
     *
     * @param world          which world to spawn it in.
     * @param position       the position to fire the rocket from.
     * @param explosionLevel how many fireworks to spawn in each corner.
     */
    private static void fireRocket(final Level world, final BlockPos position, final int explosionLevel)
    {
        if (world.canSeeSky(position))
        {
            final FireworkRocketEntity firework = new FireworkRocketEntity(world, position.getX(), position.getY(), position.getZ(), genFireworkItemStack(explosionLevel));
            world.addFreshEntity(firework);
        }
    }

    /**
     * Generates random firework with various properties.
     *
     * @param explosionAmount the amount of explosions.
     * @return ItemStack of random firework.
     */
    private static ItemStack genFireworkItemStack(final int explosionAmount)
    {
        final Random rand = new Random();
        final ItemStack fireworkItem = new ItemStack(Items.FIREWORK_ROCKET);
        List<FireworkExplosion> list = new ArrayList<>();

        final List<Integer> dyeColors = Arrays.stream(DyeColor.values()).map(DyeColor::getFireworkColor).toList();

        for (int i = 0; i < explosionAmount; i++)
        {
            final CompoundTag explosionTag = new CompoundTag();

            explosionTag.putInt(TAG_TYPE, rand.nextInt(5));

            final int numberOfColours = rand.nextInt(3) + 1;
            final IntList colors = new IntArrayList();

            for (int ia = 0; ia < numberOfColours; ia++)
            {
                colors.add(dyeColors.get(rand.nextInt(15)));
            }
            explosionTag.putIntArray(TAG_COLORS, colors);
            list.add(new FireworkExplosion(FireworkExplosion.Shape.values()[rand.nextInt(FireworkExplosion.Shape.values().length)], colors, colors, rand.nextInt(2) == 0, rand.nextInt(2) == 0));
        }

        fireworkItem.set(DataComponents.FIREWORKS, new Fireworks(explosionAmount, list));

        return fireworkItem;
    }
}
