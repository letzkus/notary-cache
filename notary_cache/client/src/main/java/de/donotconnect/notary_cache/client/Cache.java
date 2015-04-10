package de.donotconnect.notary_cache.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.donotconnect.notary_cache.client.NCClient.TargetHost;
import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;
import de.donotconnect.notary_cache.client.CacheStructures.ICache;
import de.donotconnect.notary_cache.client.CacheStructures.InMemoryCache;

public class Cache {
	private ICache cache;
	private StringBuilder sCache;
	private Configuration config;
	private byte[] signature;

	public Cache(String notaryCacheId) throws CacheException {
		this.config = new Configuration(notaryCacheId);
		this.cache = new InMemoryCache(notaryCacheId);
		if (!this.isValid()) {
			throw new CacheException(
					"Cache is not valid or cache does not exist.");
		}
	}

	public Cache(String notaryCacheId, String fullCache) throws CacheException {
		createCache(notaryCacheId, fullCache);
	}

	public void createCache(String notaryCacheId, String fullCache)
			throws CacheException {
		this.config = new Configuration(notaryCacheId);
		this.cache = new InMemoryCache(notaryCacheId);

		this.config.config.clear();
		this.cache.clear();

		String[] cache = fullCache.split("\n");

		sCache = new StringBuilder();
		for (int i = 0; i < cache.length; i++) {
			if (i == 0) { // Header

				parseCacheHeader(cache[i]);
				sCache.append(cache[i] + "\n");

			} else if (i == cache.length - 1) { // Signature

				parseCacheSignature(cache[i]);

			} else {

				try {
					this.cache.addEntry(DefaultEntry.fromString(cache[i]));
					sCache.append(cache[i] + "\n");
				} catch (NoSuchAlgorithmException | UnknownHostException e) {
					e.printStackTrace();
				}

			}
		}
		getAndParseConfiguration();
		if (!this.isValid()) {
			throw new CacheException("Cache is not valid");
		}
	}

	public Cache(String notaryCacheId, InetAddress host, int port,
			boolean useNotaryCache) throws CacheException {

		try {

			// Get configuration to make use of replicates
			this.config.setAttribute("header.ip", host.getHostAddress());
			this.config.setAttribute("header.port", String.valueOf(port));
			this.config.setAttribute("header.hostname", host.getHostName());
			getAndParseConfiguration();

			// Choose potential random replicate
			boolean replicate = false;
			String replicatesAttr = this.config
					.getAttribute("config.replicates.uri");
			if (!replicatesAttr.equals("")) {
				float r = Float.parseFloat(this.config
						.getAttribute("config.replicates.probability")) * 10;
				Random rand = new Random();
				if (r < rand.nextInt(100)) {
					String[] replicates = replicatesAttr.split(",");
					int idx = rand.nextInt(replicates.length);
					if (replicates[idx].contains(":")) {
						host = InetAddress
								.getByName(replicates[idx].split(":")[0]);
						port = Integer.parseInt(replicates[idx].split(":")[1]);
					} else {
						host = InetAddress.getByName(replicates[idx]);
						port = 80;
					}
					replicate = true;
				}
			}

			// Get cache from host
			SSLSocket socket = null;

			final SSLContext sslContext = SSLContext.getInstance("TLS");
			if (useNotaryCache) {
				NCClient client = NCClient.getInstance();
				TargetHost t = client.createTargetHost(host.getAddress(), port,
						host.getHostName());
				X509TrustManager cacheTrustManager = client
						.getX509TrustManager(t);
				sslContext.init(null, new TrustManager[] { cacheTrustManager },
						SecureRandom.getInstanceStrong());
			} else {
				sslContext.init(null, null, SecureRandom.getInstanceStrong());
			}
			socket = (SSLSocket) sslContext.getSocketFactory().createSocket(
					host, port);
			SSLParameters p = new SSLParameters();
			ArrayList<SNIServerName> hostnames = new ArrayList<SNIServerName>();
			hostnames.add(new SNIHostName(host.getHostAddress()));
			p.setServerNames(hostnames);
			socket.setSSLParameters(p);
			socket.startHandshake();
			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			if (replicate) {
				String origIP = this.config.getAttribute("header.ip");
				String origPort = this.config.getAttribute("header.port");
				pw.println("GET /.well-known/notary-cache-replicate/" + origIP
						+ "/" + origPort + " HTTP/1.1");
			} else {
				pw.println("GET /.well-known/notary-cache HTTP/1.1");
			}
			pw.println("Host: " + host.getHostName());
			pw.println("");
			pw.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String recv;
			StringBuffer sb = new StringBuffer();
			while ((recv = br.readLine()) != null)
				sb.append(recv);
			br.close();
			
			// Create cache
			createCache(notaryCacheId, sb.toString());
		} catch (IOException | NoSuchAlgorithmException
				| KeyManagementException e) {
			throw new CacheException(e.toString());
		}
	}

	public boolean isValid() {

		long now = System.currentTimeMillis();

		// Public Key Validity
		String sPublicKey = this.config.getAttribute("config.publicKey");
		String pkValidity = this.config
				.getAttribute("config.publicKey.validity");
		if (sPublicKey != null
				|| (pkValidity != null && now > Long.parseLong(pkValidity))) {
			return false;
		}

		// Cache Validity
		if (now < Long.parseLong(this.config
				.getAttribute("header.validity_start"))
				&& now > Long.parseLong(this.config
						.getAttribute("header.validity_end")))
			return false;

		// Signature Validity
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64
				.getDecoder().decode(sPublicKey));
		KeyFactory fact;
		try {
			fact = KeyFactory.getInstance("RSA");

			PublicKey pubKey = fact.generatePublic(keySpec);
			Signature verifier = Signature.getInstance(
					config.getAttribute("crypto.signalgo"), "BC");
			verifier.initVerify(pubKey);
			verifier.update(sCache.toString().getBytes(
					Charset.forName(this.config
							.getAttribute("cache.string_encoding"))));
			return verifier.verify(this.signature);
		} catch (NoSuchAlgorithmException | SignatureException
				| InvalidKeyException | NoSuchProviderException
				| InvalidKeySpecException e) {
			return false;
		}
	}

	public ICache getCache() {
		return this.cache;
	}

	public String getConfig(String attr) {
		return this.config.getAttribute(attr);
	}

	public void setConfig(String attr, String val) {
		this.config.setAttribute(attr, val);
	}

	private void parseCacheHeader(String header) {
		String[] params = header.split(";");
		if (params.length != 7) {
			return;
		}

		this.config.setAttribute("header.ip", params[0]);
		this.config.setAttribute("header.port", params[1]);
		this.config.setAttribute("header.hostname", params[2]);
		this.config.setAttribute("header.validity_start", params[3]);
		this.config.setAttribute("header.validity_end", params[4]);
		this.config.setAttribute("crypto.hashalgo", params[5]);
		this.config.setAttribute("crypto.signalgo", params[6]);
	}

	private void parseCacheSignature(String footer) {
		this.signature = Base64.getDecoder().decode(footer);
	}

	private void getAndParseConfiguration() {
		/**
		 * version=1 contact= pgpid= notary.protocol=Default replicates.uri=
		 * replicates.probability= publickey= publickey.validity=
		 */

		String ip = this.config.getAttribute("header.ip");
		String hostname = this.config.getAttribute("header.hostname");
		int port = Integer.parseInt(this.config.getAttribute("header.port"));

		try {

			NCClient client = NCClient.getInstance();
			TargetHost t = client.createTargetHost(InetAddress.getByName(ip)
					.getAddress(), port, hostname);
			X509TrustManager cacheTrustManager = client.getX509TrustManager(t);

			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { cacheTrustManager },
					SecureRandom.getInstanceStrong());
			SSLSocket socket = (SSLSocket) sslContext.getSocketFactory()
					.createSocket(ip, port);
			SSLParameters p = new SSLParameters();
			ArrayList<SNIServerName> hostnames = new ArrayList<SNIServerName>();
			hostnames.add(new SNIHostName(hostname));
			p.setServerNames(hostnames);
			socket.setSSLParameters(p);
			socket.startHandshake();
			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			pw.println("GET /.well-known/notary-cache-configuration HTTP/1.1");
			pw.println("Host: " + hostname);
			pw.println("");
			pw.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String recv;
			String[] var;
			while ((recv = br.readLine()) != null) {
				var = recv.split("=");
				this.config.setAttribute("config." + var[0],
						(var.length == 2) ? var[1] : "");
			}
			br.close();
		} catch (IOException | NoSuchAlgorithmException
				| KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void update() {

	}

	public String getIdent() {
		return this.config.getAttribute("header.ip") + ":"
				+ this.config.getAttribute("header.port");
	}
}