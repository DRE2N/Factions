package de.erethon.factions.entity;

import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * @author Fyreum
 */
public abstract class FLegalEntity extends EConfig implements FEntity {

    public static final int CONFIG_VERSION = 1;

    protected final Factions plugin = Factions.get();

    protected final int id;
    protected String name;
    protected String description;

    public FLegalEntity(@NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, CONFIG_VERSION);
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * Initializes a FEntity from a file.
     *
     * @param file the file to load the data from
     * @throws NumberFormatException if the file name doesn't contain the entity ID
     */
    public FLegalEntity(@NotNull File file) throws NumberFormatException {
        super(file, CONFIG_VERSION);
        this.id = Integer.parseInt(file.getName().replace(".yml", ""));
        this.name = config.getString("name");
        this.description = config.getString("description");
    }

    /* Serialisation */

    public void saveData() {
        config.set("name", name);
        config.set("description", description);
        serializeData();
        save();
    }

    protected void saveEntities(@NotNull String key, @NotNull Collection<? extends FLegalEntity> entities) {
        config.set(key, entities.stream().map(FLegalEntity::getId).toList());
    }

    /* Abstracts */

    protected abstract void serializeData();

    /* Getters and setters */

    public int getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public boolean matchingName(@NotNull String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @NotNull String getDisplayDescription() {
        return description == null ? FMessage.GENERAL_DEFAULT_DESCRIPTION.getMessage() : description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }
}
