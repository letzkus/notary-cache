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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

//import org.bouncycastle.util.encoders.Base64;

import java.util.Base64;
import java.util.Random;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;
import de.donotconnect.notary_cache.client.CacheStructures.ICache;
import de.donotconnect.notary_cache.client.CacheStructures.InMemoryCache;


/**
 * Cache structure for NCClient.
 * 
 * @author fabianletzkus
 *
 */
public class Cache {
	private ICache cache;
	private StringBuilder sCache;
	private Configuration config;
	private byte[] signature;

	private final static Logger log = LogManager.getLogger("Cache");

	/*
	 * --------------------------------------------------------------------------
	 * 
	 * STATIC FUNCTIONS
	 * 
	 * --------------------------------------------------------------------------
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
	 * @throws CacheException
	 */
	public static Cache fromFile(String filename) throws IOException,
			CacheException {

		FileReader fr = new FileReader(filename);
		StringBuffer cacheContents = new StringBuffer();
		BufferedReader br = new BufferedReader(fr);
		String line;

		while ((line = br.readLine()) != null) {
			cacheContents.append(line + "\n");
		}
		br.close();
		fr.close();

		Cache c = null;
		c = new Cache(cacheContents.toString());

		return c;
	}

	/*
	 * --------------------------------------------------------------------------
	 * 
	 * CONSTRUCTORS
	 * 
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructs a new cache object from arbitrary input. No connection is
	 * established.
	 * 
	 * @param notaryCacheId
	 *            the id which uniquely identifies the cache
	 * @param fullCache
	 *            string containing the whole cache.
	 * @throws CacheException
	 */
	public Cache(String fullCache) throws CacheException {

		_parseCache(fullCache);
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
	public Cache(InetAddress host, int port, boolean useNotaryCache)
			throws CacheException {

		try {

			// Get configuration to make use of replicates
			this.config.setAttribute("header.ip", host.getHostAddress());
			this.config.setAttribute("header.port", String.valueOf(port));
			this.config.setAttribute("header.hostname", host.getHostName());

			// Choose potential random replicate
			this.update(useNotaryCache);

		} catch (IOException | NoSuchAlgorithmException
				| KeyManagementException e) {
			throw new CacheException(e.toString());
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * 
	 * PUBLIC METHODS
	 * 
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Checks, if the cache is valid. A check consists of checking the public
	 * keys validity and the signature and the validity dates of the cache.
	 * 
	 * @return a boolean indicating the validity of the cache.
	 */
	public boolean isValid() {

		long now = System.currentTimeMillis() / 1000;

		// Public Key Validity
		String sPublicKey = this.config.getAttribute("config.publickey");
		String pkValidity = this.config
				.getAttribute("config.publickey.validity");
		if (sPublicKey == null
				|| (pkValidity != null && now > Long.parseLong(pkValidity))) {

			log.debug("Cache is not valid: (" + (sPublicKey == null)
					+ " != false && " + now + " > "
					+ Long.parseLong(pkValidity) + ")");

			return false;
		}

		// Cache Validity
		if (now < Long.parseLong(this.config
				.getAttribute("header.validity_start"))
				&& now > Long.parseLong(this.config
						.getAttribute("header.validity_end"))) {
			log.debug("Cache is outside its validity.");
			return false;
		}

		// Signature Validity
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(sPublicKey));
		KeyFactory fact;
		try {
			fact = KeyFactory.getInstance("RSA");
			PublicKey pubKey = fact.generatePublic(keySpec);
			
			MessageDigest md;
			String digest = null;
			// Calculate digest
			try {
				md = MessageDigest.getInstance(this.config
						.getAttribute("crypto.hashalgo"));
				md.update(sCache.toString().getBytes(
						Charset.forName(this.config
								.getAttribute("cache.string_encoding"))));
				digest = (new java.math.BigInteger(1, md
						.digest())).toString(16);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(digest==null)
				return false;
			
			Signature verifier = Signature.getInstance(
					config.getAttribute("crypto.signalgo"), "BC");
			verifier.initVerify(pubKey);
			verifier.update(digest.getBytes(
					Charset.forName(this.config
							.getAttribute("cache.string_encoding"))));
			boolean result = verifier.verify(this.signature);
			if (!result){
				log.debug("Cache Signature was not valid: ");
				log.debug("- Public Key: "+sPublicKey);
				log.debug("- Signature: "+Base64.getDecoder().decode(this.signature));
				log.debug("- Digest: ");
			}
			return result;
		} catch (NoSuchAlgorithmException | SignatureException
				| InvalidKeyException | NoSuchProviderException
				| InvalidKeySpecException | IllegalArgumentException e) {
			log.debug("Cache is not valid, because some exception was thrown: "
					+ e);
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
	 * Gathers the configuration from the information given in the cache using
	 * TLS connection to NotaryCache host.
	 * 
	 * @param useNotaryCache
	 *            boolean indicating the use of NotaryCache to validate received
	 *            certificates
	 */
	public void updateConfig(boolean useNotaryCache) {
		/**
		 * version=1 contact= pgpid= notary.protocol=Default replicates.uri=
		 * replicates.probability= publickey= publickey.validity=
		 */

		String ip = this.config.getAttribute("header.ip");
		String hostname = this.config.getAttribute("header.hostname");
		int port = Integer.parseInt(this.config.getAttribute("header.port"));

		try {

			NCClient client = NCClient.getInstance();
			X509TrustManager cacheTrustManager = client.getX509TrustManager();

			((NCTrustManager) cacheTrustManager).update(
					InetAddress.getByName(ip).getAddress(), port, hostname);

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

			this.config.setAttribute("internal.config_avail", "true");
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
	 * @throws CacheException
	 */
	public void updateConfig(String filename) throws CacheException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			_parseConfig(br);
			br.close();
			log.debug("Successfully loaded configuration: " + filename);
		} catch (IOException e) {
			log.debug("Error loading configuration: " + filename);
			throw new CacheException("Configuration not found for cache.");
		}
	}

	/**
	 * Updates the configuration and the cache using the network.
	 * 
	 * Note: This method is not implemented, yet.
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws UnknownHostException
	 * @throws KeyManagementException
	 * @throws CacheException
	 */
	public void update(boolean useNotaryCache) throws KeyManagementException,
			UnknownHostException, NoSuchAlgorithmException, IOException,
			CacheException {
		updateConfig(useNotaryCache);
		updateCache(useNotaryCache);
	}

	/*
	 * --------------------------------------------------------------------------
	 * 
	 * PRIVATE METHODS
	 * 
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Parses the complete cache and sets both the configuration parameters as
	 * well as creates the cache
	 * 
	 * @param notaryCacheId
	 * @param fullCache
	 */
	private void _parseCache(String fullCache) throws CacheException {

		this.config = new Configuration();
		this.cache = new InMemoryCache();

		String[] cache = fullCache.split("\\n");
		log.debug("Cache has " + cache.length + " lines.");

		sCache = new StringBuilder();
		for (int i = 0; i < cache.length; i++) {
			if (i == 0) { // Header

				log.debug("_parseCache: Parsing cache header... ");

				_parseCacheHeader(cache[i]);
				sCache.append(cache[i] + "\n");

			} else if (i == cache.length - 1) { // Signature

				log.debug("_parseCache: Parsing cache signature... ");
				_parseCacheSignature(cache[i]);

			} else {

				// log.debug("_parseCache: Adding entry...");

				try {
					this.cache.addEntry(DefaultEntry.fromString(cache[i]));
					sCache.append(cache[i] + "\n");
				} catch (NoSuchAlgorithmException | UnknownHostException e) {
					e.printStackTrace();
					log.debug("Failed to add entry: " + e);
					throw new CacheException(
							"At least one entry is not correct.");
				}

			}
		}
	}

	/**
	 * parses the first line of a cache and sets the configuration
	 * 
	 * @param header
	 * @throws CacheException
	 */
	private void _parseCacheHeader(String header) throws CacheException {

		String[] params = header.split(";");
		if (params.length != 7) {
			log.debug("Header is not correct. Length is " + params.length);
			throw new CacheException("Header is not correct.");
		}

		this.config.setAttribute("header.ip", params[0]);
		this.config.setAttribute("header.port", params[1]);
		this.config.setAttribute("header.hostname", params[2]);
		this.config.setAttribute("header.validity_start", params[3]);
		this.config.setAttribute("header.validity_end", params[4]);
		this.config.setAttribute("crypto.hashalgo", params[5]);
		this.config.setAttribute("crypto.signalgo", params[6]);
	}

	/**
	 * Decodes the signature
	 * 
	 * @param footer
	 * @throws CacheException
	 */
	private void _parseCacheSignature(String footer) throws CacheException {
		try {
			this.signature = Base64.getDecoder().decode(footer);
		} catch (IllegalArgumentException e) {
			log.debug("Signature is not correctly decoded.");
			throw new CacheException("Signature is not correctly decoded.");
		}
	}

	/**
	 * Parses the configuration.
	 * 
	 * @param br
	 * @throws IOException
	 */
	private void _parseConfig(BufferedReader br) throws IOException {
		String line;
		String[] var;
		while ((line = br.readLine()) != null) {
			var = line.split("=");
			this.config.setAttribute("config." + var[0],
					(var.length == 2) ? var[1] : "");
		}
	}

	/**
	 * Gathers a cache via TLS connection
	 * 
	 * @param useNotaryCache
	 * @throws UnknownHostException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws CacheException
	 */
	private void updateCache(boolean useNotaryCache)
			throws UnknownHostException, NoSuchAlgorithmException,
			KeyManagementException, IOException, CacheException {

		InetAddress host = InetAddress.getByName(this.config
				.getAttribute("header.ip"));
		host = InetAddress.getByAddress(
				this.config.getAttribute("header.hostname"), host.getAddress());
		int port = Integer.parseInt(this.config.getAttribute("header.port"));
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
					host = InetAddress.getByName(replicates[idx].split(":")[0]);
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
			X509TrustManager cacheTrustManager = client.getX509TrustManager();
			((NCTrustManager) cacheTrustManager).update(host.getAddress(),
					port, host.getHostName());

			sslContext.init(null, new TrustManager[] { cacheTrustManager },
					SecureRandom.getInstanceStrong());
		} else {
			sslContext.init(null, null, SecureRandom.getInstanceStrong());
		}
		socket = (SSLSocket) sslContext.getSocketFactory().createSocket(host,
				port);
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
		_parseCache(sb.toString());
	}
}