package com.minecolonies.core.client.gui.citizen;

import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.*;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.colony.citizen.AdjustSkillCitizenMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * BOWindow for the citizen.
 */
public class MainWindowCitizen extends AbstractWindowCitizen
{
    /**
     * The citizenData.View object.
     */
    private final ICitizenDataView citizen;

    /**
     * Tick function for updating every second.
     */
    private int tick = 0;

    /**
     * Constructor to initiate the citizen windows.
     *
     * @param citizen citizen to bind the window to.
     */
    public MainWindowCitizen(final ICitizenDataView citizen)
    {
        super(citizen, Constants.MOD_ID + CITIZEN_MAIN_RESOURCE_SUFFIX);
        this.citizen = citizen;

        final Image statusIcon = findPaneOfTypeByID(STATUS_ICON, Image.class);
        if (citizen.getVisibleStatus() == null)
        {
            statusIcon.setVisible(false);
        }
        else
        {
            statusIcon.setImage(citizen.getVisibleStatus().getIcon(), false);
            PaneBuilders.tooltipBuilder()
                .append(Component.translatableEscape(citizen.getVisibleStatus().getTranslationKey()))
                .hoverPane(statusIcon)
                .build();
        }
    }

    public ICitizenDataView getCitizen()
    {
        return citizen;
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (tick++ == 20)
        {
            tick = 0;
            CitizenWindowUtils.createSkillContent(citizen, this);
        }
    }

    /**
     * Called when the gui is opened by an player.
     */
    @Override
    public void onOpened()
    {
        super.onOpened();
        findPaneOfTypeByID(WINDOW_ID_NAME, Text.class).setText(Component.literal(citizen.getName()));

        CitizenWindowUtils.createHealthBar(citizen, findPaneOfTypeByID(WINDOW_ID_HEALTHBAR, View.class));
        CitizenWindowUtils.createSaturationBar(citizen, this);
        CitizenWindowUtils.createHappinessBar(citizen, this);
        CitizenWindowUtils.createSkillContent(citizen, this);

        //Tool of class:§rwith minimal level:§rWood or Gold§r and§rwith maximal level:§rWood or Gold§r

        if (citizen.isFemale())
        {
            findPaneOfTypeByID(WINDOW_ID_GENDER, Image.class).setImage(ResourceLocation.parse(FEMALE_SOURCE), false);
        }
    }

    /**
     * Called when a button in the citizen has been clicked.
     *
     * @param button the clicked button.
     */
    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        super.onButtonClicked(button);
        if (button.getID().contains(PLUS_PREFIX))
        {
            final String label = button.getID().replace(PLUS_PREFIX, "");
            final Skill skill = Skill.valueOf(StringUtils.capitalize(label));

            new AdjustSkillCitizenMessage(colony, citizen, 1, skill).sendToServer();
        }
        else if (button.getID().contains(MINUS_PREFIX))
        {
            final String label = button.getID().replace(MINUS_PREFIX, "");
            final Skill skill = Skill.valueOf(StringUtils.capitalize(label));

            new AdjustSkillCitizenMessage(colony, citizen, -1, skill).sendToServer();
        }
    }
}
