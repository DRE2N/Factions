package de.erethon.factions.integration.qxl.objective;

import de.erethon.factions.event.FPlayerFactionJoinEvent;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.objective.ActiveObjective;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "faction_join",
        description = "Objective is completed when the player joins a faction.",
        shortExample = "faction_join:",
        longExample = {
                "faction_join:"
        }
)
public class JoinFactionObjective extends FBaseObjective<FPlayerFactionJoinEvent> {

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Join a Faction; de=Tritt einer Fraktion bei");
    }

    @Override
    public Class<FPlayerFactionJoinEvent> getEventType() {
        return FPlayerFactionJoinEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, FPlayerFactionJoinEvent event) {
        if (!conditions(event.getFPlayer().getPlayer())) return;
        complete(activeObjective.getHolder(), this, getQPlayer(event.getFPlayer()));
    }
}
