package de.erethon.factions.building;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.RegionType;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuildingEffect implements ConfigurationSerializable {

    private String displayName = "";
    private long duration = -1;
    private Map<Resource, Double> consumptionModifier = new HashMap<>();
    private Map<Resource, Double> productionModifier = new HashMap<>();
    private final Map<Resource, Integer> virtualResourceProduction = new HashMap<>(); // Produces resources
    private final Map<Material, Integer> physicalResourceProduction = new HashMap<>(); // TODO: Custom item support
    private final Set<BuildingRespawnableBlockEntry> respawnableBlocks = new HashSet<>();
    private Map<PopulationLevel, Integer> happinessBuff = new HashMap<>();
    private Map<Effect, Integer> minecraftEffects = new HashMap<>();
    private RegionType changeTypeTo;
    private double memberModifier = 0.00;
    private double regionModifier = 0.00;
    private double manpowerModifier = 0.00;
    private int prestige = 0;
    private int allianceLimitBuff = 0;
    private int transportShipLimit = 0;
    private int transportAirshipLimit = 0;
    private int transportCoachLimit = 0;
    private int importDailyLimit = 0;
    private int exportDailyLimit = 0;
    private String memberPermission;

    public void add(Faction faction) {

    }

    public void remove(Faction faction) {

    }


    public Map<Resource, Double> getConsumptionModifier() {
        return consumptionModifier;
    }

    public void setConsumptionModifier(Map<Resource, Double> consumptionModifier) {
        this.consumptionModifier = consumptionModifier;
    }

    public Map<Resource, Double> getProductionModifier() {
        return productionModifier;
    }

    public void setProductionModifier(Map<Resource, Double> productionModifier) {
        this.productionModifier = productionModifier;
    }

    public Map<PopulationLevel, Integer> getHappinessBuff() {
        return happinessBuff;
    }

    public void setHappinessBuff(Map<PopulationLevel, Integer> happinessBuff) {
        this.happinessBuff = happinessBuff;
    }

    public double getMemberModifier() {
        return memberModifier;
    }

    public void setMemberModifier(double memberModifier) {
        this.memberModifier = memberModifier;
    }

    public double getRegionModifier() {
        return regionModifier;
    }

    public void setRegionModifier(double regionModifier) {
        this.regionModifier = regionModifier;
    }

    public double getManpowerModifier() {
        return manpowerModifier;
    }

    public void setManpowerModifier(double manpowerModifier) {
        this.manpowerModifier = manpowerModifier;
    }

    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestigeModifier) {
        this.prestige = prestigeModifier;
    }


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<Effect, Integer> getMinecraftEffects() {
        return minecraftEffects;
    }

    public void setMinecraftEffects(Map<Effect, Integer> minecraftEffects) {
        this.minecraftEffects = minecraftEffects;
    }

    public RegionType getChangeTypeTo() {
        return changeTypeTo;
    }

    public void setChangeTypeTo(RegionType changeTypeTo) {
        this.changeTypeTo = changeTypeTo;
    }

    public int getAllianceLimitBuff() {
        return allianceLimitBuff;
    }

    public void setAllianceLimitBuff(int allianceLimitBuff) {
        this.allianceLimitBuff = allianceLimitBuff;
    }

    public int getTransportShipLimit() {
        return transportShipLimit;
    }

    public void setTransportShipLimit(int transportShipLimit) {
        this.transportShipLimit = transportShipLimit;
    }

    public int getTransportAirshipLimit() {
        return transportAirshipLimit;
    }

    public void setTransportAirshipLimit(int transportAirshipLimit) {
        this.transportAirshipLimit = transportAirshipLimit;
    }

    public int getTransportCoachLimit() {
        return transportCoachLimit;
    }

    public void setTransportCoachLimit(int transportCoachLimit) {
        this.transportCoachLimit = transportCoachLimit;
    }

    public int getImportDailyLimit() {
        return importDailyLimit;
    }

    public void setImportDailyLimit(int importDailyLimit) {
        this.importDailyLimit = importDailyLimit;
    }

    public int getExportDailyLimit() {
        return exportDailyLimit;
    }

    public void setExportDailyLimit(int exportDailyLimit) {
        this.exportDailyLimit = exportDailyLimit;
    }

    public String getMemberPermission() {
        return memberPermission;
    }

    public void setMemberPermission(String memberPermission) {
        this.memberPermission = memberPermission;
    }

    public long getDuration() {
        return duration;
    }

    public BuildingEffect fromConfigSection(ConfigurationSection section) {
        displayName = (String) section.get("displayName");
        for (String key : section.getKeys(false)) {
            if (key.contains("consumptionModifier.")) {
                String name = key.replace("consumptionModifier.", "");
                consumptionModifier.put(Resource.getByID(name), section.getDouble(key, 0.0));
            }
            if (key.contains("productionModifier.")) {
                String name = key.replace("productionModifier.", "");
                productionModifier.put(Resource.getByID(name), section.getDouble(key, 0.0));
            }
            if (key.contains("virtualResourceProduction.")) {
                String name = key.replace("virtualResourceProduction.", "");
                virtualResourceProduction.put(Resource.getByID(name), section.getInt(key));
            }
            if (key.contains("physicalResourceProduction.")) {
                String name = key.replace("physicalResourceProduction.", "");
                physicalResourceProduction.put(Material.valueOf(name), section.getInt(key));
            }
            if (key.contains("effects.")) {
                String name = key.replace("effects.", "");
                minecraftEffects.put(Effect.valueOf(name), section.getInt(key));
            }
            if (key.contains("happiness.")) {
                String name = key.replace("happiness.", "");
                happinessBuff.put(PopulationLevel.valueOf(name), section.getInt(key));
            }
        }
        memberModifier = section.getDouble("members", 0.0);
        regionModifier = section.getDouble("regions", 0.0);
        manpowerModifier = section.getDouble("manpower", 0.0);
        prestige = section.getInt("prestige", 0);
        if (section.contains("type")) {
            changeTypeTo = RegionType.valueOf(section.getString("type"));
        }
        allianceLimitBuff = section.getInt("allianceLimit", 0);
        transportShipLimit = section.getInt("transportShipLimit", 0);
        transportCoachLimit = section.getInt("transportCoachLimit", 0);
        transportAirshipLimit = section.getInt("transportAirshipLimit", 0);
        importDailyLimit = section.getInt("importLimit", 0);
        exportDailyLimit = section.getInt("exportLimit", 0);
        memberPermission = section.getString("memberPermission", "");
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> args = new HashMap<>();
        args.put("displayName", displayName);
        for (Resource resource : consumptionModifier.keySet()) {
            args.put("consumption." + resource.getID(), consumptionModifier.get(resource));
        }
        for (Resource resource : productionModifier.keySet()) {
            args.put("production." + resource.getID(), productionModifier.get(resource));
        }
        for (PopulationLevel pop : happinessBuff.keySet()) {
            args.put("happiness." + pop, happinessBuff.get(pop));
        }
        for (Effect eff : minecraftEffects.keySet()) {
            args.put("effects." + eff, minecraftEffects.get(eff));
        }
        for (Resource resource : virtualResourceProduction.keySet()) {
            args.put("virtualResourceProduction." + resource.getID(), virtualResourceProduction.get(resource));
        }
        for (Material material : physicalResourceProduction.keySet()) {
            args.put("physicalResourceProduction." + material.name(), physicalResourceProduction.get(material));
        }

        args.put("members", memberModifier);
        args.put("regions", regionModifier);
        args.put("manpower", manpowerModifier);
        args.put("prestige", prestige);
        args.put("type", changeTypeTo);
        args.put("allianceLimit", allianceLimitBuff);
        args.put("transportShipLimit", transportShipLimit);
        args.put("transportCoachLimit", transportCoachLimit);
        args.put("transportAirshipLimit", transportAirshipLimit);
        args.put("permission", memberPermission);
        return args;
    }
}