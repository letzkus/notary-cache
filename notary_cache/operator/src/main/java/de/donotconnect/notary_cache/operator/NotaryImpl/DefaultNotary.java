package de.donotconnect.notary_cache.operator.NotaryImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import de.donotconnect.notary_cache.operator.Configuration;
import de.donotconnect.notary_cache.operator.EventMgr;
import de.donotconnect.notary_cache.operator.Interfaces.AbstractNotary;
import de.donotconnect.notary_cache.operator.Interfaces.DefaultEntry;
import de.donotconnect.notary_cache.operator.Interfaces.ICacheStrategy;
import de.donotconnect.notary_cache.operator.Interfaces.IListener;

public class DefaultNotary extends AbstractNotary implements IListener {

	private EventMgr e;
	private final static Logger log = LogManager.getLogger("DefaultNotary");
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);

	public DefaultNotary() {
		e = EventMgr.getInstance();
		e.registerEventListener("new-request", this);
	}

	public class CertificateExtractor implements X509TrustManager {

		private ArrayList<String> digests = new ArrayList<String>();
		private MessageDigest md;

		public CertificateExtractor() {

			Configuration conf = Configuration.getInstance();
			try {
				this.md = MessageDigest.getInstance(conf
						.getAttribute("crypto.hashalgo"));
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public String[] getDigests() {

			String[] result = new String[digests.size()];
			digests.toArray(result);

			return result;
		}

		public void checkClientTrusted(final X509Certificate[] chain,
				final String authType) {
		}

		public void checkServerTrusted(final X509Certificate[] chain,
				final String authType) throws CertificateException {
			for (X509Certificate c : chain) {
				this.md.update(c.getEncoded());
				this.digests
						.add((new java.math.BigInteger(1, this.md.digest()))
								.toString(16));
				this.md.reset();
			}
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	@Override
	public String[] getDigestsFromTargetHost() {
		// TODO Auto-generated method stub

		CertificateExtractor extract = new CertificateExtractor();

		try {
			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { extract },
					SecureRandom.getInstanceStrong());

			SSLSocket socket = (SSLSocket) sslContext.getSocketFactory()
					.createSocket(this.host.getHostAddress(), this.port);

			// We need some parameters..
			SSLParameters p = new SSLParameters();
			// First: suited ciphersuites, not sorted, since we only care about
			// the certificate and not security
			// http://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html
			// https://tools.ietf.org/html/rfc5246#section-7.4.2
			// Not supported, yet: AES_256, SHA384
			if (this.keyalgo.equals("RSA"))
				p.setCipherSuites(new String[] {
						// "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
						// "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
						// "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
						// "TLS_RSA_WITH_AES_256_GCM_SHA384",
						// "TLS_RSA_WITH_AES_256_CBC_SHA256",
						// "TLS_RSA_WITH_AES_256_CBC_SHA",
						"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
						"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
						"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
						"TLS_RSA_WITH_AES_128_GCM_SHA256",
						"TLS_RSA_WITH_AES_128_CBC_SHA256",
						"TLS_RSA_WITH_AES_128_CBC_SHA" });
			else if (this.keyalgo.equals("ECC"))
				p.setCipherSuites(new String[] {
						// "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
						// "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
						// "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
						// "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
						// "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
						// "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
						// "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
						// "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
						// "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
						"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
						"TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
						"TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
						"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
						"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
						"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
						"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
						"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
						"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA" });
			else if (this.keyalgo.equals("DSA"))
				p.setCipherSuites(new String[] {
						// "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
						// "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
						// "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
						// "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
						"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
						"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
						"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256" });
			p.setProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" });

			// Second: SNI...
			ArrayList<SNIServerName> hostnames = new ArrayList<SNIServerName>();
			hostnames.add(new SNIHostName(this.host.getHostName()));
			p.setServerNames(hostnames);

			socket.setSSLParameters(p);

			socket.startHandshake();

			socket.close();

			return extract.getDigests();

		} catch (NoSuchAlgorithmException | KeyManagementException
				| IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String examRolesOnTargetHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleRequest(String target, Request baseRequest,
			HttpServletRequest req, HttpServletResponse resp, ICacheStrategy cs) {

		try {

			// Parse input
			String hostname = req.getParameter("hostname");
			String ip = req.getParameter("ip");
			int port = Integer
					.valueOf((req.getParameter("port") == null) ? "443" : req
							.getParameter("port"));
			String keyalgo = (req.getParameter("hostname") == null) ? "RSA"
					: req.getParameter("hostname");

			if (hostname == null) {
				if (ip == null) {
					// Both == null is bad...
					resp.setContentType("text/plain; charset=utf-8");
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				} else {
					hostname = InetAddress.getByName(ip).getCanonicalHostName();
				}
			} else {
				if (ip == null) {
					ip = InetAddress.getByName(hostname).getHostAddress();
				}
			}

			DefaultEntry entry = cs.getEntry(new DefaultEntry(InetAddress
					.getByName(ip).getAddress(), port, hostname, keyalgo));

			// If entry was not found in cache, create new request and return
			// 404
			if (entry == null) {
				// Issue Event to evaluate
				e.newEvent("new-request " + host.getHostAddress() + " " + port
						+ " " + host.getHostName() + " " + keyalgo);
				resp.setContentType("text/plain; charset=utf-8");
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// Else return entry;
			PrintWriter out = resp.getWriter();
			resp.setContentType("application/x-notarycache-entry; charset=utf-8");
			resp.setStatus(HttpServletResponse.SC_OK);
			out.println(entry.toString());

		} catch (IOException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void doAction(String evnt) {
		log.debug("Received event: " + evnt);
		Configuration conf = Configuration.getInstance();
		if (evnt.startsWith("new-request")) {
			/**
			 * new-requests has the following arguments:
			 * 
			 * Eventcode - IP - Port - Hostname - Algorithm - tryagain-flag
			 */
			String[] args = evnt.split(" ");
			if (args.length >= 5) {
				try {
					InetAddress ip = InetAddress.getByName(args[1]);
					if (!args[1].equals(ip.getHostAddress()))
						log.warn("IP was not in valid format. This could lead to issues at client side. (IN: "
								+ args[1] + " OUT:" + ip.getHostAddress() + ")");
					int port = Integer.parseInt(args[2]);

					this.setTargetHost(new DefaultEntry(ip.getAddress(), port,
							args[3], args[4]));

					String[] digests = this.getDigestsFromTargetHost();

					// If notary didn't return a list of digests, try again
					// once, than quit.
					if (digests == null
							&& !(args.length == 6 && args[5].equals("false"))) {
						log.debug("Notary didn't return a result for " + ip
								+ "/" + args[3] + ":" + port
								+ ". Trying again in 8 hours.");
						final String eventcode;
						if (evnt.endsWith("true")) {
							eventcode = evnt.substring(0, evnt.length() - 6);
						} else {
							eventcode = evnt;
						}

						final Runnable scheduledRequest = new Runnable() {
							public void run() {
								EventMgr e = EventMgr.getInstance();
								e.newEvent(eventcode + " false");
							}
						};
						scheduler.schedule(scheduledRequest, 8, TimeUnit.HOURS);
						return;
					}

					StringBuffer sb = new StringBuffer();
					sb.append("add-to-cache");
					sb.append(" " + args[1]); // ip
					sb.append(" " + args[2]); // port
					sb.append(" " + args[3]); // hostname
					sb.append(" " + args[4]); // keyalgo
					if (!this.getHashalgo().equals(
							conf.getAttribute("crypto.hashalgo")))
						sb.append(" " + this.getHashalgo() + ":" + digests[0]);
					else
						sb.append(" " + digests[0]);
					for (int i = 1; i < digests.length; i++) {
						if (!this.getHashalgo().equals(
								conf.getAttribute("crypto.hashalgo")))
							sb.append("," + this.getHashalgo() + ":"
									+ digests[0]);
						else
							sb.append("," + digests[0]);
					}
					sb.append(" " + this.examRolesOnTargetHost());

					log.debug("Sending event: " + sb.toString());
					this.e.newEvent(sb.toString());

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

}
