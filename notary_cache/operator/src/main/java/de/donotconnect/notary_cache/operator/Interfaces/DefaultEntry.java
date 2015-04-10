package de.donotconnect.notary_cache.operator.Interfaces;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import de.donotconnect.notary_cache.operator.Configuration;

public class DefaultEntry implements Serializable {
	
	public final String _ROLE_NONE_ = "0";
	public final String _ROLE_OPERATOR_ = "1";

	/**
	 * 
	 */
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
	
	public String getDigestsAsString(char delimiter) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<this.digests.size();i++) {
			sb.append(this.digests.get(i));
			if(i<this.digests.size()-1)
				sb.append(';');
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
	
	public void addDigests(String[] digests){
		for(String d : digests)
			this.digests.add(d);
	}
	
	@Override
	public String toString() {
		char firstDelim=';';
		char secondDelim=',';
		return this.getIPasString() + firstDelim + this.getPort() + firstDelim
				+ this.getHostname() + firstDelim + this.getKeyalgo() + firstDelim
				+ this.getDigestsAsString(secondDelim) + firstDelim
				+ this.getRolesAsString();
	}

}
