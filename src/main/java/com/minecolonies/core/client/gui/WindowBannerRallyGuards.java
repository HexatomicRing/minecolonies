package com.minecolonies.core.client.gui;

import com.ldtteam.blockui.Color;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.api.colony.guardtype.registry.ModGuardTypes;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.network.messages.server.RemoveFromRallyingListMessage;
import com.minecolonies.core.network.messages.server.ToggleBannerRallyGuardsMessage;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.core.items.ItemBannerRallyGuards.*;

/**
 * ClipBoard window.
 */
public class WindowBannerRallyGuards extends AbstractWindowSkeleton
{
    /**
     * Resource suffix.
     */
    private static final String BUILD_TOOL_RESOURCE_SUFFIX = ":gui/windowbannerrallyguards.xml";

    /**
     * Requests list id.
     */
    private static final String LIST_GUARDTOWERS = "guardtowers";

    /**
     * Requestst stack id.
     */
    private static final String ICON_GUARD = "guardicon";

    /**
     * Id of the resource add button.
     */
    private static final String BUTTON_REMOVE = "remove";

    /**
     * Id of the resource add button.
     */
    private static final String BUTTON_RALLY = "rally";

    /**
     * Id of the detail button.
     */
    private static final String LABEL_GUARDTYPE = "guardtype";

    /**
     * Id of the short detail label.
     */
    private static final String LABEL_POSITION = "position";

    /**
     * Scrollinglist of the guard towers.
     */
    private ScrollingList guardTowerList;

    /**
     * Banner for which the window is opened
     */
    private ItemStack banner = null;

    /**
     * Constructor of the rally banner window
     *
     * @param banner The banner to be displayed
     */
    public WindowBannerRallyGuards(final ItemStack banner)
    {
        super(Constants.MOD_ID + BUILD_TOOL_RESOURCE_SUFFIX);
        this.banner = banner;

        registerButton(BUTTON_REMOVE, this::removeClicked);
        registerButton(BUTTON_RALLY, this::rallyClicked);
    }

    @Override
    public void onOpened()
    {
        guardTowerList = findPaneOfTypeByID(LIST_GUARDTOWERS, ScrollingList.class);

        if (isActive(banner))
        {
            findPaneOfTypeByID(BUTTON_RALLY, ButtonImage.class).setText(Component.translatableEscape(COM_MINECOLONIES_BANNER_RALLY_GUARDS_GUI_DISMISS));
        }
        else
        {
            findPaneOfTypeByID(BUTTON_RALLY, ButtonImage.class).setText(Component.translatableEscape(COM_MINECOLONIES_BANNER_RALLY_GUARDS_GUI_RALLY));
        }

        guardTowerList.setDataProvider(() -> getGuardTowerViews(banner, mc.level).size(), (index, rowPane) ->
        {
            final List<Pair<BlockPos, AbstractBuildingGuards.View>> guardTowers = getGuardTowerViews(banner, mc.level);
            if (index < 0 || index >= guardTowers.size())
            {
                return;
            }

            final Pair<BlockPos, AbstractBuildingGuards.View> guardTower = guardTowers.get(index);

            //todo we probably want to display the exact mix.
            final ItemIcon exampleStackDisplay = rowPane.findPaneOfTypeByID(ICON_GUARD, ItemIcon.class);
            final AbstractBuildingGuards.View guardTowerView = guardTower.getSecond();

            if (guardTowerView != null)
            {
                exampleStackDisplay.setItem(new ItemStack(Items.IRON_SWORD));
                rowPane.findPaneOfTypeByID(LABEL_GUARDTYPE, Text.class).setText(Component.translatableEscape(ModGuardTypes.knight.get().getJobTranslationKey())
                  .append("|")
                  .append(Component.translatableEscape(ModGuardTypes.ranger.get().getJobTranslationKey()))
                  .append(": ")
                  .append(String.valueOf(guardTowerView.getGuards().size())));
                rowPane.findPaneOfTypeByID(LABEL_POSITION, Text.class).setText(Component.literal(guardTower.getFirst().toString()));
            }
            else
            {
                exampleStackDisplay.setItem(new ItemStack(Items.COOKIE));

                rowPane.findPaneOfTypeByID(LABEL_GUARDTYPE, Text.class)
                  .setText(Component.translatableEscape(COM_MINECOLONIES_BANNER_RALLY_GUARDS_GUI_TOWERMISSING));
                rowPane.findPaneOfTypeByID(LABEL_GUARDTYPE, Text.class).setColors(Color.rgbaToInt(255, 0, 0, 1));
                rowPane.findPaneOfTypeByID(LABEL_POSITION, Text.class).setText(Component.literal(guardTower.getFirst().toString()));
            }
        });
    }

    /**
     * Handles removal of towers from the rallying list.
     *
     * @param button The button used to remove the tower.
     */
    private void removeClicked(@NotNull final Button button)
    {
        final int row = guardTowerList.getListElementIndexByPane(button);

        final List<Pair<BlockPos, AbstractBuildingGuards.View>> guardTowers = getGuardTowerViews(banner, mc.level);
        if (guardTowers.size() > row && row >= 0)
        {
            final BlockPos locationToRemove = guardTowers.get(row).getFirst();
            // Server side removal
            new RemoveFromRallyingListMessage(banner, locationToRemove).sendToServer();

            // Client side removal
            removeGuardTowerAtLocation(banner, locationToRemove);
        }
    }

    /**
     * Handles toggle of banner.
     *
     * @param button The button used to toggle the banner.
     */
    private void rallyClicked(@NotNull final Button button)
    {
        new ToggleBannerRallyGuardsMessage(banner).sendToServer();
        this.close();
    }
}
