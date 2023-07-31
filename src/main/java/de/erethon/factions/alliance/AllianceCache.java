package de.erethon.factions.alliance;

import de.erethon.factions.entity.FEntityCache;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Fyreum
 */
public class AllianceCache extends FEntityCache<Alliance> {

    public AllianceCache(@NotNull File folder) {
        super(folder);
    }

    @Override
    protected @Nullable Alliance create(@NotNull File file) {
        try {
            return new Alliance(file);
        } catch (NumberFormatException e) {
            FLogger.ERROR.log("Couldn't load alliance file '" + file.getName() + "': Invalid ID");
            return null;
        }
    }

    @Override
    public void loadAll() {
        super.loadAll();
        FLogger.INFO.log("Loaded " + cache.size() + " alliances");
    }

    public @NotNull List<Alliance> ranked() {
        return cache.values().stream().sorted(Comparator.comparingDouble(Alliance::getWarScore)).collect(Collectors.toList());
    }
}
