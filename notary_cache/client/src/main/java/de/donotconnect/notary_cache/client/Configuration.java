package de.donotconnect.notary_cache.client;

import java.io.File;
import java.security.Security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

public class Configuration {

	private DB db;
	HTreeMap<String, String> config;
	private final static Logger log = LogManager.getLogger("Configuration");

	public Configuration(String notarycache) {

		/**
		 * Init BC
		 */
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		/**
		 * Load configuration
		 */
		this.reload(notarycache);
	}

	public String getAttribute(String attr) {
		if (attr.startsWith("internal.")) {
			log.info("Invalid configuration item retrieval.");
			return null;
		}
		return config.get(attr);
	}

	public void setAttribute(String attr, String value) {
		if (config.containsKey(attr))
			config.remove(attr);
		config.put(attr, value);
		db.commit();
	}

	private void reload(String notarycache) {

		// Create encrypted map to store information in-memory
		db = DBMaker.newFileDB(new File(notarycache+"-config.db")).make();
		config = db.createHashMap("configuration").makeOrGet();
		
		// Add all elements to map
		config.put("instance.string_encoding", "UTF-8");
		config.put("crypto.hashalgo", "SHA-256");
		config.put("crypto.signalgo", "SHA256withRSA");

		db.commit();

	}

}
