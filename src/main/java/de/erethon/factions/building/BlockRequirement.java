package de.erethon.factions.building;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class BlockRequirement {

    private final Material material;
    private final FSetTag tag;
    private final int amount;

    public BlockRequirement(@NotNull Material material, int amount) {
        this.material = material;
        this.tag = null;
        this.amount = amount;
    }

    public BlockRequirement(@NotNull FSetTag tag, int amount) {
        this.material = null;
        this.tag = tag;
        this.amount = amount;
    }

    @Nullable
    public Material getMaterial() {
        return material;
    }

    @Nullable
    public FSetTag getTag() {
        return tag;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isMaterialRequirement() {
        return material != null;
    }

    public boolean isTagRequirement() {
        return tag != null;
    }

    public boolean matches(Material mat) {
        if (isMaterialRequirement()) {
            return material == mat;
        } else {
            if (tag == null) {
                return false;
            }
            return tag.hasBlock(mat);
        }
    }

    public Set<Material> getAllValidMaterials() {
        if (isMaterialRequirement()) {
            Set<Material> result = new HashSet<>();
            result.add(material);
            return result;
        } else {
            if (tag == null) {
                return new HashSet<>();
            }
            return tag.getMaterials();
        }
    }

    public String getDisplayName() {
        if (isMaterialRequirement()) {
            return material.name();
        } else {
            return tag.name();
        }
    }
}

