package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "faction_money",
        description = "Checks if the player's faction has a certain amount of money in a specified currency.",
        shortExample = "faction_money: currency=herone; amount=1000",
        longExample = {
                "faction_money:",
                "  amount: 1000",
        })
public class FactionMoneyCondition extends FBaseCondition {

    @QParamDoc(name = "currency", description = "The currency to check the balance of.", def = "herone")
    private String currency = "herone";
    @QParamDoc(name = "amount", description = "The amount of money the faction must have.", required = true)
    private int amount;

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
        if (faction.getFAccount().getBalance(currency) >= amount) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        currency = cfg.getString("currency", "herone");
        amount = cfg.getInt("amount", 0);
    }
}
