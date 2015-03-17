package net.kaikk.mc.uuidprovider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UUIDProvider extends JavaPlugin {
	public final EventListener eventListener = new EventListener();
	static UUIDProvider instance;
	static ConcurrentHashMap<String,PlayerData> cachedPlayersName;
	static ConcurrentHashMap<UUID,PlayerData> cachedPlayersUUID;
	
	static ArrayList<String> cachedNullPlayersName;
	static ArrayList<UUID> cachedNullPlayersUUID;
	
	static Method getUniqueId;
	static Method getPlayerByUUID;
	
	static Config config;
	static DataStore ds;
	
	public void onEnable() {
		instance=this;
		
		config=new Config(); 
		
        try {
        	getUniqueId = OfflinePlayer.class.getMethod("getUniqueId");
        	getPlayerByUUID = Server.class.getMethod("getOfflinePlayer", UUID.class);
        	this.getLogger().info("UUIDProvider uses Bukkit 1.7.5+ to retrieve players UUID. (1.7.5 or greater)");
        } catch (Exception e) {
        	getUniqueId=null;
        	getPlayerByUUID=null;
        	cachedPlayersName = new ConcurrentHashMap<String,PlayerData>();
        	cachedPlayersUUID = new ConcurrentHashMap<UUID,PlayerData>();
        	cachedNullPlayersName = new ArrayList<String>();
        	cachedNullPlayersName = new ArrayList<String>();
        	
        	try {
				ds=new DataStore(this, config.dbUrl, config.dbUsername, config.dbPassword);
			} catch (Exception e1) {
				ds=null;
			}
        	this.getLogger().info("UUIDProvider uses UUIDFetcher to retrieve players UUID. (1.6.4 or less)");
        }
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.eventListener, this);
	}
	
	/** get the player UUID for this player
	 * @return player's UUID, null if it couldn't have found */
	static public UUID get(OfflinePlayer player) {
		if (player==null) {
			return null;
		}
		
		// Bukkit 1.7.5+
		if (getUniqueId!=null) {
			try {
				return (UUID) getUniqueId.invoke(player);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		String name = player.getName();
		if (name!=null) {
			return retrieveUUID(name);
		}
		return null;
	}
	
	
	static public UUID retrieveUUID(String name) {
		// Cache
		PlayerData playerData = cachedPlayersName.get(name);
		if (playerData!=null && playerData.check()) {
			return playerData.uuid;
		}

		if (cachedNullPlayersName.contains(name)) {
			return null;
		}
		
		// Database cache
		if (ds!=null) {
			playerData = ds.getPlayerData(name);
			if (playerData!=null) {
				return playerData.uuid;
			}
		}
		
		// UUIDFetcher
		UUIDFetcher uuidfetcher = new UUIDFetcher(Arrays.asList(name));
		try {
			Map<String,UUID> nameToUUIDMap = uuidfetcher.call();
			UUID uuid=null;
			for (UUID uuidRow : nameToUUIDMap.values()) {
				uuid=uuidRow;
			}
			
			UUIDProvider.instance.getLogger().info(name+" <-> "+(uuid!=null?uuid.toString():"null"));

			// cache result
			if (uuid!=null) {
				PlayerData oldPlayerData;
				playerData = new PlayerData(uuid, name);
				if ((oldPlayerData=cachedPlayersUUID.put(uuid, playerData))!=null) {
					// this player changed name... remove old name from cache
					cachedPlayersName.remove(oldPlayerData.name);
				}
				cachedPlayersName.put(name, playerData);
				
				if (ds!=null){
					ds.addData(playerData);
				}
			} else {
				cachedNullPlayersName.add(name);
			}
			
			return uuid;
		} catch (Exception e) {
			return null;
		}
	}
	
	
	/** get the OfflinePlayer for this UUID
	 * @return OfflinePlayer, null if it couldn't have found */
	static public OfflinePlayer get(UUID uuid) {
		if (uuid==null) {
			return null;
		}
		
		// Bukkit 1.7.5+
		if (getPlayerByUUID!=null) {
			try {
				OfflinePlayer player = (OfflinePlayer) getPlayerByUUID.invoke(instance.getServer(), uuid);
				
				return player;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		// NameFetcher
		String name = retrieveName(uuid);
		if (name==null) {
			return null;
		}

		return instance.getServer().getOfflinePlayer(name);
	}
	
	/** This gets the player name without checking the server data
	 * @return String player name for this uuid, null if it couldn't have found*/
	static public String retrieveName(UUID uuid) {
		if (uuid==null) {
			return null;
		}
		
		// Cache
		PlayerData playerData = UUIDProvider.cachedPlayersUUID.get(uuid);
		if (playerData!=null && playerData.check()) {
			return playerData.name;
		}
		
		if (cachedNullPlayersUUID.contains(uuid)) {
			return null;
		}
		
		// Database cache
		if (ds!=null) {
			playerData = ds.getPlayerData(uuid);
			if (playerData!=null) {
				return playerData.name;
			}
		}
		
		// NameFetcher
		NameFetcher namefetcher = new NameFetcher(Arrays.asList(uuid));
		try {
			String name=null;
			Map<UUID,String> UUIDToNameMap = namefetcher.call();
			for (String nameRow : UUIDToNameMap.values()) {
				name=nameRow;
			}
			
			UUIDProvider.instance.getLogger().info((name!=null?name:"null")+" <-> "+uuid.toString());
			
			// cache result
			if (name!=null) {
				playerData=new PlayerData(uuid, name);
				cachedPlayersName.put(name, playerData);
				cachedPlayersUUID.put(uuid, playerData);
				
				if (ds!=null){
					ds.addData(playerData);
				}
			} else {
				cachedNullPlayersUUID.add(uuid);
			}
			
			return name;
		} catch (Exception e) {
			return null;
		}
	}
}
