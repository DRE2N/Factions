package de.erethon.factions.region.schematic;

import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.QuadConsumer;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class RegionSchematic extends EConfig {

    public static final int CONFIG_VERSION = 1;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy_MM_dd");

    private final String name;
    private String[][][] blocks;

    protected RegionSchematic(@NotNull String name) {
        super(new File(Factions.SCHEMATICS, name + ".yml"), CONFIG_VERSION);
        this.name = name;
    }

    protected RegionSchematic(@NotNull File file) {
        super(file, CONFIG_VERSION);
        this.name = file.getName().replace(".yml", "");
        load();
    }

    /* Instantiation stuff */

    public void scanAndInsertBlocks(@NotNull World world, @NotNull Position a, @NotNull Position b) {
        int minX = Math.min(a.blockX(), b.blockX()),
                minY = Math.min(a.blockY(), b.blockY()),
                minZ = Math.min(a.blockZ(), b.blockZ()),
                maxX = Math.max(a.blockX(), b.blockX()),
                maxY = Math.max(a.blockY(), b.blockY()),
                maxZ = Math.max(a.blockZ(), b.blockZ());

        blocks = new String[maxX - minX][maxY - minY][maxZ - minZ];

        foreach((x, y, z, data) -> {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.AIR) {
                return;
            }
            blocks[x][y][z] = block.getBlockData().getAsString();
        });
    }

    public void rescanAndInsertBlocks(@NotNull World world, @NotNull Position a, @NotNull Position b) {
        saveBackup();
        scanAndInsertBlocks(world, a, b);
    }

    private void saveBackup() {
        File backupFile = createBackupFile();
        FLogger.DEBUG.log("Creating region schematic backup: " + backupFile.getName() + "...");
        YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        applyData(backupConfig);
        try {
            backupConfig.save(backupFile);
        } catch (IOException e) {
            FLogger.ERROR.log("Couldn't save backup of region schematic '" + name + "': ");
            e.printStackTrace();
        }
    }

    private File createBackupFile() {
        String fileName = name + "_" + DATE_FORMAT.format(new Date());
        File backupFile = new File(Factions.SCHEMATICS, fileName + ".yml");
        if (!backupFile.exists()) {
            return backupFile;
        }
        int i = 0;
        while ((backupFile = new File(Factions.SCHEMATICS, fileName + "-" + i + ".yml")).exists()) {
            i++;
        }
        return backupFile;
    }

    /* Serialization */

    @Override
    public void load() {
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            FLogger.ERROR.log("Couldn't load region schematic '" + name + "': No blocks found");
            return;
        }
        this.blocks = new String[config.getInt("xLength")][config.getInt("yLength")][config.getInt("zLength")];

        for (String xKey : blocksSection.getKeys(false)) {
            ConfigurationSection xSection = blocksSection.getConfigurationSection(xKey);
            if (xSection == null) {
                continue;
            }
            int x = NumberUtil.parseInt(xKey);
            if (x < 0) {
                FLogger.ERROR.log("Illegal x-coordinate in region schematic " + name + " found: " + xKey);
                continue;
            }
            for (String yKey : blocksSection.getKeys(false)) {
                ConfigurationSection ySection = xSection.getConfigurationSection(yKey);
                if (ySection == null) {
                    continue;
                }
                int y = NumberUtil.parseInt(yKey);
                if (y < 0) {
                    FLogger.ERROR.log("Illegal y-coordinate in region schematic " + name + " found: " + yKey);
                    continue;
                }
                for (String zKey : blocksSection.getKeys(false)) {
                    int z = NumberUtil.parseInt(zKey);
                    if (z < 0) {
                        FLogger.ERROR.log("Illegal z-coordinate in region schematic " + name + " found: " + zKey);
                        continue;
                    }
                    blocks[x][y][z] = ySection.getString(zKey);
                }
            }
        }
    }

    public void applyData(@NotNull ConfigurationSection config) {
        int xLength = getXLength(), yLength = getYLength(), zLength = getZLength();

        config.set("xLength", xLength);
        config.set("yLength", yLength);
        config.set("zLength", zLength);

        Map<String, Object> xMap = new HashMap<>();
        for (int x = 0; x < xLength; x++) {
            Map<String, Object> yMap = new HashMap<>();
            for (int y = 0; y < yLength; y++) {
                Map<String, Object> zMap = new HashMap<>();
                for (int z = 0; z < zLength; z++) {
                    String blockData = blocks[x][y][z];
                    if (blockData == null) {
                        continue;
                    }
                    zMap.put(String.valueOf(z), blockData);
                }
                yMap.put(String.valueOf(y), zMap);
            }
            xMap.put(String.valueOf(x), yMap);
        }
        config.set("blocks", xMap);
    }

    public void saveData() {
        applyData(config);
        save();
    }

    /* Getters and setters */

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String[][][] getBlocks() {
        return blocks;
    }

    /**
     * @return the blockdata at the given coordinates
     * @throws IndexOutOfBoundsException if the coordinates are out of bounds
     */
    public @Nullable String getBlockAt(int x, int y, int z) throws IndexOutOfBoundsException {
        return blocks[x][y][z];
    }

    /**
     * @return the blockdata at the given coordinates
     * @throws IndexOutOfBoundsException if the coordinates are out of bounds
     */
    public @NotNull BlockData getBlockDataAt(int x, int y, int z) throws IndexOutOfBoundsException {
        String data = getBlockAt(x, y, z);
        return data == null ? Material.AIR.createBlockData() : Bukkit.createBlockData(data);
    }

    public int getXLength() {
        return blocks.length;
    }

    public int getYLength() {
        return blocks[0].length;
    }

    public int getZLength() {
        return blocks[0][0].length;
    }

    public void foreach(@NotNull QuadConsumer<Integer, Integer, Integer, String> action) {
        int xLength = getXLength(), yLength = getYLength(), zLength = getZLength();

        for (int x = 0; x < xLength; x++) {
            for (int y = 0; y < yLength; y++) {
                for (int z = 0; z < zLength; z++) {
                    action.accept(x, y, z, blocks[x][y][z]);
                }
            }
        }
    }

}
