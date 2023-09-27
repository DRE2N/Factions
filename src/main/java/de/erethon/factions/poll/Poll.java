package de.erethon.factions.poll;

import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;

/**
 * @author Fyreum
 */
public abstract class Poll<V> implements Listener {

    public static final long DEFAULT_DURATION = TickUtil.HOUR;
    public static final int ITEMS_PER_PAGE = 45;
    public static final NamespacedKey POLL_ITEM_ID_KEY = new NamespacedKey(Factions.get(), "pollItemId");
    public static final ItemStack NEXT_PAGE_BUTTON = buildFillItemStack(Material.PAPER, FMessage.GUI_POLL_NEXT_PAGE_NAME.message(), FMessage.GUI_POLL_NEXT_PAGE_INFO.itemMessage());
    public static final ItemStack PREVIOUS_PAGE_BUTTON = buildFillItemStack(Material.PAPER, FMessage.GUI_POLL_PREVIOUS_PAGE_NAME.message(), FMessage.GUI_POLL_PREVIOUS_PAGE_INFO.itemMessage());

    protected final Factions plugin = Factions.get();
    protected final String name;
    protected final PollScope scope;
    protected final TreeSet<PollEntry> entries;
    protected final Function<V, ItemStack> subjectConverter;
    protected final Map<Integer, Inventory> inventories = new HashMap<>();
    protected boolean open = false;
    protected long closeTime;
    protected BukkitTask closeTask = null;

    public Poll(@NotNull String name, @NotNull PollScope scope, @NotNull Collection<@NotNull V> entries, @NotNull Function<V, ItemStack> subjectConverter) {
        this(name, scope, entries, subjectConverter, null);
    }

    public Poll(@NotNull String name, @NotNull PollScope scope, @NotNull Collection<@NotNull V> entries, @NotNull Function<V, ItemStack> subjectConverter, @Nullable Comparator<V> comparator) {
        assert !name.contains(" ") : "Illegal name found: whitespaces are not allowed";
        this.name = name;
        this.scope = scope;
        Comparator<PollEntry> comp = comparator == null ? Comparator.naturalOrder() : (o1, o2) -> {
            int weightCompare = Integer.compare(o1.getTotalWeight(), o2.getTotalWeight());
            return weightCompare == 0 ? comparator.compare(o1.getSubject(), o2.getSubject()) : weightCompare;
        };
        this.entries = new TreeSet<>(comp);
        this.subjectConverter = subjectConverter;
        for (V subject : entries) {
            this.entries.add(new PollEntry(subject));
        }
    }

    public Poll(@NotNull ConfigurationSection config, @NotNull Function<V, ItemStack> subjectConverter) throws IllegalArgumentException {
        this.name = config.getString("name");
        this.scope = PollScope.valueOf(config.getString("scope"));
        this.open = config.getBoolean("open");
        this.closeTime = config.getLong("closeTime");
        this.entries = new TreeSet<>();
        this.subjectConverter = subjectConverter;

        ConfigurationSection entriesSection = config.getConfigurationSection("entries");
        if (entriesSection != null) {
            for (String key : entriesSection.getKeys(false)) {
                V subject = idToSubject(key);
                if (subject == null) {
                    FLogger.ERROR.log("Unknown subject in poll '" + name + "' found: " + key);
                    continue;
                }
                PollEntry entry = new PollEntry(subject);
                ConfigurationSection votesSection = entriesSection.getConfigurationSection(key);
                if (votesSection != null) {
                    for (String uuidString : votesSection.getKeys(false)) {
                        UUID uuid = UUID.fromString(uuidString);
                        int weight = votesSection.getInt(uuidString);
                        entry.addVote(uuid, weight);
                    }
                }
                entries.add(entry);
            }
        }
    }

    public void openPoll() {
        openPoll(DEFAULT_DURATION);
    }

    public void openPoll(long duration) {
        open = true;
        buildInventories();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (duration > 0) {
            closeTime = System.currentTimeMillis() + duration;
            closeTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::closePoll, duration);
        }
    }

    public void closePoll() {
        open = false;
        closeTime = System.currentTimeMillis();
        if (closeTask != null) {
            closeTask.cancel();
            closeTask = null;
        }
        onResult(new TreeSet<>(entries));
        buildInventories();
    }

    public void show(@NotNull HumanEntity player, int page) {
        assert inventories.containsKey(page) : "Page not found";
        player.openInventory(inventories.get(page));
    }

    public void showIfExistent(@NotNull HumanEntity player, int page) {
        try {
            show(player, page);
        } catch (Exception ignored) {
        }
    }

    private void buildInventories() {
        int page = 0;
        while (page * ITEMS_PER_PAGE <= entries.size()) {
            buildInventory(page++);
        }
    }

    private void buildInventory(int page) {
        List<PollEntry> entriesCopy = new ArrayList<>(entries).subList(page * ITEMS_PER_PAGE, Math.min((page + 1) * ITEMS_PER_PAGE, entries.size()));
        Inventory inventory = Bukkit.createInventory(null, 54, open ? FMessage.GUI_POLL_TITLE.message(name) : FMessage.GUI_POLL_TITLE_CLOSED.message(name));
        ItemStack[] contents = new ItemStack[54];

        for (int i = 0; i < entriesCopy.size(); i++) {
            PollEntry entry = entriesCopy.get(i);
            contents[i] = entry.itemStack;
        }
        contents[47] = PREVIOUS_PAGE_BUTTON;
        contents[49] = new ItemStack(Material.STICK);
        contents[51] = NEXT_PAGE_BUTTON;
        for (int i = 45; i < 54; i++) {
            if (contents[i] == null) {
                contents[i] = buildFillItemStack(Material.BLACK_STAINED_GLASS, MessageUtil.parse("<black>"));
            }
        }
        inventory.setContents(contents);
        inventories.put(page, inventory);
    }

    /* Listeners */

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        int page = getPage(event.getInventory());
        if (page == -1) {
            return;
        }
        event.setCancelled(true);
        if (!open) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer((Player) event.getWhoClicked());

        if (hasParticipated(fPlayer) || !canParticipate(fPlayer)) {
            return;
        }
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }
        if (itemStack.equals(PREVIOUS_PAGE_BUTTON)) {
            showIfExistent(event.getWhoClicked(), page + 1);
            return;
        }
        if (itemStack.equals(NEXT_PAGE_BUTTON)) {
            showIfExistent(event.getWhoClicked(), page - 1);
            return;
        }
        PollEntry entry = getEntry(itemStack);
        if (entry == null) {
            return;
        }
        entry.addVote(fPlayer.getUniqueId(), getVotingWeight(fPlayer));
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent event) {
        int page = getPage(event.getInventory());
        if (page == -1) {
            return;
        }
        event.setCancelled(true);
    }

    private int getPage(Inventory inventory) {
        int page = -1;
        // Search for a corresponding inventory.
        for (int pageNr : inventories.keySet()) {
            if (inventories.get(pageNr) == inventory) {
                page = pageNr;
                break;
            }
        }
        return page;
    }

    private PollEntry getEntry(ItemStack itemStack) {
        String itemId = getItemId(itemStack);
        if (itemId == null) {
            return null;
        }
        for (PollEntry entry : entries) {
            if (getItemId(entry.itemStack).equals(itemId)) {
                return entry;
            }
        }
        return null;
    }

    private String getItemId(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().get(POLL_ITEM_ID_KEY, PersistentDataType.STRING);
    }

    /* Serialization */

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("type", getClass().getSimpleName());
        serialized.put("name", name);
        serialized.put("scope", scope.name());
        serialized.put("open", open);
        serialized.put("closeTime", closeTime);
        Map<String, Object> serializedEntries = new HashMap<>(entries.size());
        for (PollEntry entry : entries) {
            Map<String, Object> serializedVotes = new HashMap<>(entry.votes.size());
            for (UUID uuid : entry.votes.keySet()) {
                serializedVotes.put(uuid.toString(), entry.votes.get(uuid));
            }
            serializedEntries.put(String.valueOf(subjectToId(entry.getSubject())), serializedVotes);
        }
        serialized.put("entries", serializedEntries);
        return serialized;
    }

    /* Abstracts */

    protected abstract @NotNull Object subjectToId(@NotNull V subject);

    protected abstract @Nullable V idToSubject(@NotNull Object id);

    protected abstract void onResult(@NotNull TreeSet<PollEntry> results);

    public abstract boolean canParticipate(@NotNull FPlayer fPlayer);

    /* Getters */

    public boolean hasParticipated(@NotNull FPlayer fPlayer) {
        for (PollEntry entry : entries) {
            if (entry.containsVote(fPlayer.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public int getVotingWeight(@NotNull FPlayer fPlayer) {
        return 1;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull PollScope getScope() {
        return scope;
    }

    public @NotNull TreeSet<PollEntry> getEntries() {
        return entries;
    }

    public boolean isOpen() {
        return open;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public @Nullable BukkitTask getCloseTask() {
        return closeTask;
    }

    /* Statics */

    private static ItemStack buildFillItemStack(Material material, Component name) {
        return buildFillItemStack(material, name, null);
    }

    private static ItemStack buildFillItemStack(Material material, Component name, Component info) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(name);
        if (info != null) {
            meta.lore(List.of(info));
        }
        meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /* Sub classes */

    public class PollEntry implements Comparable<PollEntry> {

        private final V subject;
        private final Map<UUID, Integer> votes = new HashMap<>();
        private final ItemStack itemStack;
        private final ItemMeta meta;
        private int totalWeight = 0;

        public PollEntry(V subject) {
            this.subject = subject;
            this.itemStack = subjectConverter.apply(subject);
            this.meta = itemStack.getItemMeta();
            // Store random UUID, to guarantee unique items.
            meta.getPersistentDataContainer().set(POLL_ITEM_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            displayVotes(false);
        }

        public V getSubject() {
            return subject;
        }

        public @NotNull Map<UUID, Integer> getVotes() {
            return new HashMap<>(votes);
        }

        public void addVote(@NotNull UUID uuid, int weight) {
            votes.put(uuid, weight);
            totalWeight += weight;
            displayVotes(true);
        }

        public void removeVote(@NotNull UUID uuid) {
            Integer weight = votes.remove(uuid);
            if (weight == null) {
                return;
            }
            totalWeight -= weight;
            displayVotes(true);
        }

        private void displayVotes(boolean deletePrevious) {
            List<Component> lore = meta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            } else if (deletePrevious) {
                lore.remove(lore.size() - 1);
            } else {
                lore.add(Component.empty());
            }
            lore.add(FMessage.GUI_POLL_VOTES_DISPLAY.itemMessage(String.valueOf(totalWeight)));
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }

        /* Getters */

        public boolean containsVote(@NotNull UUID uuid) {
            return votes.containsKey(uuid);
        }

        public int getTotalWeight() {
            return totalWeight;
        }

        @Override
        public int compareTo(PollEntry other) {
            return Integer.compare(totalWeight, other.totalWeight);
        }
    }

}
