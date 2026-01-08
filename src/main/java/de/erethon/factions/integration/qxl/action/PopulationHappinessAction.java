package de.erethon.factions.integration.qxl.action;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "population_happiness",
        description = "Changes the happiness of a population level in the player's faction.",
        shortExample = "population_happiness: level=citizen; happiness=5.0",
        longExample = {
                "population_happiness:",
                "  level: citizen",
                "  happiness: 5.0",
        }
)
public class PopulationHappinessAction extends FBaseAction {

    @QParamDoc(name = "level", description = "The population level to change the happiness of (e.g., citizen, beggar, patrician).", required = true)
    private PopulationLevel level;
    @QParamDoc(name = "happiness", description = "The amount to change the happiness by (can be negative).", required = true)
    private double happinessChange;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer == null) return;
        Faction faction = fPlayer.getFaction();
        if (faction == null) return;
        faction.setHappiness(level, faction.getHappiness(level) + happinessChange);
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String levelStr = cfg.getString("level", "citizen").toUpperCase();
        level = PopulationLevel.valueOf(levelStr);
        happinessChange = cfg.getDouble("happiness", 1.0);
    }
}
