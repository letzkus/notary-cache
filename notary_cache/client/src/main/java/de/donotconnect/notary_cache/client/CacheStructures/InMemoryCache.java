package de.donotconnect.notary_cache.client.CacheStructures;

import java.util.Collection;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

/**
 * Example implementation for a cache structure based on MapDB.
 * 
 * @author fabianletzkus
 *
 */
public class InMemoryCache implements ICache {

	DB db;
	HTreeMap<String, DefaultEntry> cache;

	/**
	 * Creates a new cache which only resides in memory.
	 */
	public InMemoryCache() {
		this.open();
	}

	@Override
	public DefaultEntry getEntry(DefaultEntry base) {
		return this.cache.get(base.getIdentifier());
	}

	@Override
	public void addEntry(DefaultEntry e) {
		if (this.cache.containsKey(e.getIdentifier())) {
			this.cache.remove(e.getIdentifier());
		}
		this.cache.put(e.getIdentifier(), e);
	}

	@Override
	public void removeEntry(DefaultEntry base) {
		this.cache.remove(base.getIdentifier());
	}

	@Override
	public Collection<DefaultEntry> getCollection() {
		return this.cache.values();
	}

	@Override
	public boolean entryExists(DefaultEntry base) {
		return this.cache.containsKey(base.getIdentifier());
	}

	@Override
	public int size() {
		return this.cache.size();
	}

	@Override
	public void commit() {
		db.commit();
	}

	@Override
	public void close() {
		this.commit();
		this.cache.close();
		this.db.close();
	}

	@Override
	public void open() {
		this.db = DBMaker.newMemoryDB().make();
		this.cache = db.createHashMap("cachestruct").makeOrGet();
	}

	@Override
	public void clear() {
		this.cache.clear();
	}
}
