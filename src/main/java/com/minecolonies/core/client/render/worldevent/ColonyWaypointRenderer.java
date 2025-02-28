package com.minecolonies.core.client.render.worldevent;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.ldtteam.structurize.storage.rendering.RenderingCache;
import com.ldtteam.structurize.storage.rendering.types.BlueprintPreviewData;
import net.minecraft.core.BlockPos;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.minecolonies.api.util.constant.CitizenConstants.WAYPOINT_STRING;
import static com.minecolonies.api.util.constant.Constants.STORAGE_STYLE;

public class ColonyWaypointRenderer
{
    /**
     * Cached wayPointBlueprint.
     */
    private static BlueprintPreviewData wayPointTemplate;

    /**
     * Pending template to be loaded.
     */
    private static Future<Blueprint> pendingTemplate;

    /**
     * Renders waypoints of current colony.
     * 
     * @param ctx rendering context
     */
    static void render(final WorldEventContext ctx)
    {
        if (!RenderingCache.hasBlueprint("blueprint"))
        {
            return;
        }
        final Blueprint structure = RenderingCache.getOrCreateBlueprintPreviewData("blueprint").getBlueprint();
        if (structure != null && structure.getFilePath().toString().contains(WAYPOINT_STRING) && ctx.nearestColony != null)
        {
            if (wayPointTemplate == null && pendingTemplate == null)
            {
                pendingTemplate = StructurePacks.getBlueprintFuture(STORAGE_STYLE, "infrastructure/misc/waypoint.blueprint", ctx.clientLevel.registryAccess());
            }

            if (pendingTemplate != null)
            {
                if (pendingTemplate.isDone())
                {
                    try
                    {
                        final BlueprintPreviewData tempPreviewData = new BlueprintPreviewData();
                        tempPreviewData.setBlueprint(pendingTemplate.get());
                        tempPreviewData.setPos(BlockPos.ZERO);
                        wayPointTemplate = tempPreviewData;
                        pendingTemplate = null;
                    }
                    catch (InterruptedException | ExecutionException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    return;
                }
            }

            if (wayPointTemplate == null)
            {
                return;
            }

            ctx.renderBlueprint(
                RenderingCache.getOrCreateBlueprintPreviewData("blueprint").getBlueprint().hashCode() == wayPointTemplate.hashCode() ?
                    RenderingCache.getOrCreateBlueprintPreviewData("blueprint") :
                    wayPointTemplate,
                ctx.nearestColony.getWayPoints().keySet());
        }
    }
}
