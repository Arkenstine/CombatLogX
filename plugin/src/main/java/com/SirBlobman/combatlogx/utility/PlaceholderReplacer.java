package com.SirBlobman.combatlogx.utility;

import java.text.DecimalFormat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.github.sirblobman.api.nms.EntityHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.utility.MessageUtility;

import com.SirBlobman.combatlogx.api.ICombatLogX;
import com.SirBlobman.combatlogx.api.utility.ICombatManager;
import com.SirBlobman.combatlogx.api.utility.ILanguageManager;

public final class PlaceholderReplacer {
    public static String getTimeLeftSeconds(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        int secondsLeft = manager.getTimerSecondsLeft(player);
        if(secondsLeft <= 0) {
            ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
            return languageManager.getMessage("placeholders.time-left-zero");
        }

        return Integer.toString(secondsLeft);
    }

    public static String getInCombat(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        boolean isInCombat = manager.isInCombat(player);
        String path = "placeholders.status." + (isInCombat ? "in-combat" : "not-in-combat");
    
        ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
        return languageManager.getMessageColored(path);
    }

    public static String getCombatStatus(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        boolean isInCombat = manager.isInCombat(player);
        String path = "placeholders.status." + (isInCombat ? "fighting" : "idle");
    
        ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
        return languageManager.getMessageColored(path);
    }

    public static String getEnemyName(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        LivingEntity enemy = manager.getEnemy(player);
        if(enemy == null) {
            ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
            return languageManager.getMessage("errors.unknown-entity-name");
        }
    
        MultiVersionHandler multiVersionHandler = plugin.getMultiVersionHandler();
        EntityHandler entityHandler = multiVersionHandler.getEntityHandler();
        return entityHandler.getName(enemy);
    }

    public static String getEnemyHealth(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        LivingEntity entity = manager.getEnemy(player);
        if(entity == null) {
            ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
            return languageManager.getMessage("placeholders.unknown-enemy");
        }

        double health = entity.getHealth();
        DecimalFormat format = new DecimalFormat("0.00");
        return format.format(health);
    }

    public static String getEnemyHealthRounded(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        LivingEntity entity = manager.getEnemy(player);
        if(entity == null) {
            ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
            return languageManager.getMessage("placeholders.unknown-enemy");
        }

        double health = entity.getHealth();
        long rounded = Math.round(health);
        return Long.toString(rounded);
    }

    public static String getEnemyHearts(ICombatLogX plugin, Player player) {
        ICombatManager manager = plugin.getCombatManager();
        LivingEntity entity = manager.getEnemy(player);
        if(entity == null) {
            ILanguageManager languageManager = plugin.getCombatLogXLanguageManager();
            return languageManager.getMessage("placeholders.unknown-enemy");
        }

        double health = entity.getHealth();
        double hearts = Math.ceil(health / 2.0D);
        long heartsLong = Double.valueOf(hearts).longValue();
        
        StringBuilder builder = new StringBuilder("&4");
        String heartSymbol = "\u2764";
        while(heartsLong > 0) {
            builder.append(heartSymbol);
            heartsLong--;
        }

        return MessageUtility.color(builder.toString());
    }
}
