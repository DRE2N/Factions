package de.erethon.factions.economy.resource;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Malfrador
 */
public enum Resource {

    // Food - base resources
    GRAIN,
    FRUIT,
    VEGETABLES,
    FISH,
    COWS,
    PIGS,
    SHEEP,
    CHICKENS,
    // Food - processed
    BREAD,
    MEAT,
    CHEESE,
    BEER,
    WINE,
    // Goods - base resources
    IRON,
    COPPER,
    WAX,
    BARRELS,
    GOLD,
    COAL,
    STONE,
    WOOD,
    // Goods - processed
    TOOLS,
    CLOTH,
    // Luxury Goods - base resources
    SILK,
    SPICES,
    SALT,
    // Luxury Goods - processed
    JEWELRY,
    FURNITURE,
    // Building Materials
    BRICKS,
    GLASS,
    // Culture, Religion, Science
    BOOKS,
    PAPER,
    INK,
    CANDLES,
    MITHRIL,
    // Military resources
    HORSES,
    SIEGE_EQUIPMENT,
    WEAPONS,
    ARMOR;

    public static @Nullable Resource getById(@NotNull String id) {
        try {
            return valueOf((id.equalsIgnoreCase("fruits") ? "FRUIT" : id).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public @NotNull String getId() {
        return name().toLowerCase();
    }

    public Component displayName() {
        return Component.translatable("factions.economy.resource." + getId());
    }

}
