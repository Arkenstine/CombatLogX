package com.SirBlobman.combatlogx.manager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.event.*;
import com.SirBlobman.combatlogx.api.listener.ICustomDeathListener;
import com.SirBlobman.combatlogx.api.shaded.nms.AbstractNMS;
import com.SirBlobman.combatlogx.api.shaded.nms.EntityHandler;
import com.SirBlobman.combatlogx.api.shaded.nms.MultiVersionHandler;
import com.SirBlobman.combatlogx.api.utility.ICombatManager;
import com.SirBlobman.combatlogx.api.utility.ILanguageManager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class CombatManager implements ICombatManager, Runnable {
    private final ICombatLogX plugin;
    private final Map<UUID, Long> expireTimeMap;
    private final Map<UUID, LivingEntity> enemyMap;
    public CombatManager(ICombatLogX plugin) {
        this.plugin = plugin;
        this.expireTimeMap = new HashMap<>();
        this.enemyMap = new HashMap<>();
    }

    @Override
    public boolean tag(Player player, LivingEntity enemy, PlayerPreTagEvent.TagType tagType, PlayerPreTagEvent.TagReason tagReason) {
        if(player == null || tagType == null || tagReason == null) {
            this.plugin.printDebug("null cannot be tagged!");
            return false;
        }
        
        if(failsPreTagEvent(player, enemy, tagType, tagReason)) {
            this.plugin.printDebug("The PlayerPreTagEvent was cancelled.");
            return false;
        }
        
        boolean wasInCombat = isInCombat(player);
        this.plugin.printDebug(" Was In Combat: " + wasInCombat);

        FileConfiguration config = this.plugin.getConfig("config.yml");
        long systemMillis = System.currentTimeMillis();
        long timerMillis = (1000L * config.getLong("combat.timer"));
        long endMillis = (systemMillis + timerMillis);

        if(!wasInCombat) {
            PlayerTagEvent tagEvent = new PlayerTagEvent(player, enemy, tagType, tagReason, endMillis);
            PluginManager manager = Bukkit.getPluginManager();
            manager.callEvent(tagEvent);
            endMillis = tagEvent.getEndTime();

            sendTagMessage(player, enemy, tagType, tagReason);
        } else {
            PlayerReTagEvent tagEvent = new PlayerReTagEvent(player, enemy, tagType, tagReason, endMillis);
            PluginManager manager = Bukkit.getPluginManager();
            manager.callEvent(tagEvent);
            endMillis = tagEvent.getEndTime();
        }

        UUID uuid = player.getUniqueId();
        expireTimeMap.put(uuid, endMillis);

        if(enemy == null) this.enemyMap.putIfAbsent(uuid, null);
        else this.enemyMap.put(uuid, enemy);
        
        this.plugin.printDebug("Successfully put player '" + player.getName() + "' into combat.");
        return true;
    }

    @Override
    public void untag(Player player, PlayerUntagEvent.UntagReason untagReason) {
        if(player == null | untagReason == null || !isInCombat(player)) return;

        UUID uuid = player.getUniqueId();
        this.expireTimeMap.remove(uuid);

        LivingEntity previousEnemy = this.enemyMap.remove(uuid);
        PlayerUntagEvent untagEvent = new PlayerUntagEvent(player, untagReason, previousEnemy);

        PluginManager manager = Bukkit.getPluginManager();
        manager.callEvent(untagEvent);
    }

    @Override
    public boolean isInCombat(Player player) {
        if(player == null) return false;

        UUID uuid = player.getUniqueId();
        return this.expireTimeMap.containsKey(uuid);
    }

    @Override
    public List<Player> getPlayersInCombat() {
        List<Player> playerList = new ArrayList<>();
        Set<UUID> uuidSet = new HashSet<>(this.expireTimeMap.keySet());

        for(UUID uuid : uuidSet) {
            Player player = Bukkit.getPlayer(uuid);
            if(player != null) {
                playerList.add(player);
                continue;
            }

            this.expireTimeMap.remove(uuid);
        }

        return playerList;
    }

    @Override
    public LivingEntity getEnemy(Player player) {
        if(player == null) return null;

        UUID uuid = player.getUniqueId();
        return this.enemyMap.getOrDefault(uuid, null);
    }

    @Override
    public OfflinePlayer getByEnemy(LivingEntity enemy) {
        if(enemy == null) return null;

        UUID enemyId = enemy.getUniqueId();
        Set<Map.Entry<UUID, LivingEntity>> entrySet = new HashSet<>(this.enemyMap.entrySet());

        for(Map.Entry<UUID, LivingEntity> entry : entrySet) {
            LivingEntity valueEnemy = entry.getValue();
            if(valueEnemy == null || valueEnemy.isDead()) {
                UUID key = entry.getKey();
                this.enemyMap.remove(key);
                continue;
            }

            UUID valueEnemyId = valueEnemy.getUniqueId();
            if(!valueEnemyId.equals(enemyId)) continue;

            UUID playerId = entry.getKey();
            return Bukkit.getOfflinePlayer(playerId);
        }

        return null;
    }

    @Override
    public int getTimerSecondsLeft(Player player) {
        if(!isInCombat(player)) return -1;

        double millisLeft = getTimerMillisLeft(player);
        double secondsLeft = (millisLeft / 1_000.0D);
        return (int) Math.ceil(secondsLeft);
    }

    @Override
    public long getTimerMillisLeft(Player player) {
        if(!isInCombat(player)) return -1;

        UUID uuid = player.getUniqueId();
        long combatEnds = expireTimeMap.get(uuid);
        long systemMillis = System.currentTimeMillis();
        return (combatEnds - systemMillis);
    }

    @Override
    public boolean punish(Player player, PlayerUntagEvent.UntagReason punishReason, LivingEntity previousEnemy) {
        PlayerPunishEvent punishEvent = new PlayerPunishEvent(player, punishReason, previousEnemy);
        PluginManager manager = Bukkit.getPluginManager();

        manager.callEvent(punishEvent);
        if(punishEvent.isCancelled()) return false;

        checkKill(player);
        runPunishCommands(player, previousEnemy);
        return true;
    }

    @Override
    public String getSudoCommand(Player player, LivingEntity enemy, String command) {
        String playerName = player.getName();
        String enemyName = getEntityName(enemy);
        command = command.replace("{player}", playerName).replace("{enemy}", enemyName);
        command = replacePAPI(player, command);
        command = replaceMVdW(player, command);
        return command;
    }

    @Override
    public void run() {
        PluginManager manager = Bukkit.getPluginManager();
        List<Player> combatPlayerList = getPlayersInCombat();
        for(Player player : combatPlayerList) {
            int timeLeft = getTimerSecondsLeft(player);
            if(timeLeft <= 0) untag(player, PlayerUntagEvent.UntagReason.EXPIRE);

            Runnable task = () -> {
                PlayerCombatTimerChangeEvent event = new PlayerCombatTimerChangeEvent(player, timeLeft);
                manager.callEvent(event);
            };
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTaskAsynchronously(this.plugin.getPlugin(), task);
        }
    }

    private boolean failsPreTagEvent(Player player, LivingEntity enemy, PlayerPreTagEvent.TagType tagType, PlayerPreTagEvent.TagReason tagReason) {
        PluginManager manager = Bukkit.getPluginManager();
        PlayerPreTagEvent event = new PlayerPreTagEvent(player, enemy, tagType, tagReason);

        manager.callEvent(event);
        return event.isCancelled();
    }

    private void sendTagMessage(Player player, LivingEntity enemy, PlayerPreTagEvent.TagType tagType, PlayerPreTagEvent.TagReason tagReason) {
        if(tagType == PlayerPreTagEvent.TagType.UNKNOWN || tagReason == PlayerPreTagEvent.TagReason.UNKNOWN) {
            ILanguageManager languageManager = this.plugin.getLanguageManager();
            String message = languageManager.getMessageColored("tag-messages.unknown");
            languageManager.sendMessage(player, message);
        }

        String enemyType = (enemy == null ? EntityType.UNKNOWN.name() : enemy.getType().name());
        String enemyName = getEntityName(enemy);

        if(tagType == PlayerPreTagEvent.TagType.MOB) {
            ILanguageManager languageManager = this.plugin.getLanguageManager();
            String messagePath = ("tag-messages.attacke" + (tagReason == PlayerPreTagEvent.TagReason.ATTACKER ? "r.of" : "d.by") + "-mob");
            String message = languageManager.getMessageColored(messagePath).replace("{mob_type}", enemyType).replace("{name}", enemyName);
            languageManager.sendMessage(player, message);
        }

        if(tagType == PlayerPreTagEvent.TagType.PLAYER) {
            ILanguageManager languageManager = this.plugin.getLanguageManager();
            String messagePath = ("tag-messages.attacke" + (tagReason == PlayerPreTagEvent.TagReason.ATTACKER ? "r.of" : "d.by") + "-player");
            String message = languageManager.getMessageColored(messagePath).replace("{mob_type}", enemyName).replace("{name}", enemyName);
            languageManager.sendMessage(player, message);
        }
    }

    private String getEntityName(LivingEntity enemy) {
        if(enemy == null) {
            ILanguageManager languageManager = this.plugin.getLanguageManager();
            return languageManager.getMessage("errors.unknown-entity-name");
        }
    
        MultiVersionHandler<?> multiVersionHandler = this.plugin.getMultiVersionHandler();
        AbstractNMS nmsHandler = multiVersionHandler.getInterface();
        
        EntityHandler entityHandler = nmsHandler.getEntityHandler();
        return entityHandler.getName(enemy);
    }
    
    private void checkKill(Player player) {
        YamlConfiguration config = this.plugin.getConfig("config.yml");
        String killOption = config.getString("punishments.kill-time");
        if(killOption == null) killOption = "QUIT";
        
        if(killOption.equals("QUIT")) {
            player.setHealth(0.0D);
            ICustomDeathListener customDeathListener = this.plugin.getCustomDeathListener();
            customDeathListener.add(player);
            return;
        }
        
        if(killOption.equals("JOIN")) {
            YamlConfiguration data = this.plugin.getDataFile(player);
            data.set("kill-on-join", true);
            this.plugin.saveDataFile(player);
        }
        
        // NEVER options means don't do anything
    }

    private void runPunishCommands(Player player, LivingEntity previousEnemy) {
        YamlConfiguration config = this.plugin.getConfig("config.yml");
        List<String> punishCommandList = config.getStringList("punishments.punish-command-list");
        for(String punishCommand : punishCommandList) {
            String sudoCommand = getSudoCommand(player, previousEnemy, punishCommand);
            if(sudoCommand.startsWith("[PLAYER]")) {
                String command = sudoCommand.substring("[PLAYER]".length());
                runAsPlayer(player, command);
                continue;
            }

            if(sudoCommand.startsWith("[OP]")) {
                String command = sudoCommand.substring("[OP]".length());
                if(player.isOp()) {
                    runAsPlayer(player, command);
                    continue;
                }

                player.setOp(true);
                runAsPlayer(player, command);
                player.setOp(false);

                continue;
            }
            
            runAsConsole(sudoCommand);
        }
    }
    
    private void runAsPlayer(Player player, String command) {
        try {
            player.performCommand(command);
        } catch(Throwable ex) {
            Logger logger = this.plugin.getLogger();
            logger.log(Level.SEVERE, "An error occurred while executing a command as a player:", ex);
        }
    }
    
    private void runAsConsole(String command) {
        try {
            CommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, command);
        } catch(Throwable ex) {
            Logger logger = this.plugin.getLogger();
            logger.log(Level.SEVERE, "An error occurred while executing a command as the console:", ex);
        }
    }

    private String replacePAPI(Player player, String string) {
        PluginManager manager = Bukkit.getPluginManager();
        if(!manager.isPluginEnabled("PlaceholderAPI")) return string;
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, string);
    }

    private String replaceMVdW(Player player, String string) {
        PluginManager manager = Bukkit.getPluginManager();
        if(!manager.isPluginEnabled("MVdWPlaceholderAPI")) return string;
        return be.maximvdw.placeholderapi.PlaceholderAPI.replacePlaceholders(player, string);
    }
}