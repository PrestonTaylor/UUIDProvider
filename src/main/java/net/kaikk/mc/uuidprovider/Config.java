package net.kaikk.mc.uuidprovider;

import org.bukkit.plugin.java.JavaPlugin;


public class Config {
	public String dbHostname, dbUsername, dbPassword, dbDatabase;
	public boolean offlineMode;
	
	Config(JavaPlugin instance) {
		instance.reloadConfig();
		instance.getConfig().options().copyDefaults(true);
		instance.saveDefaultConfig();
		
		this.dbHostname=instance.getConfig().getString("MySQL.Hostname");
		this.dbUsername=instance.getConfig().getString("MySQL.Username");
		this.dbPassword=instance.getConfig().getString("MySQL.Password");
		this.dbDatabase=instance.getConfig().getString("MySQL.Database");
		
		this.offlineMode=instance.getConfig().getBoolean("OfflineMode");
	}
}