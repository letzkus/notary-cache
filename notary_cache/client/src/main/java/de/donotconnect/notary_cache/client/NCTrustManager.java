package de.donotconnect.notary_cache.client;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

import de.donotconnect.notary_cache.client.NCClient.TargetHost;
import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;

public class NCTrustManager implements X509TrustManager {

	private TargetHost target;
	private Cache[] caches;
	private String defaultHashAlgo;
	private HashMap<String, Integer> probabilities = new HashMap<String, Integer>();

	public NCTrustManager(TargetHost t, Cache[] c, String defaultHashAlgo) {
		this.target = t;
		this.caches = c;
		this.defaultHashAlgo = defaultHashAlgo;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		try {

			DefaultEntry targetEntry = new DefaultEntry(this.target.ip,
					this.target.port, this.target.hostname, chain[0]
							.getPublicKey().getAlgorithm());
			
			String[] origDigests = new String[chain.length];
			
			MessageDigest md = MessageDigest
					.getInstance(this.defaultHashAlgo);
			
			for (int i = 0; i < chain.length; i++) {
				X509Certificate cert = chain[i];
				md.update(cert.getEncoded());
				origDigests[i] = (new java.math.BigInteger(1, md.digest()))
						.toString(16);
				md.reset();
			}

			for (Cache c : caches) {		

				String[] digests = getDigestsFromCache(targetEntry, c);

				int length = (digests.length > chain.length) ? chain.length
						: digests.length;
				int succ = 0;
				String differingDigest = null;

				for (int i = 0; i < length; i++) {
					if (origDigests[i].equals(digests[i])) {
						succ++;
					} else {
						differingDigest = digests[i];
					}
				}

				if (length > 0 && succ == length) {
					if (this.probabilities.containsKey(origDigests)) {
						Integer num = (this.probabilities.get(origDigests[0])) + 1;
						this.probabilities.put(origDigests[0], num);
					} else {
						this.probabilities.put(origDigests[0], 1);
					}
				} else {
					if (this.probabilities.containsKey(differingDigest)) {
						Integer num = (this.probabilities.get(differingDigest)) + 1;
						this.probabilities.put(differingDigest, num);
					} else {
						this.probabilities.put(differingDigest, 1);
					}
				}			
			}
			
			if (this.probabilities.size() > 1) {				
				Integer num = (this.probabilities.get(origDigests[0]));
				this.probabilities.remove(origDigests[0]);
				
				Set<String> probs = this.probabilities.keySet();
				for(String prob : probs) {
					Integer otherNum = this.probabilities.get(prob);
					if(otherNum > num) {
						throw new CertificateException("NotaryCache evaluated false.");
					}else{
						// Cases 2 and 3
					}
				}					
			}else{
				// Case 1
			}

		} catch (NoSuchAlgorithmException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
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

	private String getDigestFromLiveRequest(TargetHost t, Cache c) {

		return null;
	}

}
