package de.erethon.factions.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public final class FDialogFactory {

    private static final int DEFAULT_DIALOG_WIDTH = 360;

    private FDialogFactory() {
    }

    public static void showNotice(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body) {
        player.showDialog(notice(player, title, body));
    }

    public static void showMultiAction(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body,
                                       @NotNull List<ActionButton> actions) {
        player.showDialog(multiAction(player, title, body, actions));
    }

    public static void showConfirmation(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body,
                                        @NotNull ActionButton yes, @NotNull ActionButton no) {
        player.showDialog(confirmation(player, title, body, yes, no));
    }

    public static void showTextInput(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body,
                                     @NotNull String inputKey, @NotNull Component inputLabel,
                                     @NotNull Component initialValue, int maxLength,
                                     @NotNull Component buttonLabel,
                                     @NotNull BiConsumer<String, Player> onSubmit) {
        player.showDialog(textInput(player, title, body, inputKey, inputLabel, initialValue, maxLength, buttonLabel, onSubmit));
    }

    public static Dialog notice(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body) {
        return Dialog.create(builder -> builder.empty()
                .base(base(localize(player, title), localize(player, body)).build())
                .type(DialogType.notice(button(localize(player, Component.translatable("factions.dialog.button.close")), null, null))));
    }

    public static Dialog notice(@NotNull Component title, @NotNull List<Component> body) {
        return Dialog.create(builder -> builder.empty()
                .base(base(title, body).build())
                .type(DialogType.notice(button(Component.translatable("factions.dialog.button.close"), null, null))));
    }

    public static Dialog multiAction(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body,
                                     @NotNull List<ActionButton> actions) {
        return multiAction(localize(player, title), localize(player, body), localizeButtons(player, actions));
    }

    public static Dialog multiAction(@NotNull Component title, @NotNull List<Component> body, @NotNull List<ActionButton> actions) {
        return Dialog.create(builder -> builder.empty()
                .base(base(title, body).build())
                .type(DialogType.multiAction(actions).columns(1).build()));
    }

    public static Dialog confirmation(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body,
                                      @NotNull ActionButton yes, @NotNull ActionButton no) {
        return confirmation(localize(player, title), localize(player, body), localizeButton(player, yes), localizeButton(player, no));
    }

    public static Dialog confirmation(@NotNull Component title, @NotNull List<Component> body,
                                      @NotNull ActionButton yes, @NotNull ActionButton no) {
        return Dialog.create(builder -> builder.empty()
                .base(base(title, body).build())
                .type(DialogType.confirmation(yes, no)));
    }

    public static Dialog textInput(@NotNull Player player, @NotNull Component title, @NotNull List<Component> body,
                                   @NotNull String inputKey, @NotNull Component inputLabel,
                                   @NotNull Component initialValue, int maxLength,
                                   @NotNull Component buttonLabel,
                                   @NotNull BiConsumer<String, Player> onSubmit) {
        return textInput(localize(player, title), localize(player, body), inputKey, localize(player, inputLabel),
                localize(player, initialValue), maxLength, localize(player, buttonLabel), onSubmit);
    }

    public static Dialog textInput(@NotNull Component title, @NotNull List<Component> body,
                                   @NotNull String inputKey, @NotNull Component inputLabel,
                                   @NotNull Component initialValue, int maxLength,
                                   @NotNull Component buttonLabel,
                                   @NotNull BiConsumer<String, Player> onSubmit) {
        return Dialog.create(builder -> builder.empty()
                .base(base(title, body)
                        .inputs(List.of(DialogInput.text(inputKey, inputLabel)
                                .width(DEFAULT_DIALOG_WIDTH)
                                .initial(plainText(initialValue))
                                .maxLength(maxLength)
                                .multiline(TextDialogInput.MultilineOptions.create(null, maxLength))
                                .build()))
                        .build())
                .type(DialogType.notice(button(buttonLabel, null, DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player player) {
                        onSubmit.accept(response.getText(inputKey), player);
                    }
                }, ClickCallback.Options.builder().build())))));
    }

    public static ActionButton button(@NotNull Component label, @Nullable Component tooltip,
                                      @Nullable DialogAction action) {
        ActionButton.Builder builder = ActionButton.builder(label).width(DEFAULT_DIALOG_WIDTH);
        if (tooltip != null) {
            builder.tooltip(tooltip);
        }
        if (action != null) {
            builder.action(action);
        }
        return builder.build();
    }

    public static @NotNull Component localize(@NotNull Player player, @NotNull Component component) {
        return GlobalTranslator.render(component, player.locale());
    }

    public static @NotNull List<Component> localize(@NotNull Player player, @NotNull List<Component> components) {
        return components.stream().map(component -> localize(player, component)).toList();
    }

    public static @NotNull ActionButton localizeButton(@NotNull Player player, @NotNull ActionButton button) {
        ActionButton.Builder builder = ActionButton.builder(localize(player, button.label()))
                .width(button.width());
        if (button.tooltip() != null) {
            builder.tooltip(localize(player, button.tooltip()));
        }
        if (button.action() != null) {
            builder.action(button.action());
        }
        return builder.build();
    }

    public static @NotNull List<ActionButton> localizeButtons(@NotNull Player player, @NotNull List<ActionButton> buttons) {
        return buttons.stream().map(button -> localizeButton(player, button)).toList();
    }

    private static DialogBase.Builder base(@NotNull Component title, @NotNull List<Component> body) {
        return DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList());
    }

    private static String plainText(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
