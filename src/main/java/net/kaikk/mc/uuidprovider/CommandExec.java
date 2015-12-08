package net.kaikk.mc.uuidprovider;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandExec implements CommandExecutor {
	private UUIDProvider instance;
	
	CommandExec(UUIDProvider instance) {
		this.instance = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("uuidprovider.manage")) {
			sender.sendMessage("No permissions.");
			return false;
		}
		
		if (cmd.getName().equalsIgnoreCase("uuidprovider")) {
			if (args.length==0) {
				sender.sendMessage("Usage: /"+label+" (get|reload|clearcache)");
				return false;
			}
			
			if (args[0].equalsIgnoreCase("get")) {
				if (args.length==1) {
					sender.sendMessage("Usage: /"+label+" get (name|uuid) [player]");
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
						player=instance.getServer().getPlayer(args[2]);
						if (player==null) {
							sender.sendMessage("Player not found");
							return false;
						}
					}
					
					UUID uuid = UUIDProvider.get(player.getName());
					
					sender.sendMessage(player.getName()+"'s UUID is "+(uuid!=null?uuid.toString():"null"));
					return true;
				}
				
				if (args[1].equalsIgnoreCase("name")) {
					if (args.length==2) {
						sender.sendMessage("Usage: /"+label+" get name (player's uuid)");
						return false;
					}
					UUID uuid = UUID.fromString(args[2]);
					if (uuid==null) {
						sender.sendMessage("Invalid UUID.");
						return false;
					}
					
					String name = UUIDProvider.get(uuid);
					if(name==null) {
						sender.sendMessage("I can't find the name for "+args[2]);
						return false;
					}
					
					sender.sendMessage(args[2]+" is "+name+"'s UUID.");
					return true;
				}
			}
			
			if (args[0].equalsIgnoreCase("reload")) {
				Bukkit.getPluginManager().disablePlugin(instance);
				Bukkit.getPluginManager().enablePlugin(instance);
				sender.sendMessage("UUIDProvider reloaded.");
				return true;
			}
			
			if (args[0].equalsIgnoreCase("clearcache")) {
				instance.ds.clearCache();
				instance.cachedPlayersName.clear();
				instance.cachedPlayersUUID.clear();
				sender.sendMessage("UUIDProvider's cache cleared.");
				return true;
			}
		}
		return true;
	}
}
