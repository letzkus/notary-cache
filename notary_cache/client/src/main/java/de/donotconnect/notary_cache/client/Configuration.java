package de.donotconnect.notary_cache.client;

import java.security.Security;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Configuration {

	HashMap<String, String> config;
	private final static Logger log = LogManager.getLogger("Configuration");

	public Configuration() {

		/**
		 * Init BC
		 */
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		/**
		 * Load configuration
		 */
		this.reload();
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
	}

	private void reload() {
		
		this.config = new HashMap<String, String>();
		
		// Add all elements to map
		config.put("cache.string_encoding", "UTF-8");
		config.put("crypto.hashalgo", "SHA-256");
		config.put("crypto.signalgo", "SHA256withRSA");

	}

}
