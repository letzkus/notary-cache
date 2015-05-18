package de.donotconnect.notary_cache.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;

/**
 * 
 * NCClient which provides functionality to automatically check caches from
 * NotaryCache.
 * 
 * @author fabianletzkus
 *
 */
public class NCClient {

	private final static Logger log = LogManager.getLogger("NCClient");

	public static NCClient instance = null;

	/**
	 * returns an Instance of NCClient
	 * 
	 * @return Instance of NCClient
	 */
	public static NCClient getInstance() {
		if (instance == null)
			instance = new NCClient();
		return instance;
	}

	private HashSet<Cache> activeCaches = new HashSet<Cache>();
	private String ownLocation = null;
	public String NCCLIENT_HASH_ALGORITHM = "SHA-256";

	/**
	 * Simple Constructor to create an object without any caches
	 */
	public NCClient() {
		ownLocation = getOwnLocation();
	}

	/**
	 * Constructor consuming an array of existing caches
	 * 
	 * @param caches
	 */
	public NCClient(Cache[] caches) {

		ownLocation = getOwnLocation();

		for (Cache c : caches)
			this.addCache(c);

	}

	/**
	 * Adds an existing cache to the NCClient
	 * 
	 * @param cache
	 */
	public void addCache(Cache cache) {
		if (cache.isValid())
			this.activeCaches.add(cache);
	}

	/**
	 * Loads existing caches from a directory to the NCClient
	 * 
	 * @param cacheLocation
	 *            The location to find the cache files
	 * @throws IOException
	 *             If errors occure while reading files
	 */
	public void addCachesFromDirectory(String cacheLocation) throws IOException {

		File loc = new File(cacheLocation);
		if (loc != null && loc.isDirectory()) {
			File[] cacheFiles = loc.listFiles();
			for (File cacheFile : cacheFiles) {
				if (cacheFile.getName().endsWith(".cache")) {
					Cache c;
					try {
						if (!cacheFile.isDirectory()) {
							c = Cache.fromFile(cacheFile.getAbsolutePath());
							c.updateConfig(cacheFile.getAbsolutePath()
									.substring(
											0,
											cacheFile.getAbsolutePath()
													.length() - 6)
									+ ".config");
							log.debug("Successfully loaded cache " + cacheFile);
							this.addCache(c);
						}
					} catch (CacheException e) {
						log.warn("Cache "
								+ cacheFile
								+ " contains errors and was not added to NCCLient: "
								+ e);
					}
				}
			}
		}
	}

	/**
	 * Returns the NCTrustManager object for the given configuration
	 * 
	 * @return
	 */
	public X509TrustManager getX509TrustManager() {
		return new NCTrustManager(this);
	}

	/**
	 * Retrieves new caches and updates existing caches.
	 * 
	 * @param depth
	 *            The number of repetitions of this process
	 * @param updateExistingCaches
	 *            Flag indicating whether to update the cache
	 * @param useNotaryCache
	 *            Flag indicating the use of Notary Cache during retrieval
	 */
	public void updateClient(int depth, boolean updateExistingCaches,
			boolean useNotaryCache) {

		HashSet<Cache> newCaches = new HashSet<Cache>();

		for (Cache c : this.activeCaches) {

			if (updateExistingCaches)
				try {
					c.update(useNotaryCache);
				} catch (KeyManagementException | NoSuchAlgorithmException
						| IOException e) {
				} catch (CacheException e) {
					log.error("Updating the cache failed.");
				}

			// Load new caches
			newCaches.addAll(retrieveCachesFromCache(c, true));

		}
		this.activeCaches.addAll(newCaches);
		if (depth != 0) {
			updateClient(depth - 1, false, useNotaryCache);
		}
	}

	/**
	 * Updates the client with a list of host, for example from history.
	 * 
	 * @param hosts
	 */
	public void updateClient(List<String> hosts) {
		for (String host : hosts) {
			InetAddress inethost;
			try {
				inethost = InetAddress.getByName(host);
				Cache c = new Cache(inethost, 443, false);
				this.activeCaches.add(c);
			} catch (CacheException | UnknownHostException e) {

			}
		}
	}

	/**
	 * Returns true, if the NCClient instance contains active caches.
	 * 
	 * @return
	 */
	public boolean hasCaches() {
		return this.activeCaches.size() > 0;
	}

	// ------------------------ Retrieval ------------------------
	private HashSet<Cache> retrieveCachesFromCache(Cache cache,
			boolean ignoreRolesFlag) {

		HashSet<Cache> newCaches = new HashSet<Cache>();

		Collection<DefaultEntry> coll = cache.getCache().getCollection();
		for (DefaultEntry e : coll) {
			try {
				if (ignoreRolesFlag
						|| Integer.parseInt((e.getRolesAsString())) > 0) {
					InetAddress host = InetAddress.getByAddress(
							e.getHostname(), e.getIP());
					Cache c = new Cache(host, e.getPort(), false);
					newCaches.add(c);
				}
			} catch (CacheException | UnknownHostException ex) {

			}
		}

		return newCaches;
	}

	// ------------------------ Selection ------------------------
	private String getLocation(String sIP) {
		try {
			HttpURLConnection urlcon = (HttpURLConnection) new URL(
					"http://ip2c.org/" + sIP).openConnection();
			urlcon.setDefaultUseCaches(false);
			urlcon.setUseCaches(false);
			urlcon.connect();
			InputStream is = urlcon.getInputStream();
			int c = 0;
			String s = "";
			while ((c = is.read()) != -1)
				s += (char) c;
			is.close();
			if (s.charAt(0) == '1') {
				String[] reply = s.split(";");
				return reply[1];
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "??";
	}

	private String getOwnLocation() {
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					whatismyip.openStream()));
			String ip = in.readLine();
			return this.getLocation(ip);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "??";
	}

	/**
	 * Select appropriate caches based on the ip. Moreover, the process can be
	 * manipulated by the flags.
	 * 
	 * @param ip
	 *            The ip of the target server
	 * @param ownLocation
	 *            Flag indicating the use of the clients location
	 * @param targetLocation
	 *            Flag indicating the use of the targets location
	 * @return A list of appropriate caches from all available caches
	 */
	public Cache[] selectAppropriateCaches(byte[] ip, boolean ownLocation,
			boolean targetLocation) {

		HashSet<Cache> appropriateCaches = new HashSet<Cache>();

		String locTarget = null;
		try {
			locTarget = this.getLocation(InetAddress.getByAddress(ip)
					.getHostAddress());

			for (Cache c : this.activeCaches) {

				String loc = c.getConfig("header.location");
				if (loc == null || loc.equals("")) {
					loc = this.getLocation(c.getConfig("header.ip"));
				}
				if (ownLocation) {
					if (!loc.equals(this.ownLocation)) {
						if (targetLocation) {
							if (!loc.equals(locTarget))
								appropriateCaches.add(c);
						} else
							appropriateCaches.add(c);
					}
				} else {
					if (targetLocation) {
						if (!loc.equals(locTarget))
							appropriateCaches.add(c);
					} else
						appropriateCaches.add(c);
				}
			}

		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (locTarget == null)
			return new Cache[] {};

		Cache[] result = new Cache[appropriateCaches.size()];
		appropriateCaches.toArray(result);

		return result;
	}
}
