package de.erethon.factions.integration.qxl.objective;

import de.erethon.factions.event.FactionCreateEvent;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.objective.ActiveObjective;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "faction_create",
        description = "Create a faction.",
        shortExample = "faction_create:",
        longExample = {
                "faction_create:"
        }
)
public class FactionCreateObjective extends FBaseObjective<FactionCreateEvent> {

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Create a Faction; de=Erstelle eine Fraktion");
    }

    @Override
    public Class<FactionCreateEvent> getEventType() {
        return FactionCreateEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, FactionCreateEvent event) {
        if (!conditions(event.getFPlayer().getPlayer())) {
            return;
        }
        checkCompletion(activeObjective, this);
    }
}



