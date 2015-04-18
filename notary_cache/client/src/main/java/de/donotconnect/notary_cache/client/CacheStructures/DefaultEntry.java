package de.donotconnect.notary_cache.client.CacheStructures;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class DefaultEntry implements Serializable {

	public static DefaultEntry fromString(String e)
			throws NoSuchAlgorithmException, UnknownHostException {
		DefaultEntry entry = null;
		String[] elem = e.split(";");
		if (elem.length >= 4) {
			byte[] ip = InetAddress.getByName(elem[0]).getAddress();
			int port = Integer.parseInt(elem[1]);
			String host = elem[2];
			String keyalgo = elem[3];
			entry = new DefaultEntry(ip, port, host, keyalgo);
			if(elem.length>=5) {
				String digests = elem[4];
				entry.addDigests(digests.split(";"));
				if(elem.length==6) {
					int roles = Integer.parseInt(elem[5]);
					entry.setRoles(roles);
				}
			}
		}
		return entry;
	}

	private static final long serialVersionUID = 3515210753976675556L;
	private final InetAddress host;
	private final int port;
	private final String keyalgo;
	private final String ident;
	private ArrayList<String> digests = new ArrayList<String>();
	private int roles = 0;

	public DefaultEntry(byte[] ip, int port, String hostname, String keyalgo)
			throws NoSuchAlgorithmException, UnknownHostException {
		this.host = InetAddress.getByAddress(hostname, ip);
		this.port = port;
		this.keyalgo = keyalgo;

		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update((this.host.toString() + ";" + String.valueOf(port) + ";"
				+ this.keyalgo + "").getBytes(Charset.forName("UTF-8")));
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

	public String getDigestsAsString(char delimiter) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.digests.size(); i++) {
			sb.append(this.digests.get(i));
			if (i < this.digests.size() - 1)
				sb.append(delimiter);
		}
		return sb.toString();
	}

	public String getRolesAsString() {
		return Integer.toString(this.roles);
	}

	public void addDigest(int pos, String digest) {
		this.digests.add(pos, digest);
	}

	public void setRoles(int roles) {
		this.roles = roles;
	}

	public void addDigests(String[] digests) {
		for (String d : digests)
			this.digests.add(d);
	}

	@Override
	public String toString() {
		char firstDelim = ';';
		char secondDelim = ',';
		return this.getIPasString() + firstDelim + this.getPort() + firstDelim
				+ this.getHostname() + firstDelim + this.getKeyalgo()
				+ firstDelim + this.getDigestsAsString(secondDelim)
				+ firstDelim + this.getRolesAsString() + System.lineSeparator();
	}

}
