package net.kaikk.mc.uuidprovider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UUIDProvider extends JavaPlugin {
	public final EventListener eventListener = new EventListener();
	static UUIDProvider instance;
	static ConcurrentHashMap<String,PlayerData> cachedPlayersName = new ConcurrentHashMap<String,PlayerData>();
	static ConcurrentHashMap<UUID,PlayerData> cachedPlayersUUID = new ConcurrentHashMap<UUID,PlayerData>();
	
	static Method getUniqueId;
	static Method getPlayerByUUID;
	
	static Config config;
	static DataStore ds;
	
	public void onEnable() {
		instance=this;

        try {
        	getUniqueId = OfflinePlayer.class.getMethod("getUniqueId");
        	getPlayerByUUID = Server.class.getMethod("getOfflinePlayer", UUID.class);
        	this.getLogger().info("Bukkit 1.7.5+ UUID support found.");
        } catch (Exception e) {
        	getUniqueId=null;
        	getPlayerByUUID=null;
        	
        	this.getLogger().info("No Bukkit 1.7.5+ UUID support found.");
        }
        
        config=new Config(); 
        
    	try {
			ds=new DataStore(this, config.dbUrl, config.dbUsername, config.dbPassword);
		} catch (Exception e1) {
			this.getLogger().warning("UUIDProvider won't use a MySQL database! This may affect performances!");
			ds=null;
		}
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.eventListener, this);
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage("No permissions.");
			return false;
		}
		
		if (cmd.getName().equalsIgnoreCase("uuidprovider")) {
			if (args.length==0) {
				sender.sendMessage("Usage: /uuidprovider (get|reload|clearcache)");
				return false;
			}
			
			if (args[0].equalsIgnoreCase("get")) {
				if (args.length==1) {
					sender.sendMessage("Usage: /uuidprovider get (name|uuid) [player]");
					return false;
				}
				
				if (args[1].equalsIgnoreCase("uuid")) {
					Player player;
					if (args.length==2) {
						if (!(sender instanceof Player)) {
							sender.sendMessage("You're the console.");
							return false;
						}
						
						player=(Player) sender;
					} else {
						player=this.getServer().getPlayer(args[2]);
						if (player==null) {
							sender.sendMessage("Player not found");
							return false;
						}
					}
					
					UUID uuid = get(player);
					
					sender.sendMessage(player.getName()+"'s UUID is "+(uuid!=null?uuid.toString():"null"));
					return true;
				}
				
				if (args[1].equalsIgnoreCase("name")) {
					if (args.length==2) {
						sender.sendMessage("Usage: /uuidprovider get name (player's uuid)");
						return false;
					}
					UUID uuid = UUID.fromString(args[2]);
					if (uuid==null) {
						sender.sendMessage("Invalid UUID.");
						return false;
					}
					
					String name = retrieveName(uuid);
					if(name==null) {
						sender.sendMessage("I can't find the name for "+args[2]);
						return false;
					}
					
					sender.sendMessage(args[2]+" is "+name+"'s UUID.");
					return true;
				}
			}
			
			if (args[0].equalsIgnoreCase("reload")) {
				cachedPlayersName.clear();
				cachedPlayersUUID.clear();
				this.onEnable();
				return true;
			}
			
			if (args[0].equalsIgnoreCase("clearcache")) {
				ds.clearCache();
				cachedPlayersName.clear();
				cachedPlayersUUID.clear();
				return true;
			}
		}
		return true;
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
				UUID bukkitUUID = (UUID) getUniqueId.invoke(player);
				if (bukkitUUID!=null && player.getName()!=null && !player.getName().isEmpty()) {
					if (!cachedPlayersUUID.containsKey(bukkitUUID)) {
						cacheAdd(bukkitUUID, player.getName());
					}
					return bukkitUUID;
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		String name = player.getName();
		if (name!=null) {
			return retrieveUUID(name);
		}
		return null;
	}
	
	/** This gets the player's uuid without checking the server data
	 * @return UUID player's uuid for this name, null if not found*/
	static public UUID retrieveUUID(String name) {
		// Cache
		UUID uuid = getCachedPlayer(name);
		if (uuid!=null) {
			return uuid;
		}
		
		// UUIDFetcher
		return uuidFetcher(name);
	}
	
	/** get the OfflinePlayer for this UUID
	 * @return OfflinePlayer, null if not found */
	static public OfflinePlayer get(UUID uuid) {
		if (uuid==null) {
			return null;
		}
		
		// Bukkit 1.7.5+
		if (getPlayerByUUID!=null) {
			try {
				OfflinePlayer player = (OfflinePlayer) getPlayerByUUID.invoke(instance.getServer(), uuid);

				if (player!=null && player.getName()!=null && !player.getName().isEmpty()) {
					if (!cachedPlayersUUID.containsKey(uuid)) {
						cacheAdd(uuid, player.getName());
					}
					return player;
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		String name = retrieveName(uuid);
		if (name==null) {
			return null;
		}

		return instance.getServer().getOfflinePlayer(name);
	}
	
	/** This gets the player name without checking the server data
	 * @return String player's name for this uuid, null if not found*/
	static public String retrieveName(UUID uuid) {
		if (uuid==null) {
			return null;
		}
		
		// Cache
		String name = getCachedPlayer(uuid);
		if (name!=null) {
			return name;
		}
		
		// NameFetcher
		return nameFetcher(uuid);
	}
	
	/** This gets the player name from the Mojang's server (slowest method)
	 * @return String player's name for this uuid, null if not found*/
	public static String nameFetcher(UUID uuid) {
		NameFetcher namefetcher = new NameFetcher(Arrays.asList(uuid));
		try {
			String name=null;
			Map<UUID,String> UUIDToNameMap = namefetcher.call();
			for (String nameRow : UUIDToNameMap.values()) {
				name=nameRow;
			}
			
			UUIDProvider.instance.getLogger().info((name!=null?name:"null")+" <-> "+uuid.toString());
			
			// cache result
			cacheAdd(uuid, name);
			
			return name;
		} catch (Exception e) {
			return null;
		}
	}
	
	
	/** This gets the player's uuid from the Mojang's server (slowest method)
	 * @return UUID player's uuid for this name, null if not found*/
	public static UUID uuidFetcher(String name) {
		UUIDFetcher uuidfetcher = new UUIDFetcher(Arrays.asList(name));
		try {
			Map<String,UUID> nameToUUIDMap = uuidfetcher.call();
			UUID uuid=null;
			for (UUID uuidRow : nameToUUIDMap.values()) {
				uuid=uuidRow;
			}
			
			UUIDProvider.instance.getLogger().info(name+" <-> "+(uuid!=null?uuid.toString():"null"));

			// cache result
			cacheAdd(uuid, name);
			
			return uuid;
		} catch (Exception e) {
			return null;
		}
	}

	/** This gets the player's name if in cache (MySQL database included)
	 * @return String player name for this uuid, null if not found*/
	public static String getCachedPlayer(UUID uuid) {
		PlayerData playerData = UUIDProvider.cachedPlayersUUID.get(uuid);
		if (playerData!=null && playerData.check()) {
			return playerData.name;
		}
		
		// Database cache
		if (ds!=null) {
			playerData = ds.getPlayerData(uuid);
			if (playerData!=null) {
				return playerData.name;
			}
		}
		
		return null;
	}
	
	/** This gets the player's uuid if in cache (MySQL database included)
	 * @return String player uuid for this name, null if not found*/
	public static UUID getCachedPlayer(String name) {
		PlayerData playerData = cachedPlayersName.get(name);
		if (playerData!=null && playerData.check()) {
			return playerData.uuid;
		}

		// Database cache
		if (ds!=null) {
			playerData = ds.getPlayerData(name);
			if (playerData!=null && playerData.uuid!=null) {
				return playerData.uuid;
			}
		}
		
		return null;
	}

	
	private static PlayerData cacheAdd(UUID uuid, String name) {
		PlayerData oldPlayerData, playerData = new PlayerData(uuid, name);
		
		if ((oldPlayerData=cachedPlayersUUID.put(uuid, playerData))!=null && oldPlayerData.name!=null) {
			cachedPlayersName.remove(oldPlayerData.name); // this player changed name... remove old name from cache
		}
		cachedPlayersName.put(name, playerData);
		
		if (ds!=null){
			ds.addData(playerData);
		}
		return playerData;
	}
}
