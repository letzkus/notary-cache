package de.donotconnect.notary_cache.operator.Interfaces;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.donotconnect.notary_cache.operator.Configuration;

public abstract class AbstractEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3515210753976675556L;
	private final InetAddress host;
	private final int port;
	private final String keyalgo;
	private final String ident;

	public AbstractEntry(byte[] ip, int port, String hostname, String keyalgo)
			throws NoSuchAlgorithmException, UnknownHostException {
		this.host = InetAddress.getByAddress(hostname, ip);
		this.port = port;
		this.keyalgo = keyalgo;

		Configuration conf = Configuration.getInstance();

		MessageDigest md = MessageDigest.getInstance(conf
				.getAttribute("crypto.hashalgo"));
		md.update((this.host.toString() + ";" + String.valueOf(port) + ";"
				+ this.keyalgo + "").getBytes(Charset.forName(conf
				.getAttribute("instance.string_encoding"))));
		this.ident = (new java.math.BigInteger(1, md.digest())).toString(16);
	}

	public String getIdentifier() {
		return this.ident;
	}

	public String getHostname() {
		return host.getHostName();
	}

	public byte[] getIP() {
		return host.getAddress();
	}

	public String getIPasString() {
		return host.getHostAddress();
	}

	public int getPort() {
		return port;
	}

	public String getKeyalgo() {
		return keyalgo;
	}

}
