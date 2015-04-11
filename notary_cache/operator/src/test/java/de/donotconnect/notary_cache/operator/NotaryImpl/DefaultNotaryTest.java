package de.donotconnect.notary_cache.operator.NotaryImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import de.donotconnect.notary_cache.operator.Interfaces.DefaultEntry;

public class DefaultNotaryTest {

	/**
	 * Just a simple test to check output..
	 * @throws HostConnectionException 
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException,
			UnknownHostException, HostConnectionException {

		DefaultNotary dn = new DefaultNotary();

		// SNI
		System.out.println("Alice: ");
		dn.setTargetHost(new DefaultEntry(InetAddress.getByName("85.25.46.13")
				.getAddress(), 443, "alice.sni.velox.ch", "RSA"));
		String[] digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);

		System.out.println("Bob:");
		dn.setTargetHost(new DefaultEntry(InetAddress.getByName("85.25.46.13")
				.getAddress(), 443, "sni.velox.ch", "RSA"));
		digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);
		
		// Keyalgos
		System.out.println("Facebook (RSA):");
		dn.setTargetHost(new DefaultEntry(InetAddress.getByName(
				"173.252.120.6").getAddress(), 443, "facebook.com", "RSA"));
		digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);

		System.out.println("Facebook (ECC):");
		dn.setTargetHost(new DefaultEntry(InetAddress.getByName(
				"173.252.120.6").getAddress(), 443, "facebook.com", "ECC"));
		digests = dn.getDigestsFromTargetHost();
		for (String digest : digests)
			System.out.println("Certificate: " + digest);
	}

}
