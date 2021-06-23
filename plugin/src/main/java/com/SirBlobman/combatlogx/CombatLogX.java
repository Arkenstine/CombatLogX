package com.SirBlobman.combatlogx;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.configuration.PlayerDataManager;
import com.github.sirblobman.api.plugin.ConfigurablePlugin;
import com.github.sirblobman.api.utility.MessageUtility;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.event.PlayerUntagEvent;
import com.SirBlobman.combatlogx.api.expansion.ExpansionManager;
import com.SirBlobman.combatlogx.api.listener.ICustomDeathListener;
import com.SirBlobman.combatlogx.command.CommandCombatLogX;
import com.SirBlobman.combatlogx.command.CommandCombatTimer;
import com.SirBlobman.combatlogx.command.CustomCommand;
import com.SirBlobman.combatlogx.listener.*;
import com.SirBlobman.combatlogx.manager.CombatManager;
import com.SirBlobman.combatlogx.manager.LanguageManager;
import com.SirBlobman.combatlogx.update.ConfigChecker;
import com.SirBlobman.combatlogx.update.UpdateChecker;

public class CombatLogX extends ConfigurablePlugin implements ICombatLogX {
    private final CombatManager combatManager;
    private final ExpansionManager expansionManager;
    private final LanguageManager languageManager;
    private final ICustomDeathListener customDeathListener;
    private final UpdateChecker updateChecker;
    
    public CombatLogX() {
        this.combatManager = new CombatManager(this);
        this.expansionManager = new ExpansionManager(this);
        this.languageManager = new LanguageManager(this);
        this.customDeathListener = new ListenerCustomDeath(this);
        this.updateChecker = new UpdateChecker(this);
    }

    @Override
    public CombatLogX getPlugin() {
        return this;
    }

    @Override
    public void onLoad() {
        ConfigChecker configChecker = new ConfigChecker(this);
        configChecker.checkConfig();
        
        saveDefaultConfig("config.yml");
        saveDefaultConfig("language/en_us.yml");
        
        ExpansionManager expansionManager = getExpansionManager();
        expansionManager.loadExpansions();
        broadcastLoadMessage();
    }

    @Override
    public void onEnable() {
        registerListeners();
        registerCommands();
        registerTasks();
    
        ExpansionManager expansionManager = getExpansionManager();
        expansionManager.enableExpansions();
        broadcastEnableMessage();
    
        UpdateChecker updateChecker = getUpdateChecker();
        updateChecker.checkForUpdates();
    }

    @Override
    public void onDisable() {
        untagAllPlayers();
        
        ExpansionManager expansionManager = getExpansionManager();
        expansionManager.disableExpansions();
        
        broadcastDisableMessage();
    }

    @Override
    public void registerCommand(String commandName, CommandExecutor executor, String description, String usage, String... aliases) {
        PluginCommand command = getCommand(commandName);
        if(command == null) {
            forceRegisterCommand(commandName, executor, description, usage, aliases);
            return;
        }
        command.setExecutor(executor);

        if(executor instanceof TabCompleter) {
            TabCompleter completer = (TabCompleter) executor;
            command.setTabCompleter(completer);
        }

        if(executor instanceof Listener) {
            Listener listener = (Listener) executor;
            PluginManager manager = Bukkit.getPluginManager();
            manager.registerEvents(listener, this);
        }
    }

    @Override
    public YamlConfiguration getConfig(String fileName) {
        ConfigurationManager configurationManager = getConfigurationManager();
        return configurationManager.get(fileName);
    }

    @Override
    public void reloadConfig(String fileName) {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.reload(fileName);
    }

    @Override
    public void saveConfig(String fileName) {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.save(fileName);
    }

    @Override
    public void saveDefaultConfig(String fileName) {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.saveDefault(fileName);
    }

    @Override
    public YamlConfiguration getDataFile(OfflinePlayer user) {
        PlayerDataManager playerDataManager = getPlayerDataManager();
        return playerDataManager.get(user);
    }

    @Override
    public void saveDataFile(OfflinePlayer user) {
        PlayerDataManager playerDataManager = getPlayerDataManager();
        playerDataManager.save(user);
    }

    @Override
    public ClassLoader getPluginClassLoader() {
        return super.getClassLoader();
    }

    @Override
    public CombatManager getCombatManager() {
        return this.combatManager;
    }
    
    @Override
    public LanguageManager getCombatLogXLanguageManager() {
        return this.languageManager;
    }
    
    @Override
    public ExpansionManager getExpansionManager() {
        return this.expansionManager;
    }

    @Override
    public ICustomDeathListener getCustomDeathListener() {
        return this.customDeathListener;
    }
    
    public UpdateChecker getUpdateChecker() {
        return this.updateChecker;
    }
    
    private void forceRegisterCommand(String commandName, CommandExecutor executor, String description, String usage, String... aliases) {
        if(commandName == null || executor == null || description == null || usage == null || aliases == null) return;
        PluginManager manager = Bukkit.getPluginManager();
        
        CustomCommand command = new CustomCommand(commandName, executor, description, usage, aliases);
        manager.registerEvents(command, this);
        
        if(executor instanceof Listener) {
            Listener listener = (Listener) executor;
            manager.registerEvents(listener, this);
        }
    }
    
    private void registerCommand(String commandName, CommandExecutor executor) {
        registerCommand(commandName, executor, null, null);
    }

    private void broadcastMessage(String message) {
        if(message == null || message.isEmpty()) return;
        String color = MessageUtility.color(message);

        Logger logger = getLogger();
        logger.info(color);

        Collection<? extends Player> playerList = Bukkit.getOnlinePlayers();
        for(Player player : playerList) player.sendMessage(color);
    }

    private void broadcastLoadMessage() {
        FileConfiguration config = getConfig("config.yml");
        boolean shouldBroadcast = config.getBoolean("broadcast.on-load");
        if(!shouldBroadcast) return;
        
        LanguageManager languageManager = getCombatLogXLanguageManager();
        String message = languageManager.getMessageColored("broadcasts.on-load");
        broadcastMessage(message);
    }

    private void broadcastEnableMessage() {
        FileConfiguration config = getConfig("config.yml");
        boolean shouldBroadcast = config.getBoolean("broadcast.on-enable");
        if(!shouldBroadcast) return;
    
        LanguageManager languageManager = getCombatLogXLanguageManager();
        String message = languageManager.getMessageColored("broadcasts.on-enable");
        broadcastMessage(message);
    }

    private void broadcastDisableMessage() {
        FileConfiguration config = getConfig("config.yml");
        boolean shouldBroadcast = config.getBoolean("broadcast.on-disable");
        if(!shouldBroadcast) return;
    
        LanguageManager languageManager = getCombatLogXLanguageManager();
        String message = languageManager.getMessageColored("broadcasts.on-disable");
        broadcastMessage(message);
    }

    private void untagAllPlayers() {
        CombatManager manager = getCombatManager();
        List<Player> playerList = Bukkit.getOnlinePlayers().stream().filter(manager::isInCombat).collect(Collectors.toList());
        playerList.forEach(player -> manager.untag(player, PlayerUntagEvent.UntagReason.EXPIRE));
    }

    private void registerListeners() {
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new ListenerAttack(this), this);
        manager.registerEvents(new ListenerCombatChecks(this), this);
        manager.registerEvents(new ListenerPunishChecks(this), this);
        manager.registerEvents(new ListenerUntagger(this), this);
        manager.registerEvents(this.customDeathListener, this);
    }

    private void registerCommands() {
        new CommandCombatLogX(this).register();
        new CommandCombatTimer(this).register();
    }

    private void registerTasks() {
        CombatManager combatManager = getCombatManager();
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimer(this, combatManager, 0L, 10L);
    }
}
