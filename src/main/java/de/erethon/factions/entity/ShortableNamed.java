package de.erethon.factions.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
public interface ShortableNamed {

    @NotNull String getName();

    void setName(@NotNull String name);

    default boolean hasShortName() {
        return getShortName() != null;
    }

    @Nullable String getShortName();

    default @NotNull String getDisplayShortName() {
        String shortName = getShortName();
        return shortName == null || shortName.isEmpty() ? getName() : shortName;
    }

    void setShortName(@Nullable String name);

    default boolean hasLongName() {
        return getLongName() != null;
    }

    @Nullable String getLongName();

    default @NotNull String getDisplayLongName() {
        String longName = getLongName();
        return longName == null || longName.isEmpty() ? getName() : longName;
    }

    void setLongName(@Nullable String longName);

}
