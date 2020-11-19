package com.SirBlobman.combatlogx.expansion.compatibility.citizens.listener;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.SirBlobman.combatlogx.api.event.PlayerPunishEvent;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.CompatibilityCitizens;
import com.SirBlobman.combatlogx.expansion.compatibility.citizens.manager.NPCManager;

public class ListenerPunish implements Listener {
    private final CompatibilityCitizens expansion;
    public ListenerPunish(CompatibilityCitizens expansion) {
        this.expansion = expansion;
    }
    
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void beforePunish(PlayerPunishEvent e) {
        FileConfiguration config = this.expansion.getConfig("citizens-compatibility.yml");
        if(!config.getBoolean("allow-punishments", false)) e.setCancelled(true);
        
        Player player = e.getPlayer();
        LivingEntity enemy = e.getPreviousEnemy();
        
        NPCManager npcManager = this.expansion.getNPCManager();
        YamlConfiguration data = npcManager.getData(player);
        data.set("citizens-compatibility.punish-next-join", true);
        
        npcManager.setData(player);
        npcManager.createNPC(player, enemy);

        if(config.getBoolean("retag-player-on-login", true))
            this.expansion.getEnemyStorageManager().store(player.getUniqueId(), e.getPreviousEnemy());
    }
}