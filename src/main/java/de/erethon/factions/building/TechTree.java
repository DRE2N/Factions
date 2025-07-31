package de.erethon.factions.building;

import de.erethon.factions.Factions;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class TechTree {

    public void show(Player player) {
        player.showDialog(createNotice());
    }

    public static Dialog createNotice() {
        String initialText = """
               §2// This is beautiful
               §6private void §9show§7(§bPlayer player§7) {
                   §6player§7.showDialog(§ocreateNotice()§r§7)§7;
               §7}          
                """;
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("DialogTest.java"))
                        .inputs(List.of(
                                DialogInput.text("test_text", Component.text(""))
                                        .multiline(TextDialogInput.MultilineOptions.create(null, 512))
                                        .maxLength(Integer.MAX_VALUE)
                                        .initial("§4")
                                        .width(800)
                                        .build()
                        ))
                        .build()
                )
                .type(DialogType.notice(ActionButton.builder(Component.text("button!"))
                        .tooltip(Component.text("cool button yay"))
                        .action(DialogAction.customClick((response, audience) -> {
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
