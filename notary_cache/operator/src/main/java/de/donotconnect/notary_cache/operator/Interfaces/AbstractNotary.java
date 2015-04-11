package de.donotconnect.notary_cache.operator.Interfaces;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import de.donotconnect.notary_cache.operator.NotaryImpl.HostConnectionException;

public abstract class AbstractNotary {
	
	public static final int _SC_OK_	= 0;
	public static final int _SC_BAD_REQUEST_ = 1;
	public static final int _SC_NOT_FOUND_ = 2;
	public static final int _SC_REQUEST_SCHEDULED_ = 3;
	public static final int _SC_INTERNAL_ERROR_ = 4;

	protected InetAddress host;
	protected int port;
	protected String keyalgo;

	public void setTargetHost(DefaultEntry e) {
		try {
			this.host = InetAddress.getByAddress(e.getHostname(), e.getIP());
			this.port = e.getPort();
			this.keyalgo = e.getKeyalgo();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public abstract String[] getDigestsFromTargetHost() throws HostConnectionException;

	public abstract String examRolesOnTargetHost()  throws HostConnectionException;

	public abstract void handleRequest(String target, Request baseRequest,
			HttpServletRequest req, HttpServletResponse resp, ICacheStrategy cs);

}
