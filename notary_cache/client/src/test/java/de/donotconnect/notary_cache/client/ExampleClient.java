package de.donotconnect.notary_cache.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

public class ExampleClient {
	public static void main(String[] args) throws NoSuchAlgorithmException,
			KeyManagementException, UnknownHostException, IOException {

		// Set host information
		String hostname = "google.com";
		String ip = "173.194.44.65";
		int port = 443;

		// ----------------------------------------------------------------------
		// Notary Cache
		// ----------------------------------------------------------------------

		// Initialize NotaryCache
		NCClient ncclient = NCClient.getInstance();
		// ---- Configure NotaryCache
		ArrayList<String> caches = new ArrayList<String>();
		caches.add("localhost");
		ncclient.updateClient(caches);
		// ---- Create X509TrustManager
		NCTrustManager cacheTrustManager = (NCTrustManager)ncclient.getX509TrustManager();

		// ----------------------------------------------------------------------
		// Browser Simulation
		// ----------------------------------------------------------------------
		
		// Set connection information for TrustManager
		cacheTrustManager.update(InetAddress.getByName(ip)
				.getAddress(), port, hostname);

		// Create SSL Connection
		final SSLContext sslContext = SSLContext.getInstance("TLS");
		// ---- Set X509TrustManager
		sslContext.init(null, new TrustManager[] { cacheTrustManager },
				SecureRandom.getInstanceStrong());
		// ---- Create SSLSocket
		SSLSocket socket = (SSLSocket) sslContext.getSocketFactory()
				.createSocket(ip, port);
		SSLParameters p = new SSLParameters();
		// ---- Set SNI
		ArrayList<SNIServerName> hostnames = new ArrayList<SNIServerName>();
		hostnames.add(new SNIHostName(hostname));
		p.setServerNames(hostnames);
		socket.setSSLParameters(p);
		socket.startHandshake();
		PrintWriter pw = new PrintWriter(socket.getOutputStream());
		// ---- Send HTTP Request
		pw.println("GET / HTTP/1.1");
		pw.println("Host: " + hostname);
		pw.println("");
		pw.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		// ---- Receive data
		String recv;
		while ((recv = br.readLine()) != null) {
			System.out.println(recv);
		}
		br.close();
	}
}
