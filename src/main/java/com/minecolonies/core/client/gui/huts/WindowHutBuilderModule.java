package com.minecolonies.core.client.gui.huts;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractWindowWorkerModuleBuilding;
import com.minecolonies.core.client.gui.WindowHutGuide;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * BOWindow for the builder hut.
 */
public class WindowHutBuilderModule extends AbstractWindowWorkerModuleBuilding<BuildingBuilder.View>
{
    /**
     * The advancement location.
     */

    private static final ResourceLocation GUIDE_ADVANCEMENT = new ResourceLocation(Constants.MOD_ID, "minecolonies/check_out_guide");

    /**
     * If the guide should be attempted to be opened.
     */
    private final boolean needGuide;

    /**
     * Constructor for window builder hut.
     *
     * @param building {@link BuildingBuilder.View}.
     */
    public WindowHutBuilderModule(final BuildingBuilder.View building)
    {
        this(building, true);
    }

    /**
     * Constructor for window builder hut.
     *
     * @param needGuide if the guide should be opened.
     * @param building  {@link BuildingBuilder.View}.
     */
    public WindowHutBuilderModule(final BuildingBuilder.View building, final boolean needGuide)
    {
        super(building, Constants.MOD_ID + HUT_BUILDER_RESOURCE_SUFFIX);
        this.needGuide = needGuide;
    }

    @Override
    public void onOpened()
    {
        if (needGuide)
        {
            final AdvancementHolder ad = Minecraft.getInstance().player.connection.getAdvancements().get(GUIDE_ADVANCEMENT);
            if (ad == null || !Minecraft.getInstance().player.connection.getAdvancements().progress.getOrDefault(ad, new AdvancementProgress()).isDone())
            {
                close();
                new WindowHutGuide(building).open();
                return;
            }
        }
        super.onOpened();
    }
}
