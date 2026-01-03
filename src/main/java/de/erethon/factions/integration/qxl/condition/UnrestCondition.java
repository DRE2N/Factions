package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "faction_unrest",
        description = "Checks if the player's faction's unrest level is within the specified range.",
        shortExample = "faction_unrest: minimum=10 maximum=50",
        longExample = {
                "faction_unrest:",
                "  minimum: 10",
                "  maximum: 50",
        })
public class UnrestCondition extends FBaseCondition {

    @QParamDoc(name = "minimum", description = "The minimum unrest level (inclusive).")
    private int minimumUnrest;
    @QParamDoc(name = "maximum", description = "The maximum unrest level (inclusive).")
    private int maximumUnrest;

    @Override
    public boolean check(Quester quester) {
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer == null) {
            return fail(quester);
        }
        Faction faction = fPlayer.getFaction();
        if (faction == null) {
            return fail(quester);
        }
        int unrest = (int) faction.getUnrestLevel();
        if (unrest >= minimumUnrest && unrest <= maximumUnrest) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        minimumUnrest = cfg.getInt("minimum", 0);
        maximumUnrest = cfg.getInt("maximum", Integer.MAX_VALUE);
    }
}
