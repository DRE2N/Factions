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
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author Fyreum
 */
public abstract class FLegalEntity extends EConfig implements FEntity {

    public static final int CONFIG_VERSION = 1;

    protected final Factions plugin = Factions.get();

    protected final int id;
    protected final UUID uuid;
    protected String name;
    protected String description;
    protected final Map<String, FactionAttribute> attributes = new HashMap<>();
    protected final Map<FPolicy, Boolean> policies = new HashMap<>(); // boolean == forced by war

    public FLegalEntity(@NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, CONFIG_VERSION);
        this.id = id;
        this.uuid = UUID.randomUUID();
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
        this.uuid = UUID.fromString(config.getString("uuid", UUID.randomUUID().toString()));
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
        config.set("policies", policies.keySet().stream().map(policy -> policy.name() + ":" + policies.get(policy)).toList());
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

    public UUID getUniqueId() {
        return uuid;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getName(boolean fancy) {
        return name.replace("_", " "); // For display purposes
    }


    public @NotNull Component name() {
        return Component.text(name);
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public boolean matchingName(String name) {
        if (name == null || this.name == null) { // This can happen, don't ask me why
            return false;
        }
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

    public void addAttribute(@NotNull String name, @NotNull FactionAttribute attribute) {
        attributes.put(name, attribute);
    }

    public void removeAttribute(@NotNull String name) {
        attributes.remove(name);
    }

    public @Nullable FactionAttribute getAttribute(@NotNull String name) {
        return attributes.get(name);
    }

    public @NotNull FactionAttribute getOrCreateAttribute(@NotNull String name, double def) {
        attributes.putIfAbsent(name, new FactionStatAttribute(def));
        return getAttribute(name);
    }

    public @NotNull <T extends FactionAttribute> T getOrCreateAttribute(@NotNull String name, @NotNull Class<T> clazz, @NotNull Supplier<T> def) {
        FactionAttribute found = attributes.get(name);
        if (!clazz.isInstance(found)) {
            T attribute = def.get();
            attributes.put(name, attribute);
            return attribute;
        }
        return (T) found;
    }

    public double getAttributeValue(@NotNull String name) {
        return getAttributeValue(name, 0d);
    }

    public double getAttributeValue(@NotNull String name, double def) {
        FactionAttribute attribute = attributes.get(name);
        return attribute == null ? def : attribute.getValue();
    }

    public void removeModifier(FactionAttributeModifier modifier) {
        for (FactionAttribute attribute : attributes.values()) {
            attribute.removeModifier(modifier);
        }
    }

    public @NotNull Map<FPolicy, Boolean> getPolicies() {
        return policies;
    }

    public boolean hasPolicy(@NotNull FPolicy policy) {
        return policies.containsKey(policy);
    }

    public void addPolicy(@NotNull FPolicy policy, boolean forcedByWar) {
        policies.put(policy, forcedByWar);
    }

    public void removePolicy(@NotNull FPolicy policy) {
        policies.remove(policy);
    }

}
