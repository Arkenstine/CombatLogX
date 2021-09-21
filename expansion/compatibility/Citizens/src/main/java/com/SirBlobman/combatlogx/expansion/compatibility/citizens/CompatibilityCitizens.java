package com.SirBlobman.combatlogx.expansion.compatibility.citizens;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

import com.github.sirblobman.api.utility.VersionUtility;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.expansion.Expansion;
import com.SirBlobman.combatlogx.api.expansion.ExpansionManager;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener.ListenerCombat;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener.ListenerDamageDeath;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener.ListenerLogin;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener.ListenerNPCMove;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener.ListenerPunish;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener.ListenerResurrect;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.manager.NPCManager;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.manager.SentinelManager;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.manager.enemystorage.EnemyStorageManager;

public class CompatibilityCitizens extends Expansion {
    private NPCManager npcManager;
    private SentinelManager sentinelManager;
    private EnemyStorageManager enemyStorageManager;
    public ListenerNPCMove npcMoveListener;

    public CompatibilityCitizens(ICombatLogX plugin) {
        super(plugin);
        this.npcManager = null;
        this.sentinelManager = null;
        this.enemyStorageManager = null;
        this.npcMoveListener = null;
    }
    
    @Override
    public void onLoad() {
        saveDefaultConfig("citizens-compatibility.yml");
    }

    @Override
    public void reloadConfig() {
        reloadConfig("citizens-compatibility.yml");
    }
    
    @Override
    public void onEnable() {
        Logger logger = getLogger();
        ICombatLogX plugin = getPlugin();
        ExpansionManager expansionManager = plugin.getExpansionManager();
        
        if(checkForCitizens()) {
            logger.info("Could not find the Citizens plugin.");
            logger.info("This expansion will be automatically disabled.");
            expansionManager.disableExpansion(this);
            return;
        }

        this.enemyStorageManager = new EnemyStorageManager();
        
        this.npcManager = new NPCManager(this);
        this.npcManager.registerTrait();
        
        if(checkForSentinel()) {
            this.sentinelManager = new SentinelManager(this);
            this.sentinelManager.onEnable();
        }
        
        expansionManager.registerListener(this, new ListenerCombat(this));
        expansionManager.registerListener(this, new ListenerDamageDeath(this));
        expansionManager.registerListener(this, new ListenerLogin(this));
        expansionManager.registerListener(this, new ListenerPunish(this));
        npcMoveListener = new ListenerNPCMove(this);

        // 1.11+ Totem of Undying
        int minorVersion = VersionUtility.getMinorVersion();
        if(minorVersion >= 11) expansionManager.registerListener(this, new ListenerResurrect(this));
    }
    
    @Override
    public void onDisable() {
        if(this.npcManager == null) return;
        this.npcManager.onDisable();
    }
    
    public NPCManager getNPCManager() {
        return this.npcManager;
    }
    
    public SentinelManager getSentinelManager() {
        return this.sentinelManager;
    }

    public EnemyStorageManager getEnemyStorageManager() {
        return this.enemyStorageManager;
    }

    private boolean checkForCitizens() {
        PluginManager manager = Bukkit.getPluginManager();
        if(!manager.isPluginEnabled("Citizens")) return true;
        
        Plugin plugin = manager.getPlugin("Citizens");
        if(plugin == null) return true;
        
        PluginDescriptionFile description = plugin.getDescription();
        String fullName = description.getFullName();
        
        Logger logger = getLogger();
        logger.info("Successfully hooked into " + fullName);
        return false;
    }
    
    private boolean checkForSentinel() {
        PluginManager manager = Bukkit.getPluginManager();
        if(!manager.isPluginEnabled("Sentinel")) return false;
        
        Plugin plugin = manager.getPlugin("Sentinel");
        if(plugin == null) return false;
        
        PluginDescriptionFile description = plugin.getDescription();
        String fullName = description.getFullName();
        
        Logger logger = getLogger();
        logger.info("Successfully hooked into " + fullName);
        return true;
    }
}
