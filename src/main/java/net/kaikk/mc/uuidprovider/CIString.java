package net.kaikk.mc.uuidprovider;

public class CIString {
	private String string, lcString;
	
	public CIString(String string) {
		this.string = string;
		this.lcString = string.toLowerCase();
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public int hashCode() {
		return this.lcString.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		String otherMessage = null;
		if (obj instanceof CIString) {
			otherMessage = obj.toString();
		}
		if (obj instanceof String) {
			otherMessage = (String) obj;
		}
		
		return this.lcString.equalsIgnoreCase(otherMessage);
	}
	
	
}
