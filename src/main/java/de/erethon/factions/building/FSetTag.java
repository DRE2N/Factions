package de.erethon.factions.building;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a custom building tag that groups materials together.
 * Tags are now loaded dynamically from buildingTags.yml configuration.
 *
 * @author Malfrador
 */
public class FSetTag {

    private final String name;
    private final Set<Material> materials;

    /**
     * Creates a new building tag.
     *
     * @param name The name of the tag
     * @param materials The set of materials this tag contains
     */
    public FSetTag(@NotNull String name, @NotNull Set<Material> materials) {
        this.name = name;
        this.materials = materials;
    }

    /**
     * Gets the name of this tag.
     *
     * @return The tag name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Gets all materials in this tag.
     *
     * @return An unmodifiable set of materials
     */
    @NotNull
    public Set<Material> getMaterials() {
        return Collections.unmodifiableSet(materials);
    }

    /**
     * Checks if this tag contains the specified material.
     *
     * @param block The material to check
     * @return true if this tag contains the material
     */
    public boolean hasBlock(@NotNull Material block) {
        return materials.contains(block);
    }

    @Override
    public String toString() {
        return "FSetTag{" + name + ", materials=" + materials.size() + "}";
    }
}