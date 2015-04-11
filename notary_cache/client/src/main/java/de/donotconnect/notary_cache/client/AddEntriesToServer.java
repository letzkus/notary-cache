package de.donotconnect.notary_cache.client;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;

public class AddEntriesToServer {

	public static void main(String[] args) throws Exception {

		ZipFile alexaZIP = null;
		ZipEntry alexaCSV = null;

		try {
			alexaZIP = new ZipFile("top-1m.csv.zip");
			alexaCSV = alexaZIP.getEntry("top-1m.csv");
			// System.out.println("Found Alexa Top1m file...");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			URLConnection conn;
			try {
				System.out.println("Downloading Alexa Top1m...");
				conn = new URL(
						"http://s3.amazonaws.com/alexa-static/top-1m.csv.zip")
						.openConnection();
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setRequestProperty("content-type", "binary/data");
				InputStream in = conn.getInputStream();
				FileOutputStream out = new FileOutputStream("top-1m.csv.zip");

				byte[] b = new byte[1024];
				int count;

				while ((count = in.read(b)) > 0) {
					out.write(b, 0, count);
				}
				out.close();
				in.close();
				System.out.println("Done.");
				System.out.println("Opening Zip-File...");
				alexaZIP = new ZipFile("top-1m.csv.zip");
				alexaCSV = alexaZIP.getEntry("top-1m.csv");
				System.out.println("Done.");
			} catch (MalformedURLException e1) {
				alexaZIP = null;
				System.out.println(e1.getMessage());
			} catch (IOException e1) {
				alexaZIP = null;
				System.out.println(e1.getMessage());
			}

		}

		if (alexaZIP == null || alexaCSV == null) {
			System.out
					.println("Alexa Top1m (top-1m.csv.zip or download) not available.");
			System.exit(1);
		}

		InputStream in = alexaZIP.getInputStream(alexaCSV);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		int internalLimit = 10000;

		String line, host;
		int i = 1;
		long timeStart = System.currentTimeMillis() / 1000;
		while ((line = br.readLine()) != null && internalLimit > 0) {
			host = line.split(",")[1].split("/")[0]; // 123,host.tld/some/more

			HttpClient httpClient = new HttpClient();
			httpClient.start();

			try {
				String ip = InetAddress.getByName(host).getHostAddress();

				System.out.print(i + " - Adding host " + host + ".....");
				ContentResponse resp = httpClient
						.GET("http://z.donotconnect.de/.well-known/notary?hostname="
								+ host + "&ip=" + ip);
				System.out.println(".....done: " + resp.getContentAsString());
			} catch (UnknownHostException e) {

			} finally {
				httpClient.stop();
				httpClient.destroy();
			}

			internalLimit--;
			i++;
		}
		long timeEnd = System.currentTimeMillis() / 1000;
		System.out.println("Adding " + (i - 1) + " hosts lasts "
				+ (timeEnd - timeStart) + " seconds or "
				+ ((float) (timeEnd - timeStart)) / 60 + " minutes.");

	}

}
