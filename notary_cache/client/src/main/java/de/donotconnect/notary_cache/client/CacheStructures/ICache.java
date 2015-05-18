package de.donotconnect.notary_cache.client.CacheStructures;

import java.util.Collection;

/**
 * Contains methods which must be implemented by a specific cache
 * implementation. Methods are mainly self-describing.
 * 
 * @author fabianletzkus
 *
 */
public interface ICache {

	public DefaultEntry getEntry(DefaultEntry base);

	public void addEntry(DefaultEntry e);

	public void removeEntry(DefaultEntry base);

	public boolean entryExists(DefaultEntry base);

	public Collection<DefaultEntry> getCollection();

	public int size();

	public void commit();

	public void close();

	public void open();

	public void clear();

}
