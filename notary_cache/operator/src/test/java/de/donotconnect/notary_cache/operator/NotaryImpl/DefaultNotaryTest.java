package de.donotconnect.notary_cache.operator.NotaryImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import de.donotconnect.notary_cache.operator.CacheImpl.StandardEntry;

public class DefaultNotaryTest {

	/**
	 * Just a simple test to check output..
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException,
			UnknownHostException {

		DefaultNotary dn = new DefaultNotary();

		// SNI
		System.out.println("Alice: ");
		dn.setTargetHost(new StandardEntry(InetAddress.getByName("85.25.46.13")
				.getAddress(), 443, "alice.sni.velox.ch", "RSA"));
		String[] digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);

		System.out.println("Bob:");
		dn.setTargetHost(new StandardEntry(InetAddress.getByName("85.25.46.13")
				.getAddress(), 443, "sni.velox.ch", "RSA"));
		digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);
		
		// Keyalgos
		System.out.println("Facebook (RSA):");
		dn.setTargetHost(new StandardEntry(InetAddress.getByName(
				"173.252.120.6").getAddress(), 443, "facebook.com", "RSA"));
		digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);

		System.out.println("Facebook (ECC):");
		dn.setTargetHost(new StandardEntry(InetAddress.getByName(
				"173.252.120.6").getAddress(), 443, "facebook.com", "ECC"));
		digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);
	}

}
