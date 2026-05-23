package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.war.WarPhase;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "war_phase",
        description = "Checks if the current war phase matches the specified phase.",
        shortExample = "war_phase: phase=scoring",
        longExample = {
                "war_phase:",
                "  phase: peace",
        }
)
public class WarPhaseCondition extends FBaseCondition {

    @QParamDoc(name = "phase", description = "The war phase to check for. One of `capital`, `truce`, `scoring`, `peace`", required = true)
    private WarPhase warPhase;

    @Override
    public boolean checkInternal(Quester quester) {
        if (factions.getWar() == null) {
            return fail(quester);
        }
        if (factions.getWar().getCurrentPhase() == warPhase) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String phaseName = cfg.getString("phase").toUpperCase();
        warPhase = WarPhase.valueOf(phaseName);
    }
}
