package de.donotconnect.notary_cache.client;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for NCClient
 */
public class NCClientTest {

	NCClient client = null;

	@Before
	public void createClient() throws IOException {
		this.client = new NCClient();
		this.client.addCachesFromDirectory("./");
	}

	@Test
	public void clientOwnsMoreThanZeroCaches() {
		assertTrue(this.client.hasCaches());
	}

	@Test
	public void checkValidCertificate() throws UnknownHostException,
			CertificateException {

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream in = new ByteArrayInputStream(
				Base64.decode(new String(
						"MIIHgzCCBmugAwIBAgIIPIzpUHsURsQwDQYJKoZIhvcNAQEFBQAwSTELMAkGA1UE"
								+ "BhMCVVMxEzARBgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRl"
								+ "cm5ldCBBdXRob3JpdHkgRzIwHhcNMTUwNTA2MTAwODA2WhcNMTUwODA0MDAwMDAw"
								+ "WjBmMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwN"
								+ "TW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEVMBMGA1UEAwwMKi5n"
								+ "b29nbGUuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm3lpsKLp"
								+ "Gjf5Dsz/akFsVxnNT4XijAubUMtetRDvM3tUfUVD52qDAFWN25EOb+cw/SroAQ1M"
								+ "0vDDlSmPbxvkBwDUdGJQj0SuWG9bTD4wNNNvqw1krVwGt+wl2hQlYcmyfFQFp0fO"
								+ "OXoLO4NoDLvNfKhdylHDJ2G0J8slfPYF24Dqg3pj5Dv7laOTacE5m1PH6qYpRT+9"
								+ "WLTcEB4uupkk2fUD7cjc5gt/GkoYiY3XUuCexqNyzWIT9kxXVbb7XtsAQ4247mxF"
								+ "cCqW/EytdFdaWB9I3wwD3SrUuERovBgJ06L8OcFEHMs/uSgYVODhhPi8Hycp9Rwy"
								+ "f/FdxwIiWRKKtwIDAQABo4IEUDCCBEwwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsG"
								+ "AQUFBwMCMIIDJgYDVR0RBIIDHTCCAxmCDCouZ29vZ2xlLmNvbYINKi5hbmRyb2lk"
								+ "LmNvbYIWKi5hcHBlbmdpbmUuZ29vZ2xlLmNvbYISKi5jbG91ZC5nb29nbGUuY29t"
								+ "ghYqLmdvb2dsZS1hbmFseXRpY3MuY29tggsqLmdvb2dsZS5jYYILKi5nb29nbGUu"
								+ "Y2yCDiouZ29vZ2xlLmNvLmlugg4qLmdvb2dsZS5jby5qcIIOKi5nb29nbGUuY28u"
								+ "dWuCDyouZ29vZ2xlLmNvbS5hcoIPKi5nb29nbGUuY29tLmF1gg8qLmdvb2dsZS5j"
								+ "b20uYnKCDyouZ29vZ2xlLmNvbS5jb4IPKi5nb29nbGUuY29tLm14gg8qLmdvb2ds"
								+ "ZS5jb20udHKCDyouZ29vZ2xlLmNvbS52boILKi5nb29nbGUuZGWCCyouZ29vZ2xl"
								+ "LmVzggsqLmdvb2dsZS5mcoILKi5nb29nbGUuaHWCCyouZ29vZ2xlLml0ggsqLmdv"
								+ "b2dsZS5ubIILKi5nb29nbGUucGyCCyouZ29vZ2xlLnB0ghIqLmdvb2dsZWFkYXBp"
								+ "cy5jb22CDyouZ29vZ2xlYXBpcy5jboIUKi5nb29nbGVjb21tZXJjZS5jb22CESou"
								+ "Z29vZ2xldmlkZW8uY29tggwqLmdzdGF0aWMuY26CDSouZ3N0YXRpYy5jb22CCiou"
								+ "Z3Z0MS5jb22CCiouZ3Z0Mi5jb22CFCoubWV0cmljLmdzdGF0aWMuY29tggwqLnVy"
								+ "Y2hpbi5jb22CECoudXJsLmdvb2dsZS5jb22CFioueW91dHViZS1ub2Nvb2tpZS5j"
								+ "b22CDSoueW91dHViZS5jb22CFioueW91dHViZWVkdWNhdGlvbi5jb22CCyoueXRp"
								+ "bWcuY29tggthbmRyb2lkLmNvbYIEZy5jb4IGZ29vLmdsghRnb29nbGUtYW5hbHl0"
								+ "aWNzLmNvbYIKZ29vZ2xlLmNvbYISZ29vZ2xlY29tbWVyY2UuY29tggp1cmNoaW4u"
								+ "Y29tggh5b3V0dS5iZYILeW91dHViZS5jb22CFHlvdXR1YmVlZHVjYXRpb24uY29t"
								+ "MGgGCCsGAQUFBwEBBFwwWjArBggrBgEFBQcwAoYfaHR0cDovL3BraS5nb29nbGUu"
								+ "Y29tL0dJQUcyLmNydDArBggrBgEFBQcwAYYfaHR0cDovL2NsaWVudHMxLmdvb2ds"
								+ "ZS5jb20vb2NzcDAdBgNVHQ4EFgQUzdhnY6ZHspYk0kmBmbtBDj6sBvwwDAYDVR0T"
								+ "AQH/BAIwADAfBgNVHSMEGDAWgBRK3QYWG7z2aLV29YG2u2IaulqBLzAXBgNVHSAE"
								+ "EDAOMAwGCisGAQQB1nkCBQEwMAYDVR0fBCkwJzAloCOgIYYfaHR0cDovL3BraS5n"
								+ "b29nbGUuY29tL0dJQUcyLmNybDANBgkqhkiG9w0BAQUFAAOCAQEAeEZshnTWGZeo"
								+ "ODWDj5/rihbkhBFC46ZXOWJ/2i7SL9hiUVLcF+G3/bXsCn7PG51TKIk2XtOYgYMF"
								+ "JiWVXnst1dFbdKvNOmLlGEj0lSJZTo5suTkVT2AjGcxRt4m8botitR3SIMgCeyMC"
								+ "QFxe7Ixs4ETClRhP8UF0vBOB0RnJHbVJS41CKkCyDRoC0+q5wFcQaTlhtjiZzP7O"
								+ "2qwbMslGKvUuOwIClsOg0wmTSbJGuz6z41zkwKN9IS3JU1peYUNss/aOqibKnJ50"
								+ "oLvp7KvfI2OiY8PkoHn5s1jX1JR03es99HnETQ2XRJ6Wbcp1vb7nNj7gWJNHE6Lq"
								+ "J4SaNxj2Ww==")));
		X509Certificate cert = (X509Certificate) cf.generateCertificate(in);

		NCTrustManager tm = (NCTrustManager) this.client.getX509TrustManager();
		tm.update(InetAddress.getByName("173.194.44.67").getAddress(), 443,
				"google.com", false, false);

		tm.checkServerTrusted(new X509Certificate[] { cert }, "");
		
		// Should get here..
		assertTrue(true);
	}

	@Test(expected=CertificateException.class)
	public void checkInvalidCertificate() throws UnknownHostException,
			CertificateException {

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream in = new ByteArrayInputStream(
				Base64.decode(new String(
						"MIIFZDCCA0ygAwIBAgIDC7PGMA0GCSqGSIb3DQEBBQUAMHkxEDAOBgNVBAoTB1Jvb3QgQ0ExHjAcBgNVBAsTFWh0dHA6Ly93d3cuY2FjZXJ0Lm9yZzEiMCAGA1UEAxMZQ0EgQ2VydCBTaWduaW5nIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJARYSc3VwcG9ydEBjYWNlcnQub3JnMB4XDTEyMDUwNjE4NDY0MVoXDTE0MDUwNjE4NDY0MVowWzELMAkGA1UEBhMCQVUxDDAKBgNVBAgTA05TVzEPMA0GA1UEBxMGU3lkbmV5MRQwEgYDVQQKEwtDQWNlcnQgSW5jLjEXMBUGA1UEAxMOd3d3LmNhY2VydC5vcmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDeNSAxSFtymeN6rQD69eXIJEnCCP7Z24/fdOgxRDSBhfQDUVhdmsuDOvuziOoWGqRxZPcWdMEMRcJ5SrA2aHIstvnaLhUlxp2fuaeXx9XMCJ9ZmzHZbH4wqLaU+UlhcSsdkPzapf3N3HaUAW8kT4bHEGzObYVCUBxxhpY01EoGRQmnFojzLNF3+0O1npQzXg5MeIWHW/Z+9jE+6odL6IXgg1bvrP4dFgoveTcG6BmJu+50RwHaUad7hQuNeS+pNsVzCiDdMF2qoCQXtAGhnEQ9/KHpBD2zISBVIyEbYxdyU/WxnkaOof63Mf/TAgMNzVN9duqEtFyvvMrQY1XkBBwfAgMBAAGjggERMIIBDTAMBgNVHRMBAf8EAjAAMDQGA1UdJQQtMCsGCCsGAQUFBwMCBggrBgEFBQcDAQYJYIZIAYb4QgQBBgorBgEEAYI3CgMDMAsGA1UdDwQEAwIFoDAzBggrBgEFBQcBAQQnMCUwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLmNhY2VydC5vcmcvMIGEBgNVHREEfTB7gg53d3cuY2FjZXJ0Lm9yZ4IRc2VjdXJlLmNhY2VydC5vcmeCEnd3d21haWwuY2FjZXJ0Lm9yZ4IKY2FjZXJ0Lm9yZ4IOd3d3LmNhY2VydC5uZXSCCmNhY2VydC5uZXSCDnd3dy5jYWNlcnQuY29tggpjYWNlcnQuY29tMA0GCSqGSIb3DQEBBQUAA4ICAQA2+uCGX18kZD8gyfj44TlwV4TXJ5BrT0M9qogg2k5u057i+X2ePy3DiE2REyLkU+i5ekH5gvTl74uSJKtpSf/hMyJEByyPyIULhlXCl46z2Z60drYzO4igapCdkm0JthVGvk6/hjdaxgBGhUvSTEP5nLNkDa+uYVHJI58wfX2oh9gqxf8VnMJ8/A8Zi6mYCWUlFUobNd/ozyDZ6WVntrLib85sAFhds93nkoUYxgx1N9Xg/I31/jcL6bqmpRAZcbPtvEom0RyqPLM+AOgySWiYbg1Nl8nKx25C2AuXk63NN4CVwkXpdFF3q5qk1izPruvJ68jNW0pG7nrMQsiY2BCesfGyEzY8vfrMjeR5MLNv5r+obeYFnC1juYp6JBt+thW+xPFzHYLjohKPwo/NbMOjIUM9gv/Pq3rVRPgWru4/8yYWhrmEK370rtlYBUSGRUdR8xed1Jvs+4qJ3s9t41mLSXvUfwyPsT7eoloUAfw3RhdwOzXoC2P6ftmniyu/b/HuYH1AWK+HFtFi9CHiMIqOJMhj/LnzL9udrQOpir7bVej/mlb3kSRo2lZymKOvuMymMpJkvBvUU/QEbCxWZAkTyqL2qlcQhHv7W366DOFjxDqpthaTRD69T8i/2AnsBDjYFxa47DisIvR57rLmE+fILjSvd94N/IpGs3lSOS5JeA==")));
		X509Certificate cert = (X509Certificate) cf.generateCertificate(in);

		NCTrustManager tm = (NCTrustManager) this.client.getX509TrustManager();
		tm.update(InetAddress.getByName("173.194.44.67").getAddress(), 443,
				"google.com", false, false);

		tm.checkServerTrusted(new X509Certificate[] { cert }, "");
	}
}
