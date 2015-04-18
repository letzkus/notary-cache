package de.donotconnect.notary_cache.operator.CacheStrategyImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.donotconnect.notary_cache.operator.Configuration;
import de.donotconnect.notary_cache.operator.EventMgr;
import de.donotconnect.notary_cache.operator.CacheImpl.InMemoryCache;
import de.donotconnect.notary_cache.operator.Interfaces.ICache;
import de.donotconnect.notary_cache.operator.Interfaces.ICacheStrategy;
import de.donotconnect.notary_cache.operator.Interfaces.DefaultEntry;

public class SimpleCachingStrategy implements ICacheStrategy {

	private ICache c;
	private EventMgr e;
	private final static Logger log = LogManager
			.getLogger("SimpleCachingStrategy");
	private Configuration conf;
	private int maxCacheSize = 10000;

	public void manage(ICache c) {
		conf = Configuration.getInstance();

		if (c != null)
			this.c = c;
		else
			this.c = new InMemoryCache();

		this.e = EventMgr.getInstance();

		// React upon hwmon-notify events
		this.e.registerEventListener("hwmon-notify", this);
		// React upon add-instructions
		// add-to-cache - Full entry
		this.e.registerEventListener("add-to-cache", this);
		// quit - close cache and save changes
		this.e.registerEventListener("quit", this);
		// Debugging info
		this.e.registerEventListener("get-cache-info", this);
		this.e.registerEventListener("get-cache-full", this);
		this.e.registerEventListener("clear-cache", this);
		this.e.registerEventListener("set-cache-size", this);
	}

	public synchronized void doAction(String evnt) {

		log.debug("Received event: " + evnt);

		if (evnt.equals("quit")) {
			this.c.commit();
		}
		if (evnt.startsWith("set-cache-size")) {
			String[] event = evnt.split(" ");
			this.maxCacheSize = (event.length == 2) ? Integer
					.parseInt(event[1]) : this.maxCacheSize;
		}
		if (evnt.startsWith("hwmon-notify")) {
			String[] attrs = evnt.split(" ");
			for (String attr : attrs) {
				if (attr.startsWith("time=")) {
					long time = Long.parseLong(attr.split("=")[1]);
					String tmpValidity = conf
							.getAttribute("cache.validity_end");
					String tmpDelta = Configuration.getInstance().getAttribute(
							"cache.validity_delta");
					long validity = Long.parseLong(tmpValidity);
					long validityDelta = Long.parseLong(tmpDelta);
					if (validity <= time - validityDelta || validity == 0) {
						log.debug("Cache is expired. Renewing..");
						// Maybe delete old entries, cleanup, ...
						// For now, just renew validity
						notifyOfIssuance();
					}
				}
			}

		}
		if (evnt.startsWith("add-to-cache")) {
			/**
			 * new-requests has the following arguments:
			 * 
			 * Eventcode - IP - Port - Hostname - Algorithm - digests - roles
			 */
			if (this.maxCacheSize != 0 && c.size() >= this.maxCacheSize) {
				log.debug("maxCacheSize is reached. Add request is discarded.");
				return;
			}			
			String[] val = evnt.split(" ");
			if (val.length == 7) {
				try {

					InetAddress ip = InetAddress.getByName(val[1]);
					int port = Integer.parseInt(val[2]);
					String hostname = val[3];
					String keyalgo = val[4];
					String digests = val[5];
					String roles = (val[6].equals("null")) ? "0" : val[6];

					DefaultEntry e = new DefaultEntry(ip.getAddress(), port,
							hostname, keyalgo);
					String[] digest = digests.split(",");
					for (int i = 0; i < digest.length; i++)
						e.addDigest(i, digest[i]);
					e.setRoles(Integer.parseInt(roles));

					c.addEntry(e);
					notifyOfIssuance();

				} catch (UnknownHostException | NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		if (evnt.startsWith("get-cache-full")) {
			log.debug(this.getCacheAsString(';', ','));
		}
		if (evnt.startsWith("clear-cache")) {
			log.debug("Clearing cache..");
			Collection<DefaultEntry> ce = this.c.getCollection();
			for (DefaultEntry e : ce) {
				this.c.removeEntry(e);
			}
			this.e.newEvent("get-cache-info");
		}
		if (evnt.startsWith("get-cache-info")) {

			try {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				md.update(conf.getAttribute("crypto.pubKey").getBytes(
						Charset.forName(conf
								.getAttribute("instance.string_encoding"))));

				StringBuilder sb = new StringBuilder();
				sb.append("CACHE-INFO ");
				sb.append(" validity_start="
						+ conf.getAttribute("cache.validity_start"));
				sb.append(" validity_end="
						+ conf.getAttribute("cache.validity_end"));
				sb.append(" publickey="
						+ (new java.math.BigInteger(1, md.digest())).toString(
								16).substring(0, 16) + "...");
				sb.append(" #entries=" + String.valueOf(this.c.size()));
				log.debug(sb.toString());
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Operations
	 */
	public String getCacheAsString(char firstDelim, char secondDelim) {

		this.c.commit();

		// Get items
		Collection<DefaultEntry> cache = this.c.getCollection();
		StringBuilder result = new StringBuilder();

		// Build body
		for (DefaultEntry entry : cache) {
			result.append(entry.toString() + "\n");
		}

		// Return body
		return result.toString();

	}

	private void notifyOfIssuance() {
		this.c.commit();
		long now = System.currentTimeMillis() / 1000;
		long end = now
				+ Long.valueOf(conf.getAttribute("cache.validity_period"));

		StringBuilder event = new StringBuilder();
		event.append("cache-issued");
		event.append(" " + String.valueOf(now));
		event.append(" " + String.valueOf(end));

		log.debug("Sending event: " + event.toString());

		this.e.newEvent(event.toString());
	}

	@Override
	public DefaultEntry getEntry(DefaultEntry base) {
		return this.c.getEntry(base);
	}
}
