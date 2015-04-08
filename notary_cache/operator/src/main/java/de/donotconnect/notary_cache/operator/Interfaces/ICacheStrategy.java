package de.donotconnect.notary_cache.operator.Interfaces;

public interface ICacheStrategy extends IListener {

	public void manage(ICache c, AbstractNotary n);
	public String getCacheAsString();
	
}
