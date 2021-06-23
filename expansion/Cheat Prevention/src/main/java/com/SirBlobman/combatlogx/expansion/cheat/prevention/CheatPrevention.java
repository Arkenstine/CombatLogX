package com.SirBlobman.combatlogx.expansion.cheat.prevention;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.utility.VersionUtility;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.expansion.Expansion;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerBlocks;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerChat;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerCommandBlocker;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerElytra;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerEntityInteraction;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerEssentials;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerFlight;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerGameMode;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerInventories;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerLegacyItemPickup;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerNewItemPickup;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerPotions;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerRiptide;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerTeleport;
import com.SirBlobman.combatlogx.expansion.cheat.prevention.listener.ListenerTotemOfUndying;

public class CheatPrevention extends Expansion {
    public CheatPrevention(ICombatLogX plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        saveDefaultConfig("cheat-prevention.yml");
    }

    @Override
    public void reloadConfig() {
        reloadConfig("cheat-prevention.yml");
    }

    @Override
    public void onEnable() {
        PluginManager manager = Bukkit.getPluginManager();
        ICombatLogX plugin = getPlugin();
        JavaPlugin javaPlugin = plugin.getPlugin();
        int minorVersion = VersionUtility.getMinorVersion();

        // All Versions
        manager.registerEvents(new ListenerBlocks(this), javaPlugin);
        manager.registerEvents(new ListenerChat(this), javaPlugin);
        manager.registerEvents(new ListenerCommandBlocker(this), javaPlugin);
        manager.registerEvents(new ListenerEntityInteraction(this), javaPlugin);
        manager.registerEvents(new ListenerFlight(this), javaPlugin);
        manager.registerEvents(new ListenerGameMode(this), javaPlugin);
        manager.registerEvents(new ListenerInventories(this), javaPlugin);
        manager.registerEvents(new ListenerPotions(this), javaPlugin);
        manager.registerEvents(new ListenerTeleport(this), javaPlugin);

        // 1.9+ Elytra
        if(minorVersion >= 9) manager.registerEvents(new ListenerElytra(this), javaPlugin);

        // 1.11+ Totem of Undying
        if(minorVersion >= 11) manager.registerEvents(new ListenerTotemOfUndying(this), javaPlugin);

        // 1.12+ PlayerPickupItemEvent --> EntityPickupItemEvent
        Listener itemPickupListener = (minorVersion >= 12 ? new ListenerNewItemPickup(this) : new ListenerLegacyItemPickup(this));
        manager.registerEvents(itemPickupListener, javaPlugin);

        // 1.13+ Riptide Enchantment
        if(minorVersion >= 13) manager.registerEvents(new ListenerRiptide(this), javaPlugin);
        
        // Essentials Hook
        if(manager.isPluginEnabled("Essentials")) {
            ListenerEssentials listenerEssentials = new ListenerEssentials(this);
            manager.registerEvents(listenerEssentials, javaPlugin);
        }
    }

    @Override
    public void onDisable() {
        // Do Nothing
    }
}
