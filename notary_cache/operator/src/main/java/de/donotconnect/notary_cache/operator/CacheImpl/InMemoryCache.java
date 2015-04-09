package de.donotconnect.notary_cache.operator.CacheImpl;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import de.donotconnect.notary_cache.operator.Configuration;
import de.donotconnect.notary_cache.operator.Interfaces.ICache;
import de.donotconnect.notary_cache.operator.Interfaces.DefaultEntry;

public class InMemoryCache implements ICache {

	DB db;
	HTreeMap<String, DefaultEntry> cache;

	public InMemoryCache() {
		this.open();
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
	public void open() {
		Configuration conf = Configuration.getInstance();
		this.db = DBMaker.newFileDB(new File(conf.getAttribute("cache.directory")+"/cache.db")).make();
		if (!conf.getAttribute("cache.entry_lifetime").equals("")
				|| !conf.getAttribute("cache.entry_lifetime").equals("0"))
			this.cache = db
					.createHashMap("cachestruct")
					.expireAfterAccess(
							Integer.valueOf(conf
									.getAttribute("cache.entry_lifetime")),
							TimeUnit.valueOf(conf
									.getAttribute("cache.entry_lifetime.unit")))
					.makeOrGet();
		else
			this.cache = db.createHashMap("cachestruct").makeOrGet();

	}
}
