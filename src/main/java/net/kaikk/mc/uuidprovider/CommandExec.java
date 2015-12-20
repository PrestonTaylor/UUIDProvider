package net.kaikk.mc.uuidprovider;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;


public class CommandExec implements CommandExecutor {
	private UUIDProvider instance;
	
	CommandExec(UUIDProvider instance) {
		this.instance = instance;
	}
	public CommandResult clearCache(CommandSource src, CommandContext args) throws CommandException {
		instance.ds.clearCache();
		instance.cachedPlayersName.clear();
		instance.cachedPlayersUUID.clear();
		src.sendMessage(Texts.of("UUIDProvider's cache cleared"));
		return CommandResult.success();
	}
	
	public CommandResult get(CommandSource src, CommandContext args) throws CommandException {
		if (args.getOne("Get Option").get() == "uuid") {
			String name = (String) args.getOne("Item to get").get();
			Player player = instance.game.getServer().getPlayer(name).get();
			if (player == null) {
				src.sendMessage(Texts.of("Player not found"));
				return CommandResult.empty();
			} else {
				UUID uuid = UUIDProvider.get(player.getName());
				src.sendMessage(Texts.of(player.getName()+"'s UUID is "+(uuid != null ? uuid.toString() : "null")));
				return CommandResult.success();
			}
		} else if (args.getOne("Get Option").get() == "name") {
			UUID uuid = UUID.fromString((String) args.getOne("Item to get").get());
			if (uuid == null) {
				src.sendMessage(Texts.of("Invalid UUID."));
				return CommandResult.success();
			} else {
				Optional<Player> player = instance.game.getServer().getPlayer(uuid);
				if(!player.isPresent()) {
					src.sendMessage(Texts.of("Player not found"));
					return CommandResult.empty();
				} else {
					src.sendMessage(Texts.of(uuid.toString() + " UUID belongs to " + player.get().getName()));
					return CommandResult.success();
				}
			}
		}
		return CommandResult.empty();
	}
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		return CommandResult.empty();
	}
}
