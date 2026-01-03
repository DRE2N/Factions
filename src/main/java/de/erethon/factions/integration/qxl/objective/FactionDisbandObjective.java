package de.erethon.factions.integration.qxl.objective;

import de.erethon.factions.event.FactionDisbandEvent;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.objective.ActiveObjective;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "faction_disband",
        description = "Disband a faction.",
        shortExample = "faction_disband:",
        longExample = {
                "faction_disband:"
        }
)
public class FactionDisbandObjective extends FBaseObjective<FactionDisbandEvent> {

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Disband a Faction; de=Löse eine Fraktion auf");
    }

    @Override
    public Class<FactionDisbandEvent> getEventType() {
        return FactionDisbandEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, FactionDisbandEvent event) {
        Player player = Bukkit.getPlayer(event.getFaction().getAdmin());
        if (player == null || !conditions(player)) {
            return;
        }
        checkCompletion(activeObjective, this);
    }
}



