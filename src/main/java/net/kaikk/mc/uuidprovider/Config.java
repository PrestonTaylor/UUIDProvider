package net.kaikk.mc.uuidprovider;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

class Config {
	final static String configFilePath = "plugins" + File.separator + "UUIDProvider" + File.separator + "config.yml";
	private File configFile;
	FileConfiguration config;
	
	String dbUrl;
	String dbUsername;
	String dbPassword;
	
	Config() {
		this.configFile = new File(configFilePath);
		this.config = YamlConfiguration.loadConfiguration(this.configFile);
		this.load();
	}
	
	void load() {
		this.dbUrl=config.getString("dbUrl", "jdbc:mysql://127.0.0.1/uuidprovider");
		this.dbUsername=config.getString("dbUsername", "uuidprovider");
		this.dbPassword=config.getString("dbPassword", "");
		
		this.save();
	}
	
	void save() {
		try {
			this.config.set("dbUrl", this.dbUrl);
			this.config.set("dbUsername", this.dbUsername);
			this.config.set("dbPassword", this.dbPassword);

			this.config.save(this.configFile);
		} catch (IOException e) {
			UUIDProvider.instance.getLogger().warning("Couldn't create or save config file.");
			e.printStackTrace();
		}
	}
}