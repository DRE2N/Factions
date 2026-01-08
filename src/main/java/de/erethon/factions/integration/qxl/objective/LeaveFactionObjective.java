package de.erethon.factions.integration.qxl.objective;

import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.objective.ActiveObjective;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "faction_leave",
        description = "Objective is completed when the player leaves a faction.",
        shortExample = "faction_leave:",
        longExample = {
                "faction_leave:"
        }
)
public class LeaveFactionObjective extends FBaseObjective<FPlayerFactionLeaveEvent> {

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Leave a Faction; de=Verlasse eine Fraktion");
    }

    @Override
    public Class<FPlayerFactionLeaveEvent> getEventType() {
        return FPlayerFactionLeaveEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, FPlayerFactionLeaveEvent event) {
        if (!conditions(event.getFPlayer().getPlayer())) return;
        complete(activeObjective.getHolder(), this, getQPlayer(event.getFPlayer()));
    }
}
