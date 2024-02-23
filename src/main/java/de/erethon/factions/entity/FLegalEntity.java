package de.erethon.factions.entity;

import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.misc.EnumUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.policy.FPolicy;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.building.attributes.FactionStatAttribute;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public abstract class FLegalEntity extends EConfig implements FEntity {

    public static final int CONFIG_VERSION = 1;

    protected final Factions plugin = Factions.get();

    protected final int id;
    protected String name;
    protected String description;
    protected final Map<String, FactionAttribute> attributes = new HashMap<>();
    protected final Map<FPolicy, Boolean> policies = new HashMap<>(); // boolean == forced by war

    public FLegalEntity(@NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, CONFIG_VERSION);
        this.id = id;
        this.name = name;
        this.description = description;
        addDefaultAttributes(); // Initialize default attributes
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

        for (String policyString : config.getStringList("policies")) {
            String[] split = policyString.split(":");
            String policyName = split[0];
            boolean forcedByWar = split.length > 1 && Boolean.parseBoolean(split[1]);
            FPolicy policy = EnumUtil.getEnumIgnoreCase(FPolicy.class, policyName);

            if (policy == null) {
                FLogger.WARN.log("FPolicy " + policyName + " not found for entity " + name);
                continue;
            }
            policy.apply(this);
            policies.put(policy, forcedByWar);
        }
        addDefaultAttributes(); // Initialize default attributes
    }

    protected void addDefaultAttributes() {
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

    public @NotNull Component name() {
        return Component.text(name);
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

    public @NotNull Map<String, FactionAttribute> getAttributes() {
        return attributes;
    }

    public void addModifier(FactionAttribute attribute, FactionAttributeModifier modifier) {
        attribute.addModifier(modifier);
    }

    public void removeModifier(FactionAttribute attribute, FactionAttributeModifier modifier) {
        attribute.removeModifier(modifier);
    }

    public void removeModifier(FactionAttributeModifier modifier) {
        for (FactionAttribute attribute : attributes.values()) {
            attribute.removeModifier(modifier);
        }
    }

    public @Nullable FactionAttribute getAttribute(@NotNull String name) {
        FactionAttribute attribute = attributes.get(name);
        return attribute == null ? null : attribute.apply();
    }

    public FactionAttribute getOrCreateAttribute(@NotNull String name, double def) {
        attributes.putIfAbsent(name, new FactionStatAttribute(def));
        return attributes.get(name);
    }

    public double getAttributeValue(@NotNull String name) {
        return getAttributeValue(name, 0d);
    }

    public double getAttributeValue(@NotNull String name, double def) {
        FactionAttribute attribute = attributes.get(name);
        return attribute == null ? def : attribute.getValue();
    }

    public @NotNull Map<FPolicy, Boolean> getPolicies() {
        return policies;
    }

    public void addPolicy(@NotNull FPolicy policy, boolean forcedByWar) {
        policies.put(policy, forcedByWar);
    }

    public void removePolicy(@NotNull FPolicy policy) {
        policies.remove(policy);
    }

}
