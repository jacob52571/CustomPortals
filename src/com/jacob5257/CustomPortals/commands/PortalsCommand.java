package com.jacob5257.CustomPortals.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.FileWriter;
import java.io.IOException;

public class PortalsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (s.equalsIgnoreCase("portals")) {
            if (strings.length == 0) {
                commandSender.sendMessage("§6Custom Portals §7- §aVersion 0.5.0");
                commandSender.sendMessage("§7Developed by §6Jacob5257");
                commandSender.sendMessage("§7Use §6/portals help §7for a list of commands.");
            } else if (strings[0].equalsIgnoreCase("help")) {
                commandSender.sendMessage("§6Custom Portals §7- §aHelp");
                commandSender.sendMessage("§6/portals §7- §aDisplays plugin information.");
                commandSender.sendMessage("§6/portals help §7- §aDisplays this help message.");
                commandSender.sendMessage("§6/portals reset §7- §aResets all portals. Use with caution!");
                commandSender.sendMessage("§6/portals reload §7- §aReloads the plugin and all portals.");
            } else if (strings[0].equalsIgnoreCase("reset")) {
                if (commandSender.hasPermission("customportals.reset")) {
                    if (strings.length == 1) {
                        commandSender.sendMessage("§6Custom Portals §7- §cThis command is still being tested and may not work as intented.");
                        commandSender.sendMessage("§6Custom Portals §7- §cAre you sure you want to do this? All existing portals will have to be relit! Type /portals reset confirm to do this.");
                    } else if (strings[1].equalsIgnoreCase("confirm")) {
                        commandSender.sendMessage("§6Custom Portals §7- §aResetting all portals...");
                        // open portals.json, erase it, and reload the plugin
                        try {
                            FileWriter fileWriter = new FileWriter("portals.json", false); // false means overwrite
                            fileWriter.write(""); // write empty string
                            fileWriter.close();
                            commandSender.sendMessage("§6Custom Portals §7- §aPortals file has been reset. You may need to reload the plugin (/portals reload) or restart the server.");
                        } catch (IOException e) {
                            commandSender.sendMessage("§6Custom Portals §7- §cThe portals.json file does not exist. Try reloading the plugin.");
                        }
                        //CustomPortals.resetPortals();
                        commandSender.sendMessage("§6Custom Portals §7- §aAll portals have been reset.");
                    }
                } else {
                    commandSender.sendMessage("§6Custom Portals §7- §cYou do not have permission to use this command.");
                }
            } else if (strings[0].equalsIgnoreCase("reload")) {
                if (commandSender.hasPermission("customportals.reset")) {
                    commandSender.sendMessage("§6Custom Portals §7- §aReloading plugin...");
                    PluginManager pluginManager = Bukkit.getPluginManager();
                    Plugin plugin = pluginManager.getPlugin("CustomPortals");
                    if (plugin == null) {
                        commandSender.sendMessage("§6Custom Portals §7- §cThe plugin could not be found. Try restarting the server.");
                        return false;
                    }
                    pluginManager.disablePlugin(plugin);
                    pluginManager.enablePlugin(plugin);
                    commandSender.sendMessage("§6Custom Portals §7- §aPlugin has been reloaded.");
                } else {
                    commandSender.sendMessage("§6Custom Portals §7- §cYou do not have permission to use this command.");
                }
            } else {
                commandSender.sendMessage("§6Custom Portals §7- §cUnknown command. Use /portals help for a list of commands.");
            }
        }
        return true;
    }
}
