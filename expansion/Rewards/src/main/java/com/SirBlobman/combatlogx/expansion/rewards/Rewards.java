package com.SirBlobman.combatlogx.expansion.rewards;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.expansion.Expansion;
import com.SirBlobman.combatlogx.expansion.rewards.listener.ListenerRewards;
import com.SirBlobman.combatlogx.expansion.rewards.object.Reward;

public class Rewards extends Expansion {
    private final List<Reward> rewardList;
    public static boolean usePlaceholderAPI = false;
    public Rewards(ICombatLogX plugin) {
        super(plugin);
        this.rewardList = new ArrayList<>();
    }

    @Override
    public void onLoad() {
        saveDefaultConfig("rewards.yml");
        setupRewards();
    }

    @Override
    public void reloadConfig() {
        reloadConfig("rewards.yml");
        setupRewards();
    }

    @Override
    public void onEnable() {
        ICombatLogX plugin = getPlugin();
        JavaPlugin javaPlugin = plugin.getPlugin();
        usePlaceholderAPI = javaPlugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        ListenerRewards listener = new ListenerRewards(this);
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(listener, javaPlugin);
    }

    @Override
    public void onDisable() {
        // Do Nothing
    }
    
    public List<Reward> getAllRewards() {
        return new ArrayList<>(this.rewardList);
    }

    private void setupRewards() {
        this.rewardList.clear();
        FileConfiguration config = getConfig("rewards.yml");
        if(!config.isConfigurationSection("rewards")) return;

        ConfigurationSection sectionRewards = config.getConfigurationSection("rewards");
        if(sectionRewards == null) return;

        Set<String> rewardSet = sectionRewards.getKeys(false);
        for(String rewardId : rewardSet) {
            ConfigurationSection rewardInfo = sectionRewards.getConfigurationSection(rewardId);
            if(rewardInfo == null) continue;

            Reward reward = setupReward(rewardId, rewardInfo);
            if(reward != null) this.rewardList.add(reward);
        }
    }

    private Reward setupReward(String rewardId, ConfigurationSection rewardInfo) {
        ICombatLogX plugin = getPlugin();
        Logger logger = getLogger();

        int chance = rewardInfo.getInt("chance");
        int maxChance = rewardInfo.getInt("max-chance");
        if(chance <= 0 || maxChance <= 0) {
            logger.info("Ignoring invalid reward '" + rewardId + "' with chance/max-chance 0.");
            return null;
        }

        if(chance > maxChance) {
            logger.info("Ignoring invalid reward '" + rewardId + "' with chance greater than max-chance.");
            return null;
        }

        List<String> commandList = rewardInfo.getStringList("commands");
        if(commandList.isEmpty()) {
            logger.info("Ignoring invalid reward '" + rewardId + "' with empty/null commands.");
            return null;
        }

        boolean mobWhiteList = rewardInfo.getBoolean("mob-whitelist");
        List<String> mobTypeList = rewardInfo.getStringList("mob-list");

        boolean worldWhiteList = rewardInfo.getBoolean("world-whitelist");
        List<String> worldNameList = rewardInfo.getStringList("world-list");

        boolean randomCommand = rewardInfo.getBoolean("random-command");
        return new Reward(plugin, chance, maxChance, mobWhiteList, worldWhiteList, randomCommand, mobTypeList, worldNameList, commandList);
    }
}