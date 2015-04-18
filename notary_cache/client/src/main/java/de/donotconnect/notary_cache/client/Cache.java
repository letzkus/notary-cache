package de.donotconnect.notary_cache.client;

import java.io.BufferedReader;
import java.io.FileReader;
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
	private String ident = null;
	private ICache cache;
	private StringBuilder sCache;
	private Configuration config;
	private byte[] signature;

	/*
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 * 
	 * STATIC FUNCTIONS
	 * 
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Creates a new cache from a file. There is no validity checking done and
	 * no configuration is gathered.
	 * 
	 * @param cacheId
	 *            The id which uniquely identifies the cache
	 * @param filename
	 *            The filename of the text file containing the cache
	 * @return A cache object containing the cache.
	 * @throws IOException
	 *             if cache could not be opened.
	 */
	public static Cache fromFile(String cacheId, String filename)
			throws IOException {

		FileReader fr = new FileReader(filename);
		StringBuffer cacheContents = new StringBuffer();
		BufferedReader br = new BufferedReader(fr);
		String line;

		while ((line = br.readLine()) != null) {
			cacheContents.append(line);
		}
		br.close();
		fr.close();

		Cache c = null;
		c = new Cache(cacheId, cacheContents.toString());

		return c;
	}

	/*
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 * 
	 * CONSTRUCTORS
	 * 
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Constructs a new cache object from arbitrary input.
	 * 
	 * @param notaryCacheId
	 *            the id which uniquely identifies the cache
	 * @param fullCache
	 *            string containing the whole cache.
	 */
	public Cache(String notaryCacheId, String fullCache) {
		
		this.ident = notaryCacheId;
		
		_parseCache(notaryCacheId, fullCache);
	}

	/**
	 * Constructs a new cache object from host information. The cache is
	 * gathered via TLS-secured connection from the given port. No configuration
	 * is gathered.
	 * 
	 * @param notaryCacheId
	 *            An id which uniquely identifies the cache.
	 * @param host
	 *            The host from which is the cache is gathered. This could be
	 *            either an IP address or a hostname, which resolves to an IP
	 *            address
	 * @param port
	 *            the port of the host
	 * @param useNotaryCache
	 *            A flag indicating the use of Notary while gathering the new
	 *            cache.
	 * @throws CacheException
	 */
	public Cache(String notaryCacheId, InetAddress host, int port,
			boolean useNotaryCache) throws CacheException {
		
		this.ident = notaryCacheId;

		try {

			// Get configuration to make use of replicates
			this.config.setAttribute("header.ip", host.getHostAddress());
			this.config.setAttribute("header.port", String.valueOf(port));
			this.config.setAttribute("header.hostname", host.getHostName());
			gatherConfiguration(false);

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
			_parseCache(notaryCacheId, sb.toString());

		} catch (IOException | NoSuchAlgorithmException
				| KeyManagementException e) {
			throw new CacheException(e.toString());
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 * 
	 * PUBLIC METHODS
	 * 
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Checks, if the cache is valid. A check consists of checking the public
	 * keys validity and the signature and the validity dates of the cache.
	 * 
	 * @return a boolean indicating the validity of the cache.
	 */
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

	/**
	 * Returns the cache object.
	 * 
	 * @return the ICache-compatible object
	 */
	public ICache getCache() {
		return this.cache;
	}

	/**
	 * Returns the attribute of the configuration.
	 * 
	 * @param attr
	 *            Identifier for the requested attribute
	 * @return the value of the attribute
	 */
	public String getConfig(String attr) {
		return this.config.getAttribute(attr);
	}

	/**
	 * Sets a attribute in the configuration, which must not begin with
	 * "internal."
	 * 
	 * @param attr
	 *            Identifier for the attribute
	 * @param val
	 *            New value
	 */
	public void setConfig(String attr, String val) {
		this.config.setAttribute(attr, val);
	}

	/**
	 * Gathers the configuration from the information given in the cache.
	 * 
	 * @param useNotaryCache
	 *            boolean indicating the use of NotaryCache to validate received
	 *            certificates
	 */
	public void gatherConfiguration(boolean useNotaryCache) {
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
			if (useNotaryCache)
				sslContext.init(null, new TrustManager[] { cacheTrustManager },
						SecureRandom.getInstanceStrong());
			else
				sslContext.init(null, null, SecureRandom.getInstanceStrong());
			SSLSocket socket = (SSLSocket) sslContext.getSocketFactory()
					.createSocket(ip, port);
			SSLParameters p = new SSLParameters();
			ArrayList<SNIServerName> hostnames = new ArrayList<SNIServerName>();
			hostnames.add(new SNIHostName(hostname));
			p.setServerNames(hostnames);
			socket.setSSLParameters(p);
			socket.startHandshake();
			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			pw.println("GET /.well-known/notary-cache-config HTTP/1.1");
			pw.println("Host: " + hostname);
			pw.println("");
			pw.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			_parseConfig(br);
			br.close();
		} catch (IOException | NoSuchAlgorithmException
				| KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Gathers the configuration from a file.
	 * 
	 * WARNING: This method is dangerous, especially when used in conjunction
	 * with Cache.fromFile(..), as there is no checking of the origin of both
	 * the cache and the configuration.
	 * 
	 * @param filename
	 *            the name of the file which contains the configuration
	 */
	public void gatherConfiguration(String filename) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			_parseConfig(br);
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Updates the configuration and the cache using the network.
	 * 
	 * Note: This method is not implemented, yet.
	 */
	public void update() {

	}

	/**
	 * Returns the identifier for this cache.
	 * @return identifier
	 */
	public String getIdentifier() {
		return this.ident;
	}
	
	/*
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 * 
	 * PRIVATE METHODS
	 * 
	 * --------------------------------------------------------------------------
	 * ------------------------------------------------------------------------
	 */

	private void _parseCache(String notaryCacheId, String fullCache) {
		this.config = new Configuration(notaryCacheId);
		this.cache = new InMemoryCache(notaryCacheId);

		this.config.config.clear();
		this.cache.clear();

		String[] cache = fullCache.split("\n");

		sCache = new StringBuilder();
		for (int i = 0; i < cache.length; i++) {
			if (i == 0) { // Header

				_parseCacheHeader(cache[i]);
				sCache.append(cache[i] + "\n");

			} else if (i == cache.length - 1) { // Signature

				_parseCacheSignature(cache[i]);

			} else {

				try {
					this.cache.addEntry(DefaultEntry.fromString(cache[i]));
					sCache.append(cache[i] + "\n");
				} catch (NoSuchAlgorithmException | UnknownHostException e) {
					e.printStackTrace();
				}

			}
		}
	}

	private void _parseCacheHeader(String header) {
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

	private void _parseCacheSignature(String footer) {
		this.signature = Base64.getDecoder().decode(footer);
	}

	private void _parseConfig(BufferedReader br) throws IOException {
		String line;
		String[] var;
		while ((line = br.readLine()) != null) {
			var = line.split("=");
			this.config.setAttribute("config." + var[0],
					(var.length == 2) ? var[1] : "");
		}
	}
}