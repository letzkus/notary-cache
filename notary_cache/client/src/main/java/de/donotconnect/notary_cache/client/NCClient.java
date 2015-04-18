package de.donotconnect.notary_cache.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;

public class NCClient {

	public static NCClient instance = null;

	public static NCClient getInstance() {
		if (instance == null)
			instance = new NCClient();
		return instance;
	}

	public class TargetHost {
		public byte[] ip;
		public int port;
		public String hostname;
	}

	private HashMap<String, Cache> activeCaches = new HashMap<String, Cache>();
	private HashSet<String> harvestedCaches = new HashSet<String>();
	private String ownLocation = null;
	private String hashAlgo = "SHA-256";

	public NCClient() {
		
		ownLocation = getOwnLocation();

	}

	public NCClient(Cache[] caches) {

		ownLocation = getOwnLocation();
		
		for (Cache c : caches)
			this.addCache(c);

	}

	public void addCache(Cache cache) {
		if (cache.isValid())
			this.activeCaches.put(cache.getIdentifier(), cache);
	}

	public TargetHost createTargetHost(byte[] ip, int port, String hostname) {
		TargetHost t = new TargetHost();
		t.hostname = hostname;
		t.ip = ip;
		t.port = port;
		return t;
	}

	public X509TrustManager getX509TrustManager(TargetHost t) {
		Cache[] c = this.selectAppropriateCaches(t);
		return new NCTrustManager(t, c, this.hashAlgo);
	}

	public void updateClient(int depth, boolean updateExistingCaches) {

		Set<String> keys = this.activeCaches.keySet();
		for (String key : keys) {

			// Update existing caches
			Cache c = this.activeCaches.get(key);
			if (updateExistingCaches) {
				c.update();
				c.setConfig("header.location", this.getLocation(c.getConfig("header.ip")));
				this.activeCaches.put(key, c);
			}

			// Load new caches
			if (!harvestedCaches.contains(key)) {
				harvestedCaches.add(key);
				retrieveCachesFromCache(c, true);
			}

		}
		if (depth == 0) {
			this.harvestedCaches.clear();
			return;
		}
		updateClient(depth - 1, false);

	}

	public void updateClient(int depth) {
		updateClient(depth, true);
	}

	public void updateClient(List<String> history) {
		this.retrieveCachesFromList(history);
	}

	// ------------------------ Retrieval ------------------------
	private void retrieveCachesFromCache(Cache cache, boolean ignoreRolesFlag) {

		Collection<DefaultEntry> coll = cache.getCache().getCollection();
		for (DefaultEntry e : coll) {
			try {
				if (ignoreRolesFlag
						|| Integer.parseInt((e.getRolesAsString())) > 0) {
					InetAddress host = InetAddress.getByAddress(
							e.getHostname(), e.getIP());
					Cache c = new Cache(host.getHostAddress() + "."
							+ e.getPort(), host, e.getPort(), false);
					this.activeCaches.put(c.getIdentifier(), c);
				}
			} catch (CacheException | UnknownHostException ex) {

			}
		}
	}

	private void retrieveCachesFromList(List<String> hosts) {
		for (String host : hosts) {
			InetAddress inethost;
			try {
				inethost = InetAddress.getByName(host);
				Cache c = new Cache(inethost.getHostAddress() + ".443",
						inethost, 443, false);
				this.activeCaches.put(c.getIdentifier(), c);
			} catch (CacheException | UnknownHostException e) {

			}
		}
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
			return ip;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "??";
	}

	private Cache[] selectAppropriateCaches(TargetHost t) {
		
		HashSet<Cache> appropriateCaches = new HashSet<Cache>();
		
		Collection<String> keys = this.activeCaches.keySet();
		for(String key : keys) {
			Cache c = this.activeCaches.get(key);
			String loc = c.getConfig("header.location");
			if(loc == null || loc.equals("")) {
				loc = this.getLocation(c.getConfig("header.ip"));
			}
			if(!loc.equals(ownLocation)) {
				appropriateCaches.add(c);
			}
		}
		
		Cache[] result = new Cache[appropriateCaches.size()];
		appropriateCaches.toArray(result);
		
		return result;
	}
}
