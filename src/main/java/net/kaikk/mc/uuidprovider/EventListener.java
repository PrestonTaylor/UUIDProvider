package net.kaikk.mc.uuidprovider;

import java.util.UUID;
import java.util.logging.Level;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Texts;


public class EventListener {
	UUIDProvider instance;
	
	EventListener(UUIDProvider instance) {
		this.instance = instance;
	}

	@Listener(order=Order.FIRST, beforeModifications=false)
	public void onPlayerLogin(ClientConnectionEvent.Auth event) {
		UUID uuid = event.getProfile().getUniqueId();
		if (uuid == null) {
			event.setMessage(Texts.of("Invalid UUID or Mojang's servers down. Try later."));
			event.setCancelled(true);
			instance.log(Level.WARNING, "UUIDProvider couldn't retrieve UUID for "+event.getProfile().getName());
		}
	}
}
