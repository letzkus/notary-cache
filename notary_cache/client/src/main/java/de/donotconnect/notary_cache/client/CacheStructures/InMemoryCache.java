package de.donotconnect.notary_cache.client.CacheStructures;

import java.io.File;
import java.util.Collection;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

public class InMemoryCache implements ICache {

	DB db;
	HTreeMap<String, DefaultEntry> cache;
	
	public InMemoryCache(String notarycache) {
		this.open(notarycache);
	}

	public InMemoryCache(DB db, HTreeMap<String, DefaultEntry> c) {
		this.db = db;
		this.cache = c;
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
	public void open(String notarycache) {
		this.db = DBMaker.newFileDB(new File(notarycache+"-cache.db")).make();
		this.cache = db.createHashMap("cachestruct").makeOrGet();
	}
	
	public void clear() {
		this.cache.clear();
	}
}
