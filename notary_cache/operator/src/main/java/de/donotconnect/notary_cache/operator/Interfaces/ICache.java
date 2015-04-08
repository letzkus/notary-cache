package de.donotconnect.notary_cache.operator.Interfaces;

import java.util.Collection;

public interface ICache {
	
	public AbstractEntry getEntry(AbstractEntry base);
	public void addEntry(AbstractEntry e);
	public void removeEntry(AbstractEntry base);
	public boolean entryExists(AbstractEntry base);
	public Collection<AbstractEntry> getCollection();
	public int size();

}
