package net.kaikk.mc.uuidprovider;

import java.util.UUID;

public class Utils {

	public static UUID toUUID(byte[] bytes) {
	    if (bytes.length != 16) {
	        throw new IllegalArgumentException();
	    }
	    int i = 0;
	    long msl = 0;
	    for (; i < 8; i++) {
	        msl = (msl << 8) | (bytes[i] & 0xFF);
	    }
	    long lsl = 0;
	    for (; i < 16; i++) {
	        lsl = (lsl << 8) | (bytes[i] & 0xFF);
	    }
	    return new UUID(msl, lsl);
	}
	
	public static UUID nameToGeneratedUUID(String name) {
		byte[] nameBytes = name.getBytes();
		byte[] bytes = new byte[16];
		
		for (int i=0; i<16 && i<nameBytes.length; i++) {
			bytes[i] = nameBytes[i];
		}
		
		return toUUID(bytes);
	}
	
	public static String UUIDtoHexString(UUID uuid) {
		if (uuid==null) return "0x0";
		return "0x"+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getMostSignificantBits()), 16, "0")+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getLeastSignificantBits()), 16, "0");
	}
	
	public static int epoch() {
		return (int) (System.currentTimeMillis()/1000);
	}
}
