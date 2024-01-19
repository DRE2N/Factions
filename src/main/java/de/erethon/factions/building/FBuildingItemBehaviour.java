package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import de.erethon.hephaestus.HItem;
import de.erethon.hephaestus.HItemBehaviour;
import net.minecraft.world.item.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Malfrador
 */
public class FBuildingItemBehaviour extends HItemBehaviour {

    public static final NamespacedKey KEY = new NamespacedKey(Factions.get(), "building_item");
    private final Factions plugin = Factions.get();

    private Set<Player> waitingForConfirmation = new HashSet<>();

    public FBuildingItemBehaviour(HItem item) {
        super(item);
    }

    @Override
    public void onRightClick(ItemStack stack, Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (event.getClickedBlock() == null) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        if (!fPlayer.hasFaction())  {
            // Message
            return;
        }
        if (stack.getBukkitStack().getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.STRING)) {
            Building building = plugin.getBuildingManager().getById(stack.getBukkitStack().getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING));
            if (building == null) {
                // Message
                return;
            }
            boolean canBuild = building.checkRequirements(player, fPlayer.getFaction(), player.getLocation());
            if (canBuild && waitingForConfirmation.contains(player)) {
                waitingForConfirmation.remove(player);
                building.build(player, fPlayer.getFaction(), fPlayer.getLastRegion(), event.getClickedBlock().getLocation());
                // Message
                return;
            }
            waitingForConfirmation.add(player);
            building.displayFrame(player, event.getClickedBlock().getLocation(), canBuild);
        }
    }

    @Override
    public void onLeftClick(ItemStack stack, Player player, PlayerInteractEvent event) {
        event.setCancelled(true);
        waitingForConfirmation.remove(player);
        // Message
    }
}
