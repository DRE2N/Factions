package de.erethon.factions.building;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class TechTree {

    public void show(Player player) {
        player.showDialog(createNotice());
    }

    public static Dialog createNotice() {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("notice dialog"))
                        .body(Arrays.asList(
                                DialogBody.plainMessage(
                                        Component.text("body message", NamedTextColor.LIGHT_PURPLE)
                                ),
                                DialogBody.item(new ItemStack(Material.STICK))
                                        .description(DialogBody.plainMessage(Component.text("description message")))
                                        .build()
                        ))
                        .inputs(Arrays.asList(
                                DialogInput.text("test_text", Component.text("text input"))
                                        .multiline(TextDialogInput.MultilineOptions.create(null, null))
                                        .initial("xd")
                                        .build(),
                                DialogInput.bool("test_bool", Component.text("boolean input"))
                                        .build(),
                                DialogInput.numberRange("test_number", Component.text("number input"), 0.0F, 100.0F)
                                        .step(1.0F)
                                        .build(),
                                DialogInput.singleOption(
                                                "test_single",
                                                Component.text("single input"),
                                                Arrays.asList(
                                                        SingleOptionDialogInput.OptionEntry.create("A", Component.text("Option A"), false),
                                                        SingleOptionDialogInput.OptionEntry.create("B", Component.text("Option B"), false),
                                                        SingleOptionDialogInput.OptionEntry.create("C", Component.text("Option C"), true),
                                                        SingleOptionDialogInput.OptionEntry.create("D", Component.text("Option D"), false)
                                                )
                                        )
                                        .build()
                        ))
                        .build()
                )
                .type(DialogType.notice(ActionButton.builder(Component.text("button!"))
                        .tooltip(Component.text("cool button yay"))
                        .action(DialogAction.customClick((response, audience) -> {
                                    audience.sendMessage(Component.text("test_text: " + response.getText("test_text")));
                                    audience.sendMessage(Component.text("test_bool: " + response.getBoolean("test_bool")));
                                    audience.sendMessage(Component.text("test_number: " + response.getFloat("test_number")));
                                    audience.sendMessage(Component.text("test_single: " + response.getText("test_single")));
                                },
                                ClickCallback.Options.builder().build()
                        ))
                        .build()
                ))
        );
    }
}
