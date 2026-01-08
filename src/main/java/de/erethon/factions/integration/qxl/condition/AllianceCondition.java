package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "alliance",
        description = "Checks if the player is in a specific alliance or in any alliance.",
        shortExample = "alliance: alliance=1",
        longExample = {
                "alliance:",
                "  any: true",
        }
)
public class AllianceCondition extends FBaseCondition {

    @QParamDoc(name = "alliance", description = "The ID of the alliance the player must be in.")
    private Alliance alliance;
    @QParamDoc(name = "any", description = "If true, the player can be in any alliance. `alliance` is not required if this is set to `true`.")
    private boolean anyAlliance = false;

    @Override
    public boolean check(Quester quester) {
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer != null) {
            if (fPlayer.getAlliance() != null && (anyAlliance || fPlayer.getAlliance().equals(alliance))) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        anyAlliance = cfg.getBoolean("any", false);
        if (anyAlliance) {
            return;
        }
        int allianceId = cfg.getInt("alliance");
        alliance = allianceCache.getById(allianceId);
        if (alliance == null) {
            throw new RuntimeException("Alliance with ID " + allianceId + " does not exist. Make sure to use the numerical ID");
        }
    }
}
