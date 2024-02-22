package com.jacob5257.CustomPortals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateCheck {

    private Plugin plugin;
    private String urlVersionCheck;
    private String currentVersion;

    public UpdateCheck(Plugin plugin, String UrlVersion) {
        this.plugin = plugin;
        this.urlVersionCheck = UrlVersion;
        this.currentVersion = plugin.getDescription().getVersion();
        start();
    }

    private void start() {
        Thread th = new Thread((Runnable) new BukkitRunnable() {
            @Override
            public void run() {
                String v = null;
                try {
                    v = getText(urlVersionCheck);
                } catch (Exception e) {
                    plugin.getLogger().info("[CustomPortals] Could not check for updates.");
                    plugin.getLogger().info("---------- Stack Trace ----------");
                    e.printStackTrace();
                    plugin.getLogger().info("---------- Stack Trace ----------");
                }
                assert v != null;
                if (!v.equalsIgnoreCase(currentVersion)) {
                    plugin.getLogger().info("--- [CustomPortals] ---");
                    plugin.getLogger().info("There is an update available!");
                    plugin.getLogger().info("Current version: " + currentVersion);
                    plugin.getLogger().info("New version available: " + v);
                    plugin.getLogger().info("Update your plugin at: https://github.com/jacob52571/CustomPortals/releases");
                    plugin.getLogger().info("--- [CustomPortals] ---");
                }
                CustomPortals.newUpdate = !v.equalsIgnoreCase(currentVersion);
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 200L));
        th.setName("Update Check");
        th.start();
    }

    private String getText(String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }
}