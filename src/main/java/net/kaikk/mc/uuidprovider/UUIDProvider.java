package net.kaikk.mc.uuidprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Iterables;

public class UUIDProvider extends JavaPlugin {
	static UUIDProvider instance;
	
	Config config;
	DataStore ds;
	
	Map<CIString,PlayerData> cachedPlayersName = new ConcurrentHashMap<CIString,PlayerData>();
	Map<UUID,PlayerData> cachedPlayersUUID = new ConcurrentHashMap<UUID,PlayerData>();
	
	public void onEnable() {
		instance=this;

        config=new Config(this); 
        
    	try {
			ds=new DataStore(this);
		} catch (Exception e1) {
			this.getLogger().severe("A MySQL database is required! UUIDProvider won't work without a MySQL database!");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
    	this.getCommand(this.getName()).setExecutor(new CommandExec(this));
		getServer().getPluginManager().registerEvents(new EventListener(this), this);
	}
	
	/** Get the UUID of the specified name, using all available modes <br>
	 * Thread-safe */
	public static UUID get(String name) {
		return get(name, Mode.ALL);
	}
	
	/** Get the UUID of the specified name, using the specified mode<br>
	 * One ore more modes can be specified, delimited by the | operator.<br>
	 * Example: UUID uuid = get(name, Mode.INTERNAL | Mode.DATABASE); <br>
	 * Thread-safe */
	public static UUID get(String name, int mode) {
		if (name.length()>16) {
			return null;
		}
		
		if (Mode.check(Mode.INTERNAL, mode)) {
			// Internal cache
			PlayerData playerData = getFromInternalCache(name);
			if (playerData != null) {
				return playerData.uuid;
			}
		}
		
		if (Mode.check(Mode.DATABASE, mode)) {
			// MySQL database
			PlayerData playerData = instance.ds.getPlayerData(name);
			if (playerData != null) {
				cache(playerData.uuid, name);
				return playerData.uuid;
			}
		}
		
		UUID uuid = null;
		
		if (Mode.check(Mode.MOJANG, mode)) {
			// If offline-mode is on, do not request an UUID to Mojang, but generate a new UUID
			if (instance.config.offlineMode) {
				uuid = Utils.nameToGeneratedUUID(null);
			} else {
				// Request to Mojang
				UUIDFetcher uuidfetcher = new UUIDFetcher(Arrays.asList(name));
				try {
					Map<String,UUID> nameToUUIDMap = uuidfetcher.call();
					uuid=Iterables.getFirst(nameToUUIDMap.values(), null);
				} catch (Exception e) { }
			}
			cache(uuid, name);
		}

		return uuid;
	}
	
	/** Get a map of uuids from the specified names list, using all available modes<br>
	 * This is the best method if you have to get a lot of uuids at the same time<br>
	 * Thread-safe */
	public static Map<String,UUID> getUUIDs(List<String> names) {
		return getUUIDs(names, Mode.ALL);
	}
	
	/** Get a map of uuids from the specified names list, using the specified mode<br>
	 * This is the best method if you have to get a lot of uuids at the same time<br>
	 * One ore more modes can be specified, delimited by the | operator.<br>
	 * Example: UUID uuid = get(names, Mode.INTERNAL | Mode.DATABASE); <br>
	 * Thread-safe */
	public static Map<String,UUID> getUUIDs(List<String> names, int mode) {
		Map<String,UUID> map = new HashMap<String,UUID>(names.size());
		
		if (Mode.check(Mode.MOJANG, mode)) {
			mode -= Mode.MOJANG;
			
			List<String> namesToRequest = new ArrayList<String>(names.size());
			for (String name : names) {
				UUID uuid = get(name, mode);
				if (uuid==null) {
					namesToRequest.add(name);
				}
				map.put(name, uuid);
			}
			
			if (!namesToRequest.isEmpty()) {
				// Request to Mojang
				UUIDFetcher fetcher = new UUIDFetcher(namesToRequest);
				try {
					Map<String,UUID> map2 = fetcher.call();
					map.putAll(map2);
					
					// cache results
					for (Entry<String,UUID> entry : map2.entrySet()) {
						cache(entry.getValue(), entry.getKey());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			for (String name : names) {
				UUID uuid = get(name, mode);
				map.put(name, uuid);
			}
		}
		
		return map;
	}
	
	public static String get(UUID uuid) {
		return get(uuid, Mode.ALL);
	}
	
	public static String get(UUID uuid, int mode) {
		if (Mode.check(Mode.INTERNAL, mode)) {
			// Internal cache
			PlayerData playerData = getFromInternalCache(uuid);
			if (playerData != null) {
				return playerData.name;
			}
		}
		
		String name = null;
		
		if (Mode.check(Mode.DATABASE, mode)) {
			// MySQL database
			PlayerData playerData = instance.ds.getPlayerData(name);
			if (playerData != null) {
				cache(playerData.uuid, name);
				return playerData.name;
			}
		}
		
		if (Mode.check(Mode.MOJANG, mode)) {
			// If offline-mode is on, do not request an UUID to Mojang
			if (!instance.config.offlineMode) {
				// Request to Mojang
				NameFetcher namefetcher = new NameFetcher(Arrays.asList(uuid));
				try {
					Map<UUID,String> UUIDToNameMap = namefetcher.call();
					name=Iterables.getFirst(UUIDToNameMap.values(), null);
				} catch (Exception e) { }
			}
		}
		
		cache(uuid, name);
		return name;
	}
	
	public static Map<UUID,String> getNames(List<UUID> uuids) {
		return getNames(uuids, Mode.ALL);
	}
	
	public static Map<UUID,String> getNames(List<UUID> uuids, int mode) { // TODO
		Map<UUID,String> map = new HashMap<UUID,String>(uuids.size());
		
		if (Mode.check(Mode.MOJANG, mode)) {
			mode -= Mode.MOJANG;
			
			List<UUID> uuidsToRequest = new ArrayList<UUID>(uuids.size());
			for (UUID uuid : uuids) {
				String name = get(uuid, mode);
				if (uuid==null) {
					uuidsToRequest.add(uuid);
				}
				map.put(uuid, name);
			}
			
			if (!uuidsToRequest.isEmpty()) {
				// Request to Mojang
				NameFetcher fetcher = new NameFetcher(uuidsToRequest);
				try {
					Map<UUID,String> map2 = fetcher.call();
					map.putAll(map2);
					
					// cache results
					for(Entry<UUID,String> entry : map2.entrySet()) {
						cache(entry.getKey(), entry.getValue());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			for (UUID uuid : uuids) {
				String name = get(uuid, mode);
				map.put(uuid, name);
			}
		}
		
		return map;
	}
	
	private static void cache(UUID uuid, String name) {
		PlayerData oldPlayerData, playerData = new PlayerData(uuid, name);
		if (name!=null) {
			instance.cachedPlayersName.put(new CIString(name), playerData);
		}
		
		if (uuid!=null) {
			if ((oldPlayerData=instance.cachedPlayersUUID.put(uuid, playerData))!=null && oldPlayerData.name!=null) {
				instance.cachedPlayersName.remove(oldPlayerData.name); // this player changed name... remove old name from cache
			}
			
			if (name!=null) {
				instance.ds.addData(playerData);
			}
		}
	}
	
	private static PlayerData getFromInternalCache(UUID uuid) { 
		PlayerData playerData = instance.cachedPlayersUUID.get(uuid);
		if (playerData!=null && playerData.check()) {
			return playerData;
		}
		return null;
	}
	
	private static PlayerData getFromInternalCache(String name) {
		PlayerData playerData = instance.cachedPlayersName.get(new CIString(name));
		if (playerData!=null && playerData.check()) {
			return playerData;
		}
		return null;
	}
	
	/** Modes used in get methods (see get method for more informations how to use this class)<br>
	 *  INTERNAL: Check on the internal map only <br>
	 *  DATABASE: Check the MySQL database<br>
	 *  MOJANG: Send a request to the Mojang (very slow!) <br>
	 *  ALL: All the above, in that order.
	 * */
	public static class Mode {
		public final static int INTERNAL = 1;
		public final static int DATABASE = 2;
		public final static int MOJANG = 4;
		public final static int ALL = 7;
		
		private Mode() { }
		
		public static boolean check(int mode, int selectedModes) {
			return (mode & selectedModes) != 0;
		}
	}
}
