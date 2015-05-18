package de.donotconnect.notary_cache.client;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;

public class NCTrustManager implements X509TrustManager {

	private final static Logger log = LogManager.getLogger("NCTrustManager");

	public class TargetHost {
		public byte[] ip;
		public int port;
		public String hostname;
	}

	private TargetHost target;
	private Cache[] caches;
	private String defaultHashAlgo;
	private HashMap<String, Integer> probabilities = new HashMap<String, Integer>();

	private NCClient client = null;

	public NCTrustManager(NCClient client) {
		this.client = client;
		this.defaultHashAlgo = this.client.NCCLIENT_HASH_ALGORITHM;
	}

	public void update(byte[] ip, int port, String hostname) {
		this.target = new TargetHost();
		this.target.ip = ip;
		this.target.port = port;
		this.target.hostname = hostname;
		this.caches = this.client.selectAppropriateCaches(ip);
		this.probabilities.clear();
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		try {

			if (this.caches.length == 0)
				throw new CertificateException("No caches to be checked..");

			DefaultEntry targetEntry = new DefaultEntry(this.target.ip,
					this.target.port, this.target.hostname, chain[0]
							.getPublicKey().getAlgorithm());

			String[] origDigests = new String[chain.length];

			MessageDigest md = MessageDigest.getInstance(this.defaultHashAlgo);

			for (int i = 0; i < chain.length; i++) {
				X509Certificate cert = chain[i];
				md.update(cert.getEncoded());
				origDigests[i] = (new java.math.BigInteger(1, md.digest()))
						.toString(16);
				md.reset();
			}

			log.debug("Original Digest: " + origDigests[0]);

			this.probabilities.put(origDigests[0], 1);

			for (Cache c : caches) {

				String[] digests = getDigestsFromCache(targetEntry, c);

				int length = (digests.length > chain.length) ? chain.length
						: digests.length;

				/**
				 * TODO By now, only the server certificates are checked. The CA
				 * certificates are NOT checked. Maybe change this behaviour in
				 * the future. Right now, checking of CA certificates will cause
				 * errors!
				 */
				length = (length != 0) ? 1 : 0;

				String differingDigest = null;

				if (!origDigests[0].equals(digests[0])) {
					log.debug("Differing Digest: " + digests[0]);
					differingDigest = digests[0];
				} else {
					differingDigest = null;
				}

				if (differingDigest == null) {
					Integer num = (this.probabilities.get(origDigests[0])) + 1;
					this.probabilities.put(origDigests[0], num);
				} else {
					if (this.probabilities.containsKey(differingDigest)) {
						Integer num = (this.probabilities.get(differingDigest)) + 1;
						this.probabilities.put(differingDigest, num);
					} else {
						this.probabilities.put(differingDigest, 1);
					}
				}
			}

			log.debug("Probabilities: " + this.probabilities);

			if (this.probabilities.size() > 1) {
				Integer num = (this.probabilities.get(origDigests[0]));
				this.probabilities.remove(origDigests[0]);

				Set<String> probs = this.probabilities.keySet();
				for (String prob : probs) {
					Integer otherNum = this.probabilities.get(prob);
					if (otherNum > num) {
						log.error("There are digests, which are more probable than the original digest.");
						throw new CertificateException(
								"NotaryCache evaluated false.");
					} else {
						if(otherNum == num) {
							log.warn("There are digests, which have the same probability than the original digest.");
						}else{
							log.warn("There are other digests, but they don't have a higher or same probability.");
							
						}
					}
				}
			} else {
				log.warn("There are no other digests.");
			}

		} catch (NoSuchAlgorithmException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		// TODO Auto-generated method stub

	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// TODO Auto-generated method stub
		return null;
	}

	// ------------------------ Evaluation ------------------------
	private String[] getDigestsFromCache(DefaultEntry e, Cache c) {

		DefaultEntry result = c.getCache().getEntry(e);
		String digests = result.getDigestsAsString(',');
		return digests.split(",");

	}

	@SuppressWarnings("unused")
	private String getDigestFromLiveRequest(TargetHost t, Cache c) {

		return null;
	}

}
