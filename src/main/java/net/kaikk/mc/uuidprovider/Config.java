package net.kaikk.mc.uuidprovider;
import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;

import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
 
class Config {
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	
	@Inject
	@ConfigDir(sharedRoot = true)
	private Path privateConfigDir;
	
	private UUIDProvider instance;
	
	private String dbUrl;
	private String dbUsername;
	private String dbPassword;
	private Boolean offlineMode;

	public String getDbUrl() {
		return dbUrl;
	}
	public String getDbUsername() {
		return dbUsername;
	}
	public String getDbPassword() {
		return dbPassword;
	}
	public Boolean getOfflineMode() {
		return offlineMode;
	}
	
	Config(UUIDProvider instance) {
		this.instance = instance;
		setDefaults();
		load();
	}
	Config() {
		throw new UnsupportedOperationException();
	}
	
	
	private void setDefaults() {

		try {
			File theDir = new File("mods"+ File.separator + "UUIDProvider");
			theDir.mkdirs();
			File theConfig = new File("mods"+ File.separator + "UUIDProvider" + File.separator + "UUIDProvider.conf");
			if (!theConfig.exists()) {
				theConfig.createNewFile();
				Path potentialFile = Paths.get("mods"+ File.separator + "UUIDProvider" + File.separator + "UUIDProvider.conf");
				ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(potentialFile).build();
				ConfigurationNode rootNode;
				    rootNode = loader.load();
				    rootNode.getNode("offlineMode").setValue(false);
				    rootNode.getNode("dbUrl").setValue("jdbc:mysql://127.0.0.1/UUIDProvider");
				    rootNode.getNode("dbUsername").setValue("UUIDProvider");
				    rootNode.getNode("dbPassword").setValue("");
				    loader.save(rootNode);
			}
		} catch(IOException e) {
		    e.printStackTrace();
		}
	}

	private void load() {
		Path config = Paths.get("mods"+ File.separator + "UUIDProvider" + File.separator + "UUIDProvider.conf");
		ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(config).build();
		ConfigurationNode rootNode;
		    try {
				rootNode = loader.load();
				offlineMode = rootNode.getNode("offlineMode").getBoolean(true);
				dbUrl = rootNode.getNode("dbUrl").getString("jdbc:mysql://127.0.0.1/UUIDProvider");
				dbUsername = rootNode.getNode("dbUsername").getString("UUIDProvider");
				dbPassword = rootNode.getNode("dbPassword").getString("");
		    
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}