package de.donotconnect.notary_cache.operator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import de.donotconnect.notary_cache.operator.Interfaces.IListener;

/**
 * Configuration for NotaryCache Operator
 * 
 * @author fabianletzkus
 *
 */
public class Configuration implements IListener {

	private static Configuration instance = null;

	/**
	 * Configuration is a singleton and must thus be created using
	 * getInstance();
	 * 
	 * @return
	 */
	public static synchronized Configuration getInstance() {
		if (Configuration.instance == null) {
			Configuration.instance = new Configuration();
		}
		return Configuration.instance;
	}

	private EventMgr e;
	private DB db;
	HTreeMap<String, String> config;
	private final static Logger log = LogManager.getLogger("Configuration");

	private Configuration() {

		this.e = EventMgr.getInstance();

		/**
		 * Init BC
		 */
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		/**
		 * Load configuration
		 */
		this.reload();

		/**
		 * Register Events
		 */
		// Reload configuration file
		this.e.registerEventListener("config-reload", this);
		// Regenerate Crypto keypair
		this.e.registerEventListener("config-generatePK", this);
		this.e.registerEventListener("set-keysize", this);
		// HW-info for private configuration
		this.e.registerEventListener("hwmon-notify", this);
		// A new cache was issued
		this.e.registerEventListener("cache-issued", this);
	}

	/**
	 * Returns the value for the given attribute key.
	 * 
	 * @param attr
	 * @return
	 */
	public String getAttribute(String attr) {
		if (attr.startsWith("internal.")) {
			log.info("Invalid configuration item retrieval.");
			return null;
		}
		return config.get(attr);
	}

	/**
	 * Sets the value for a given attribute.
	 * 
	 * @param attr
	 * @param value
	 */
	public void setAttribute(String attr, String value) {
		if (config.containsKey(attr))
			config.remove(attr);
		config.put(attr, value);
		db.commit();
	}

	/**
	 * React upon events. This method is usually not called by a user.
	 */
	public void doAction(String evnt) {
		log.debug("Received event: " + evnt);
		if (evnt.startsWith("config-reload")) {
			this.reload();
		}
		if (evnt.startsWith("config-generatePK")) {
			this.generatePK();
		}
		if (evnt.startsWith("set-keysize")) {
			String[] event = evnt.split(" ");
			if (event.length == 2) {
				this.setAttribute("config.keysize", event[1]);
				log.debug("Changed keysize. Sending config-regeneratePK.");
				this.e.newEvent("config-regeneratePK");
			}
		}
		if (evnt.startsWith("hwmon-notify")) {
			// TODO build statistics to adapt to environment
		}
		if (evnt.startsWith("cache-issued")) {
			String[] val = evnt.split(" ");
			if (val.length == 3) {
				this.setAttribute("cache.validity_start", val[1]);
				this.setAttribute("cache.validity_end", val[2]);
			}
		}
	}

	private void reload() {

		/**
		 * Clear existing configuration if present
		 */
		if (this.config != null && this.config.size() > 0) {
			this.config.clear();
			this.db.close();
		}

		/**
		 * Load user changeable configuration
		 */
		Properties attrs = new Properties();
		try {
			InputStream istream = this.getClass().getResourceAsStream(
					"/cache.properties");
			if (istream == null) {
				log.error("Configuration not found. Quitting.");
				this.e.newEvent("quit");
				System.exit(1);
			}
			attrs.load(istream);
			istream.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/**
		 * Generate internal data
		 * 
		 * TODO generate password randomly
		 */
		// Create encrypted map to store information in-memory
		db = DBMaker.newMemoryDB().encryptionEnable("generatemerandomly")
				.make();
		config = db.createHashMap("configuration").makeOrGet();
		// Add all elements to map
		Enumeration<Object> tmp = attrs.keys();
		while (tmp.hasMoreElements()) {
			String a = ((String) tmp.nextElement());
			this.config.put(a, attrs.getProperty(a));
		}
		// Generate Cryptodata
		config.put("crypto.keyalgo", "RSA");
		config.put("crypto.hashalgo", "SHA-256");
		config.put("crypto.signalgo", "SHA256withRSA");
		config.put("crypto.keysize", "4096");
		config.put("crypt.regeneration_period", "86400"); /* one day */
		this.generatePK();

		// Validate Configuration, e.g. check all needed fields
		if (!(config.containsKey("base.contact")
				|| config.containsKey("base.pgpid")
				|| config.containsKey("notary.protocol")
				|| config.containsKey("notary.uri")
				|| config.containsKey("replicates.uri")
				|| config.containsKey("replicates.probability")
				|| config.containsKey("hw.traffic")
				|| config.containsKey("hw.bandwidth")
				|| config.containsKey("instance.interface")
				|| config.containsKey("external.ip")
				|| config.containsKey("external.hostname")
				|| config.containsKey("instance.port")
				|| config.containsKey("external.port")
				|| config.containsKey("crypto.pubKey") || config
					.containsKey("internal.crypto.privKey"))) {
			log.error("Configuration is invalid. Quitting.");
			this.e.newEvent("quit");

			System.exit(1);
		}

		/**
		 * Generate data about this notary TODO
		 */
		config.put("instance.string_encoding", "UTF-8");
		config.put("instance.retry", "false");
		config.put("instance.retry_max_size", "20");
		config.put("cache.directory", System.getProperty("basedir") + "/var");
		config.put("cache.validity_start", "0");
		config.put("cache.validity_end", "0");
		config.put("cache.validity_period", "604800"); /* one week */
		config.put("cache.validity_delta", "3600"); /* one hour */

		db.commit();

	}

	private void generatePK() {
		KeyPairGenerator kpg;
		try {
			// Cleanup
			if (config.containsKey("crypto.pubKey"))
				config.remove("crypto.pubKey");
			if (config.containsKey("internal.crypto.privKey"))
				config.remove("internal.crypto.privKey");

			// Start generation...
			kpg = KeyPairGenerator.getInstance(config.get("crypto.keyalgo"));
			kpg.initialize(Integer.parseInt(config.get("crypto.keysize")));
			KeyPair kp = kpg.genKeyPair();

			// Save keypair to config
			config.put(
					"crypto.pubKey",
					Base64.getEncoder().encodeToString(
							kp.getPublic().getEncoded()));
			config.put("internal.crypto.privKey", Base64.getEncoder()
					.encodeToString(kp.getPrivate().getEncoded()));

		} catch (NoSuchAlgorithmException e) {
			log.error("Generation of cryptographic material failed: " + e);
		}
	}

	/**
	 * Creates a signature for the given string.
	 * 
	 * @param tosign
	 *            String to sign
	 * @return The signature of the string
	 */
	public String sign(String tosign) {
		byte[] decodedKey = Base64.getDecoder().decode(
				config.get("internal.crypto.privKey"));
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);

		try {
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PrivateKey privKey = fact.generatePrivate(keySpec);
			Signature signature = Signature.getInstance(
					config.get("crypto.signalgo"), "BC");
			signature.initSign(privKey, new SecureRandom());
			signature.update(tosign.getBytes(Charset.forName(config
					.get("instance.string_encoding"))));
			byte[] sigBytes = signature.sign();
			return Base64.getEncoder().encodeToString(sigBytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException
				| SignatureException | NoSuchProviderException
				| InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Returns the configuration as a string, e.g. for the notary-cache-config interface.
	 * @return
	 */
	public String getConfigAsString() {
		StringBuffer sb = new StringBuffer();

		sb.append("version=" + OperatorMain._NOTARY_CACHE_VERSION_ + "\n");
		sb.append("contact=" + this.getAttribute("base.contact") + "\n");
		sb.append("pgpid=" + this.getAttribute("base.pgpid") + "\n");
		sb.append("notary.protocol=" + this.getAttribute("notary.protocol")
				+ "\n");
		sb.append("replicates.uri=" + this.getAttribute("replicates.uri")
				+ "\n");
		sb.append("replicates.probability="
				+ this.getAttribute("replicates.probability") + "\n");
		sb.append("publickey=" + this.getAttribute("crypto.pubKey") + "\n");
		sb.append("publickey.validity="
				+ this.getAttribute("cache.validity_end") + "\n");

		return sb.toString();
	}
}
