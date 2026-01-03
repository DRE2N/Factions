package de.erethon.factions.integration;

import de.erethon.aergia.Aergia;
import de.erethon.aergia.event.DiscordPlayerVerifiedEvent;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.event.FPlayerChangeAllianceEvent;
import de.erethon.factions.event.FPlayerFactionJoinEvent;
import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.factions.event.FactionDisbandEvent;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

/**
 * @author Fyreum
 */
public class DiscordBotListener implements Listener {

    final Aergia aergia = Aergia.inst();

    @EventHandler
    public void onVerification(DiscordPlayerVerifiedEvent event) {
        FPlayer fPlayer = Factions.get().getFPlayerCache().getByUniqueId(event.getEPlayer().getUniqueId());
        if (fPlayer == null || !fPlayer.hasAlliance()) {
            return;
        }
        addAllianceRoles(fPlayer);
        if (!fPlayer.hasFaction()) {
            return;
        }
        addFactionRolesAndChannels(fPlayer);
    }

    @EventHandler
    public void onAllianceChange(FPlayerChangeAllianceEvent event) {
        if (event.getOldAlliance() != null) {
            Guild guild = aergia.getDiscordBot().getGuild();
            Role role = guild.getRoleById(event.getOldAlliance().getDiscordRoleId());
            if (role != null) {
                guild.removeRoleFromMember(UserSnowflake.fromId(event.getFPlayer().getEPlayer().getDiscordId()), role).queue();
            }
        }
        addAllianceRoles(event.getFPlayer());
    }

    @EventHandler
    public void onFactionJoin(FPlayerFactionJoinEvent event) { // includes faction creation
        addFactionRolesAndChannels(event.getFPlayer());
    }

    @EventHandler
    public void onFactionLeave(FPlayerFactionLeaveEvent event) {
        FPlayer fPlayer = event.getFPlayer();
        Guild guild = aergia.getDiscordBot().getGuild();
        Role role = guild.getRoleById(fPlayer.getFaction().getDiscordRoleId());
        if (role == null) {
            return;
        }
        guild.removeRoleFromMember(UserSnowflake.fromId(fPlayer.getEPlayer().getDiscordId()), role).queue();
    }

    @EventHandler
    public void onFactionDisband(FactionDisbandEvent event) {
        Faction faction = event.getFaction();
        Guild guild = aergia.getDiscordBot().getGuild();
        Role role = guild.getRoleById(faction.getDiscordRoleId());

        if (role == null) {
            return;
        }
        role.delete().queue();
        VoiceChannel voiceChannel = guild.getVoiceChannelById(faction.getDiscordVoiceChannelId());

        if (voiceChannel != null) {
            voiceChannel.delete().queue();
        }
        Alliance alliance = faction.getAlliance();
        getOrCreateArchiveCategory(guild, alliance.getDiscordArchiveCategoryId(), alliance.getName() + "-archive").thenAccept(category -> {
            TextChannel textChannel = guild.getTextChannelById(faction.getDiscordTextChannelId());
            if (textChannel != null) {
                textChannel.getManager().setParent(category).queue();
            }
        });
    }

    private void addAllianceRoles(FPlayer fPlayer) {
        Guild guild = aergia.getDiscordBot().getGuild();
        Role role = guild.getRoleById(fPlayer.getAlliance().getDiscordRoleId());
        if (role != null) {
            guild.addRoleToMember(UserSnowflake.fromId(fPlayer.getEPlayer().getDiscordId()), role).queue();
            return;
        }
        guild.createRole()
                .setName(fPlayer.getAlliance().getName())
                .setColor(fPlayer.getAlliance().getColor().value())
                .queue(created -> {
                    guild.addRoleToMember(UserSnowflake.fromId(fPlayer.getEPlayer().getDiscordId()), created).queue();
                    getOrCreateCategory(guild, fPlayer.getAlliance().getDiscordCategoryId(), created.getIdLong(), fPlayer.getAlliance().getName());
                });
    }

    private void addFactionRolesAndChannels(FPlayer fPlayer) {
        Guild guild = aergia.getDiscordBot().getGuild();
        Role role = guild.getRoleById(fPlayer.getFaction().getDiscordRoleId());
        if (role != null) {
            guild.addRoleToMember(UserSnowflake.fromId(fPlayer.getEPlayer().getDiscordId()), role).queue();
            return;
        }
        guild.createRole()
                .setName(fPlayer.getFaction().getName())
                .queue(created -> {
                    // If the role doesn't exist we need to create the channels too
                    createChannels(guild, created, fPlayer.getFaction());
                    guild.addRoleToMember(UserSnowflake.fromId(fPlayer.getEPlayer().getDiscordId()), created).queue();
                });
    }

    void createChannels(Guild guild, Role role, Faction faction) {
        Alliance alliance = faction.getAlliance();
        getOrCreateCategory(guild, alliance.getDiscordCategoryId(), alliance.getDiscordRoleId(), alliance.getName()).thenAccept(category -> {
            category.createTextChannel(faction.getName())
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .queue();
            category.createVoiceChannel(faction.getName())
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .queue();
        });
    }

    CompletableFuture<Category> getOrCreateCategory(Guild guild, long id, long allianceRole, String name) {
        Category category = guild.getCategoryById(id);
        if (category != null) {
            return CompletableFuture.completedFuture(category);
        }
        CompletableFuture<Category> future = new CompletableFuture<>();
        guild.createCategory(name)
                .addRolePermissionOverride(allianceRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .queue(future::complete);
        return future;
    }

    CompletableFuture<Category> getOrCreateArchiveCategory(Guild guild, long id, String name) {
        Category category = guild.getCategoryById(id);
        if (category != null) {
            return CompletableFuture.completedFuture(category);
        }
        CompletableFuture<Category> future = new CompletableFuture<>();
        guild.createCategory(name)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(future::complete);
        return future;
    }

}
