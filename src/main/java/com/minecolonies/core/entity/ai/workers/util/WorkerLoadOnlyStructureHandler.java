package com.minecolonies.core.entity.ai.workers.util;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.util.LoadOnlyStructureHandler;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.jobs.AbstractJobStructure;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Load only structure handler just to get dimensions etc from structures, not for placement specific for worker usage.
 */
public final class WorkerLoadOnlyStructureHandler<J extends AbstractJobStructure<?, J>, B extends AbstractBuildingStructureBuilder> extends LoadOnlyStructureHandler
{
    /**
     * The structure AI handling this task.
     */
    private final AbstractEntityAIStructure<J, B> structureAI;

    /**
     * The minecolonies specific worker load only structure placer.
     *
     * @param world          the world.
     * @param pos            the pos it is placed at.
     * @param blueprint      the blueprint.
     * @param rotMir       the placement settings.
     * @param fancyPlacement if fancy or complete.
     */
    public WorkerLoadOnlyStructureHandler(
      final Level world, final BlockPos pos, final Blueprint blueprint, final RotationMirror rotMir, final boolean fancyPlacement,
      final AbstractEntityAIStructure<J, B> entityAIStructure)
    {
        super(world, pos, blueprint, rotMir, fancyPlacement);
        this.structureAI = entityAIStructure;
    }

    @Override
    public BlockState getSolidBlockForPos(final BlockPos worldPos, @Nullable final Function<BlockPos, BlockState> virtualBlocks)
    {
        return structureAI.getSolidSubstitution(worldPos, virtualBlocks);
    }
}
