package de.donotconnect.notary_cache.operator.CacheStrategyImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.donotconnect.notary_cache.operator.Configuration;
import de.donotconnect.notary_cache.operator.EventMgr;
import de.donotconnect.notary_cache.operator.CacheImpl.InMemoryCache;
import de.donotconnect.notary_cache.operator.CacheImpl.StandardEntry;
import de.donotconnect.notary_cache.operator.Interfaces.ICache;
import de.donotconnect.notary_cache.operator.Interfaces.ICacheStrategy;
import de.donotconnect.notary_cache.operator.Interfaces.AbstractEntry;
import de.donotconnect.notary_cache.operator.Interfaces.AbstractNotary;
import de.donotconnect.notary_cache.operator.NotaryImpl.DefaultNotary;

public class SimpleCachingStrategy implements ICacheStrategy {

	private ICache c;
	private EventMgr e;
	private AbstractNotary n;
	private final static Logger log = LogManager
			.getLogger("SimpleCachingStrategy");
	private Configuration conf;
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);

	public void manage(ICache c, AbstractNotary n) {
		conf = Configuration.getInstance();

		if (c != null)
			this.c = c;
		else
			this.c = new InMemoryCache();

		this.e = EventMgr.getInstance();

		if (n != null)
			this.n = n;
		else
			this.n = new DefaultNotary();

		// React upon hwmon-notify events
		this.e.registerEventListener("hwmon-notify", this);
		// React upon add-instructions
		// add-to-cache - Full entry
		this.e.registerEventListener("add-to-cache", this);
		// new-request - new requests which must be executed by the notary
		this.e.registerEventListener("new-request", this);
		// Debugging info
		this.e.registerEventListener("get-cache-info", this);
		this.e.registerEventListener("get-cache-full", this);
		this.e.registerEventListener("clear-cache", this);
	}

	public synchronized void doAction(String evnt) {

		log.debug("Received event: " + evnt);

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
			String[] val = evnt.split(" ");
			if (val.length == 7) {
				try {

					InetAddress ip = InetAddress.getByName(val[1]);
					int port = Integer.parseInt(val[2]);
					String hostname = val[3];
					String keyalgo = val[4];
					String digests = val[5];
					String roles = (val[6].equals("null"))? "0" : val[6];

					StandardEntry e = new StandardEntry(ip.getAddress(), port,
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
			log.debug(this.getCacheAsString());
		}
		if(evnt.startsWith("clear-cache")) {
			log.debug("Clearing cache..");
			Collection<AbstractEntry> ce = this.c.getCollection();
			for(AbstractEntry e : ce) {
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
		if (evnt.startsWith("new-request")) {
			/**
			 * new-requests has the following arguments:
			 * 
			 * Eventcode - IP - Port - Hostname - Algorithm - tryagain-flag
			 */
			String[] args = evnt.split(" ");
			if (args.length >= 5) {
				try {
					InetAddress ip = InetAddress.getByName(args[1]);
					if (!args[1].equals(ip.getHostAddress()))
						log.warn("IP was not in valid format. This could lead to issues at client side. (IN: "
								+ args[1] + " OUT:" + ip.getHostAddress() + ")");
					int port = Integer.parseInt(args[2]);

					this.n.setTargetHost(new StandardEntry(ip.getAddress(),
							port, args[3], args[4]));

					String[] digests = this.n.getDigestsFromTargetHost();

					// If notary didn't return a list of digests, try again
					// once, than quit.
					if (digests == null
							&& !(args.length == 6 && args[5].equals("false"))) {
						log.debug("Notary didn't return a result for " + ip
								+ "/" + args[3] + ":" + port
								+ ". Trying again in 8 hours.");
						final String eventcode;
						if (evnt.endsWith("true")) {
							eventcode = evnt.substring(0, evnt.length() - 6);
						} else {
							eventcode = evnt;
						}

						final Runnable scheduledRequest = new Runnable() {
							public void run() {
								EventMgr e = EventMgr.getInstance();
								e.newEvent(eventcode + " false");
							}
						};
						scheduler.schedule(scheduledRequest, 8, TimeUnit.HOURS);
						return;
					}

					StringBuffer sb = new StringBuffer();
					sb.append("add-to-cache");
					sb.append(" " + args[1]); // ip
					sb.append(" " + args[2]); // port
					sb.append(" " + args[3]); // hostname
					sb.append(" " + args[4]); // keyalgo
					if (!this.n.getHashalgo().equals(
							this.conf.getAttribute("crypto.hashalgo")))
						sb.append(" " + this.n.getHashalgo() + ":" + digests[0]);
					else
						sb.append(" " + digests[0]);
					for (int i = 1; i < digests.length; i++) {
						if (!this.n.getHashalgo().equals(
								this.conf.getAttribute("crypto.hashalgo")))
							sb.append("," + this.n.getHashalgo() + ":"
									+ digests[0]);
						else
							sb.append("," + digests[0]);
					}
					sb.append(" " + n.examRolesOnTargetHost());

					this.e.newEvent(sb.toString());

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}
	}

	/**
	 * Operations
	 */
	public String getCacheAsString() {

		// Get items
		Collection<AbstractEntry> cache = this.c.getCollection();
		StringBuilder result = new StringBuilder();
		StandardEntry se;

		// Build header
		result.append(conf.getAttribute("instance.ip") + ";"
				+ conf.getAttribute("instance.port") + ";"
				+ conf.getAttribute("instance.hostname") + ";"
				+ conf.getAttribute("cache.validity_start") + ";"
				+ conf.getAttribute("cache.validity_end") + ";"
				+ conf.getAttribute("crypto.hashalgo") + ";"
				+ conf.getAttribute("crypto.signalgo") + "\n");

		// Build body
		for (AbstractEntry entry : cache) {
			if (entry instanceof StandardEntry) {
				se = (StandardEntry) entry;
				result.append(se.getIPasString() + ";" + se.getPort() + ";"
						+ se.getHostname() + ";" + se.getKeyalgo() + ";"
						+ se.getDigestsAsString(',') + ";"
						+ se.getRolesAsString() + "\n");
			}
		}

		// Build footer
		MessageDigest md;
		try {
			// Calculate digest
			md = MessageDigest.getInstance(Configuration.getInstance()
					.getAttribute("crypto.hashalgo"));
			md.update(result.toString().getBytes(
					Charset.forName(conf
							.getAttribute("instance.string_encoding"))));

			// Calculate signature and append to result;
			result.append(conf.sign((new java.math.BigInteger(1, md.digest()))
					.toString(16)));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result.toString();

	}

	private void notifyOfIssuance() {
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
}
