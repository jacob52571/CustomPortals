package com.jacob5257.CustomPortals.tabCompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PortalsCommandTabCompleter implements TabCompleter {
    List<String> arguments = new ArrayList<>();
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (arguments.isEmpty()) {
            /*
             * commandSender.sendMessage("§6Custom Portals §7- §aHelp");
             *                 commandSender.sendMessage("§6/portals §7- §aDisplays plugin information.");
             *                 commandSender.sendMessage("§6/portals help §7- §aDisplays this help message.");
             *                 if (commandSender.hasPermission("customportals.reset")) {
             *                     commandSender.sendMessage("§6/portals config §7- §Dump the configuration file to chat for debugging.");
             *                     commandSender.sendMessage("§6/portals list §7- §aLists all portals.");
             *                     commandSender.sendMessage("§6/portals reset §7- §aResets all portals. Use with caution!");
             *                     commandSender.sendMessage("§6/portals reload §7- §aReloads the plugin and all portals.");
             *                 }
             */
            arguments.add("help");
            if (sender.hasPermission("customportals.reset")) {
                arguments.add("config");
                arguments.add("list");
                arguments.add("reset");
                arguments.add("reload");
            }
        }
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String a : arguments) {
                if (a.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(a);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
