package de.donotconnect.notary_cache.operator.Interfaces;

public interface ICacheStrategy extends IListener {

	public void manage(ICache c);
	public String getCacheAsString(char firstDelim, char secondDelim);
	public DefaultEntry getEntry(DefaultEntry base);
	
}
