package net.kaikk.mc.uuidprovider;

import java.util.UUID;

class PlayerData {
	UUID uuid;
	String name;
	int lastCheck;
	
	PlayerData(UUID uuid, String name) {
		this(uuid, name, DataStore.epoch());
	}
	
	PlayerData(UUID uuid, String name, int lastCheck) {
		this.uuid = uuid;
		this.name = name;
		this.lastCheck = lastCheck;
	}
	
	boolean check() {
		return (DataStore.epoch()-this.lastCheck < 3196800); // 37 days
	}
}
