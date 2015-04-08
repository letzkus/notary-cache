package de.donotconnect.notary_cache.operator.CacheImpl;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import de.donotconnect.notary_cache.operator.Interfaces.AbstractEntry;

/**
 * 
 * Entry has a fixed set of attributes which can be set and read. This set is
 * defined in the thesis and comprises of
 * 
 * - Identifying fields - List of digests created using crypto.hashalgo as
 * Strings, separated by semicolon - Roles of a target host
 * 
 * @author fabianletzkus
 *
 */
public class StandardEntry extends AbstractEntry {

	private static final long serialVersionUID = 1609105629315902042L;
	private ArrayList<String> digests = new ArrayList<String>();
	private int roles = 0;

	public StandardEntry(byte[] ip, int port, String hostname, String keyalgo)
			throws NoSuchAlgorithmException, UnknownHostException {
		super(ip, port, hostname, keyalgo);
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

}
