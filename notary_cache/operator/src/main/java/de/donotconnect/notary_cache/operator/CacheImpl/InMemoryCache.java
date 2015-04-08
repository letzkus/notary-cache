package de.donotconnect.notary_cache.operator.CacheImpl;

import java.io.File;
import java.util.Collection;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import de.donotconnect.notary_cache.operator.Interfaces.ICache;
import de.donotconnect.notary_cache.operator.Interfaces.AbstractEntry;

public class InMemoryCache implements ICache {

	DB db;
	HTreeMap<String, AbstractEntry> cache;

	public InMemoryCache() {
		this.db = DBMaker.newFileDB(new File("cache.db")).make();
		this.cache = db.createHashMap("cachestruct").makeOrGet();
	}

	public InMemoryCache(DB db, HTreeMap<String, AbstractEntry> c) {
		this.db = db;
		this.cache = c;
	}

	@Override
	public AbstractEntry getEntry(AbstractEntry base) {
		return this.cache.get(base.getIdentifier());
	}

	@Override
	public void addEntry(AbstractEntry e) {
		if(this.cache.containsKey(e.getIdentifier())) {
			this.cache.remove(e.getIdentifier());
		}
		this.cache.put(e.getIdentifier(), e);
		db.commit();
	}

	@Override
	public void removeEntry(AbstractEntry base) {
		this.cache.remove(base.getIdentifier());
		db.commit();
	}

	@Override
	public Collection<AbstractEntry> getCollection() {
		return this.cache.values();
	}

	@Override
	public boolean entryExists(AbstractEntry base) {
		return this.cache.containsKey(base.getIdentifier());
	}

	@Override
	public int size() {
		return this.cache.size();
	}

}
