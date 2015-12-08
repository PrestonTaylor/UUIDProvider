package net.kaikk.mc.uuidprovider;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class EventListener implements Listener {
	UUIDProvider instance;
	
	EventListener(UUIDProvider instance) {
		this.instance = instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
		UUID uuid = UUIDProvider.get(event.getName());
		if (uuid==null) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Invalid UUID or Mojang's servers down. Try later.");
			instance.getLogger().warning("UUIDProvider couldn't retrieve UUID for "+event.getName());
		}
	}
}
