package de.erethon.factions.dialog;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class EditNameDialog {

    public static void show(FPlayer fplayer, Faction faction, Player player) {
        if (faction == null) {
            fplayer.sendMessage("Internal error: faction is null");
            return;
        }
        Dialog dialog =  Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.translatable("factions.dialog.edit_name.title"))
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.translatable("factions.dialog.edit_name.hint")
                                )
                        ))
                        .inputs(Arrays.asList(
                                DialogInput.text("name", Component.translatable("factions.dialog.edit_name.input.name"))
                                        .initial(faction.getName())
                                        .build(),
                                DialogInput.text("short_name", Component.translatable("factions.dialog.edit_name.input.short_name"))
                                        .initial(faction.getShortName() != null ? faction.getShortName() : "")
                                        .build(),
                                DialogInput.text("long_name", Component.translatable("factions.dialog.edit_name.input.long_name"))
                                        .initial(faction.getLongName() != null ? faction.getLongName() : "")
                                        .build()
                        ))
                        .build()
                )
                .type(DialogType.notice(ActionButton.builder(Component.translatable("factions.dialog.edit_name.button.save"))
                        .action(DialogAction.customClick((response, audience) -> {
                                    faction.setName(response.getText("name"));
                                    faction.setShortName(response.getText("short_name"));
                                    faction.setLongName(response.getText("long_name"));
                                    MessageUtil.sendMessage(player, Component.translatable("factions.dialog.edit_name.success"));
                                },
                                ClickCallback.Options.builder().build()
                        ))
                        .build()
                ))
        );
        player.showDialog(dialog);
    }
}
