package de.erethon.factions.integration.qxl.objective;


import de.erethon.factions.event.FPlayerChangeAllianceEvent;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.component.objective.ActiveObjective;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "change_alliance",
        description = "Objective completed when the player changes their alliance or initially selects one.",
        shortExample = "change_alliance:",
        longExample = {
                "change_alliance:"
        }
)
public class ChangeAllianceObjective extends FBaseObjective<FPlayerChangeAllianceEvent> {

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Change Alliance; de=Allianz wechseln");
    }

    @Override
    public Class<FPlayerChangeAllianceEvent> getEventType() {
        return FPlayerChangeAllianceEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, FPlayerChangeAllianceEvent event) {
        if (!conditions(event.getFPlayer().getPlayer())) {
            return;
        }
        checkCompletion(activeObjective, this, getQPlayer(event.getFPlayer()));
    }
}
