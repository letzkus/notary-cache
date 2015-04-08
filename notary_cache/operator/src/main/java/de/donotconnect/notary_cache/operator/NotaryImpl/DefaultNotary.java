package de.donotconnect.notary_cache.operator.NotaryImpl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import de.donotconnect.notary_cache.operator.Configuration;
import de.donotconnect.notary_cache.operator.Interfaces.AbstractNotary;

public class DefaultNotary extends AbstractNotary {

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
						"TLS_DHE_DSS_WITH_AES_128_GCM_SHA256"
				});
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
			HttpServletRequest req, HttpServletResponse resp) {
		// TODO Auto-generated method stub

	}

}
