package de.donotconnect.notary_cache.operator.NotaryImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLProtocolException;
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

/**
 * DefaultNotary provides an implementation of a notary, which checks target
 * hosts in real time. No examination of a target host is implemented, yet.
 * 
 * @author fabianletzkus
 *
 */
public class DefaultNotary extends AbstractNotary implements IListener {

	private EventMgr e;
	private final static Logger log = LogManager.getLogger("DefaultNotary");
	private final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);
	private ArrayList<InetAddress> scheduledHosts = new ArrayList<InetAddress>();
	public static int maxThreads = 100;
	private static int numThreads = 0;

	/**
	 * Default constructor.
	 */
	public DefaultNotary() {
		e = EventMgr.getInstance();
		e.registerEventListener("new-request", this);
	}

	/**
	 * Class to extract certificates during connection establishment.
	 * 
	 * @author fabianletzkus
	 *
	 */
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

		/**
		 * Returns the collected certificates as digests.
		 * 
		 * @return digests of the certificates
		 */
		public String[] getDigests() {

			String[] result = new String[digests.size()];
			digests.toArray(result);

			return result;
		}

		/**
		 * see parent class
		 */
		public void checkClientTrusted(final X509Certificate[] chain,
				final String authType) {
		}

		/**
		 * see parent class
		 */
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

		/**
		 * see parent class
		 */
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	@Override
	public String[] getDigestsFromTargetHost() throws HostConnectionException {
		// TODO Auto-generated method stub

		log.debug("Getting digests from host " + host);

		log.debug("Instanciating certificate extractor.");
		CertificateExtractor extract = new CertificateExtractor();

		SSLSocket socket = null;
		try {
			// log.debug("Creating SSLContext.");
			final SSLContext sslContext = SSLContext.getInstance("TLSv1");
			// log.debug("Initiating SSLContext with TrustManager and SecureRandom");
			sslContext.init(null, new TrustManager[] { extract }, null);

			// log.debug("Creating socket..");
			socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
			// log.debug("Connecting to socket...");
			socket.connect(new InetSocketAddress(this.host.getHostAddress(),
					this.port), Integer.parseInt(Configuration.getInstance()
					.getAttribute("notary.timeout"))); //
			socket.setSoTimeout(Integer.parseInt(Configuration.getInstance()
					.getAttribute("notary.timeout")));

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

			// log.debug("Starting handshake..");
			socket.startHandshake();

			// log.debug("Closing socket..");
			socket.close();

			// log.debug("Returning digests..");
			return extract.getDigests();

		} catch (SocketTimeoutException e) {
			// ConnectException -> no TLS-capable service @host
			// SSLHandshakeException -> wrong keyalgo
			// Maybe try later
			log.debug("Can't connect to " + host + ": " + e);
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					log.debug("IOException during socket.close(): " + e);
				}
			return null;
		} catch (SSLProtocolException | UnknownHostException
				| SSLHandshakeException | java.net.ConnectException e) {
			throw new HostConnectionException(e.getMessage());
		} catch (NoSuchAlgorithmException | KeyManagementException
				| IOException e) {

		}

		return null;
	}

	@Override
	public String examRolesOnTargetHost() {
		// TODO Auto-generated method stub
		return "0";
	}

	@Override
	public void handleRequest(String target, Request baseRequest,
			HttpServletRequest req, HttpServletResponse resp, ICacheStrategy cs) {

		String hostname = "";

		try {

			resp.setContentType("text/plain; charset=utf-8");
			PrintWriter out = resp.getWriter();

			// Parse input
			hostname = req.getParameter("hostname");
			String ip = req.getParameter("ip");
			int port = Integer
					.valueOf((req.getParameter("port") == null) ? "443" : req
							.getParameter("port"));
			String keyalgo = (req.getParameter("keyalgo") == null) ? "RSA"
					: req.getParameter("keyalgo");

			if (hostname == null) {
				if (ip == null) {
					// Both == null is bad...
					resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.println(_SC_BAD_REQUEST_);
					return;
				} else {
					hostname = InetAddress.getByName(ip).getCanonicalHostName();
				}
			} else {
				if (ip == null) {
					ip = InetAddress.getByName(hostname).getHostAddress();
				}
			}

			final String finalIp = ip;
			final String finalHost = hostname;

			log.debug("Received http request " + hostname + "/" + ip
					+ ". Looking up cache.");

			DefaultEntry entry = cs.getEntry(new DefaultEntry(InetAddress
					.getByName(ip).getAddress(), port, hostname, keyalgo));

			// If entry was not found in cache, create new request and return
			// 404
			if (entry == null) {
				// Issue Event to evaluate
				log.debug("Entry not found in cache, adding it.");

				final Runnable request = new Runnable() {
					public void run() {
						numThreads++;
						try {
							// EventMgr e = EventMgr.getInstance();
							e.newEvent("new-request " + finalIp + " " + port
									+ " " + finalHost + " " + keyalgo);
						} catch (Exception e) {
						} finally {
							numThreads--;
						}
					}
				};

				if (maxThreads > numThreads) {
					log.debug("(Thread-" + numThreads
							+ ") Sending event: new-request " + ip + " " + port
							+ " " + hostname + " " + keyalgo);
					(new Thread(request)).start();
				} else {
					log.debug("Sending event: new-request " + ip + " " + port
							+ " " + hostname + " " + keyalgo);
					request.run();
				}

				// Returning data
				resp.setStatus(HttpServletResponse.SC_OK);
				out.println(_SC_REQUEST_SCHEDULED_);

				return;
			}
			log.debug("Entry found in cache, returning it to client.");
			// Else return entry;
			resp.setStatus(HttpServletResponse.SC_OK);
			out.println(_SC_OK_);
			out.println(entry.toString());

		} catch (NoSuchAlgorithmException e) {
			log.debug("Couldn't resolve host " + hostname
					+ " or algorithm is not correct: " + e);
			try {
				PrintWriter out = resp.getWriter();
				resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				out.println(_SC_INTERNAL_ERROR_);
			} catch (IOException e1) {
				log.debug("IOException: " + e);
			}

		} catch (IOException e) {
			log.debug("IOException: " + e);
		}
	}

	@Override
	public void doAction(String evnt) {
		log.debug("Received event: " + evnt);
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
							args[3], args[4])); // sets this.hostname

					String[] digests = this.getDigestsFromTargetHost();

					// If notary didn't return a list of digests, try again
					// once, than quit.
					if (digests == null
							&& !(args.length == 6 && args[5].equals("false"))) {
						log.debug("Notary didn't return a result for " + ip
								+ "/" + args[3] + ":" + port);
						if (!this.scheduledHosts.contains(this.host)
								&& Configuration.getInstance().getAttribute(
										"instance.retry") != null
								&& Configuration.getInstance()
										.getAttribute("instance.retry")
										.equals("true")
								&& this.scheduledHosts.size() < Integer
										.parseInt(Configuration
												.getInstance()
												.getAttribute(
														"instance.retry_max_size"))) {
							log.debug("Scheduling host evaluation.");
							final String eventcode;
							if (evnt.endsWith("true")) {
								eventcode = evnt
										.substring(0, evnt.length() - 6);
							} else {
								eventcode = evnt;
							}

							this.scheduledHosts.add(this.host);

							final Runnable scheduledRequest = new Runnable() {
								public void run() {
									EventMgr e = EventMgr.getInstance();
									e.newEvent(eventcode + " false");
								}
							};
							scheduler.schedule(scheduledRequest, 1,
									TimeUnit.MINUTES);
						} else {
							log.debug("Host evaluation already scheduled, scheduler is full or scheduling disabled.");
						}
						return;
					}
					if (this.scheduledHosts.contains(this.host)) {
						log.debug("Removing " + this.host + " from scheduler");
						this.scheduledHosts.remove(this.host);
					}

					StringBuffer sb = new StringBuffer();
					sb.append("add-to-cache");
					sb.append(" " + args[1]); // ip
					sb.append(" " + args[2]); // port
					sb.append(" " + args[3]); // hostname
					sb.append(" " + args[4]); // keyalgo
					sb.append(" " + digests[0]);
					for (int i = 1; i < digests.length; i++) {
						sb.append("," + digests[i]);
					}
					sb.append(" " + this.examRolesOnTargetHost());

					log.debug("Sending event: " + sb.toString());
					this.e.newEvent(sb.toString());

				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (UnknownHostException | HostConnectionException e1) {

				}
			}
		}
		if (evnt.startsWith("hwmon-notify")) {

		}
	}
}
