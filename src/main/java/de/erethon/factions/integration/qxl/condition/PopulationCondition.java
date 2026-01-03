package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

import java.util.Map;

@QLoadableDoc(
        value = "faction_population",
        description = "Checks if the faction's population for a level is at least X. Fails too if the quester has no faction.",
        shortExample = "faction_population: beggar=10",
        longExample = {
                "faction_population",
                "  citizen: 300",
                "  noblemen: 20",
                "  patrician: 96",
        })
public class PopulationCondition extends FBaseCondition {

    @QParamDoc(name = "<level> = amount", description = "Population levels are specified by using the level name as a key, e.g. `population: beggar=10; noblemen=5;` ", required = true)
    private Map<PopulationLevel, Integer> populationMap;

    @Override
    public boolean check(Quester quester) {
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer == null) {
            return fail(quester);
        }
        Faction faction = fPlayer.getFaction();
        if  (faction == null) {
            return fail(quester);
        }
        for (Map.Entry<PopulationLevel, Integer> entry : populationMap.entrySet()) {
            PopulationLevel level = entry.getKey();
            int requiredPopulation = entry.getValue();
            int factionPopulation = faction.getPopulation(level);
            if (factionPopulation < requiredPopulation) {
                return fail(quester);
            }
        }
        return success(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        for (PopulationLevel level : PopulationLevel.values()) {
            String key = level.name().toLowerCase();
            if (cfg.contains(key)) {
                int value = cfg.getInt(key);
                populationMap.put(level, value);
            }
        }
    }
}
