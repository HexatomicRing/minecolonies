package com.minecolonies.core.client.gui;

import com.ldtteam.blockui.controls.*;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.tileentities.TileEntityRack;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.colony.HireSpiesMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import static com.minecolonies.api.util.constant.TranslationConstants.DESCRIPTION_BARRACKS_HIRE_SPIES;

/**
 * UI for hiring spies on the barracks
 */
public class WindowsBarracksSpies extends BOWindow implements ButtonHandler
{
    /**
     * The xml file for this gui
     */
    private static final String SPIES_GUI_XML = ":gui/windowbarracksspies.xml";

    /**
     * The cancel button id
     */
    private static final String BUTTON_CANCEL = "cancel";

    /**
     * The hire spies button id
     */
    private static final String BUTTON_HIRE = "hireSpies";

    /**
     * The spies button icon id
     */
    private static final String SPIES_BUTTON_ICON = "hireSpiesIcon";

    /**
     * The gold amount label id
     */
    private static final String GOLD_COST_LABEL = "amount";

    /**
     * Text element id
     */
    private static final String TEXT_ID = "text";

    private static final int GOLD_COST = 5;

    /**
     * The client side colony data
     */
    private final IBuildingView buildingView;

    public WindowsBarracksSpies(final IBuildingView buildingView, final BlockPos buildingPos)
    {
        super(ResourceLocation.parse(Constants.MOD_ID + SPIES_GUI_XML));
        this.buildingView = buildingView;

        findPaneOfTypeByID(SPIES_BUTTON_ICON, ItemIcon.class).setItem(Items.GOLD_INGOT.getDefaultInstance());
        findPaneOfTypeByID(GOLD_COST_LABEL, Text.class).setText(Component.literal("x5"));

        final IItemHandler rackInv = ((TileEntityRack) buildingView.getColony().getWorld().getBlockEntity(buildingPos)).getInventory();
        final IItemHandler playerInv = new InvWrapper(Minecraft.getInstance().player.getInventory());
        int goldCount = InventoryUtils.getItemCountInItemHandler(playerInv, Items.GOLD_INGOT);
        goldCount += InventoryUtils.getItemCountInItemHandler(rackInv, Items.GOLD_INGOT);

        if (!buildingView.getColony().isRaiding() || goldCount < GOLD_COST || buildingView.getColony().areSpiesEnabled())
        {
            findPaneOfTypeByID(BUTTON_HIRE, ButtonImage.class).disable();
        }
        findPaneOfTypeByID(TEXT_ID, Text.class).setText(Component.translatableEscape(DESCRIPTION_BARRACKS_HIRE_SPIES));
    }

    @Override
    public void onButtonClicked(final Button button)
    {
        switch (button.getID())
        {
            case BUTTON_CANCEL:
            {
                this.close();
                break;
            }
            case BUTTON_HIRE:
            {
                findPaneOfTypeByID(BUTTON_HIRE, ButtonImage.class).disable();
                new HireSpiesMessage(buildingView.getColony()).sendToServer();
                this.close();
                break;
            }
        }
    }
}
