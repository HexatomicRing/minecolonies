package com.minecolonies.core.colony.workorders.view;

import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHallView;
import com.minecolonies.api.util.constant.TranslationConstants;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.network.chat.Component;

/**
 * The client side representation for a work order that the builder can take to build plantation fields.
 */
public class WorkOrderPlantationFieldView extends AbstractWorkOrderView
{
    @Override
    public Component getDisplayName()
    {
        return getOrderTypePrefix(Component.translatableEscape(getTranslationKey()));
    }

    private Component getOrderTypePrefix(Component nameComponent)
    {
        return switch (this.getWorkOrderType())
        {
            case BUILD -> Component.translatableEscape(TranslationConstants.BUILDER_ACTION_BUILDING, nameComponent);
            case REPAIR -> Component.translatableEscape(TranslationConstants.BUILDER_ACTION_REPAIRING, nameComponent);
            case REMOVE -> Component.translatableEscape(TranslationConstants.BUILDER_ACTION_REMOVING, nameComponent);
            default -> nameComponent;
        };
    }

    @Override
    public boolean shouldShowIn(IBuildingView view)
    {
        return view instanceof ITownHallView || view instanceof BuildingBuilder.View;
    }
}
