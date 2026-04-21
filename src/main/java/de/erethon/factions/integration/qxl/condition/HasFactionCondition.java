package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "has_faction",
        description = "Checks if the player is in any faction.",
        shortExample = "has_faction:",
        longExample = {
                "has_faction:"
        }
)
public class HasFactionCondition extends FBaseCondition{

    @Override
    public boolean checkInternal(Quester quester) {
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer != null) {
            if (fPlayer.getFaction() != null) {
                return success(quester);
            }
        }
        return fail(quester);
    }
}
