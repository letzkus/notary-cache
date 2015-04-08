package de.donotconnect.notary_cache.operator.NotaryImpl;

import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jetty.server.Request;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.donotconnect.notary_cache.operator.Configuration;
import de.donotconnect.notary_cache.operator.Interfaces.AbstractNotary;

public class PerspectivesNotary extends AbstractNotary {

	@XmlRootElement(name = "notary_reply")
	public static class NotaryReply {
		@XmlElement(name = "key")
		public Observation[] observations;
		@XmlAttribute(name = "version")
		public String version;
		@XmlAttribute(name = "sig")
		public String signature;
		@XmlAttribute(name = "sig_type")
		public String signatureType;
	}

	public static class Observation {
		@XmlElement(name = "timestamp")
		public Timestamp[] timestamps;
		@XmlAttribute(name = "fp")
		public String fingerprint;
		@XmlAttribute(name = "type")
		public String type;
	}

	public static class Timestamp {
		@XmlAttribute(name = "start")
		public long start;
		@XmlAttribute(name = "end")
		public long end;
	}

	private Configuration c;

	public PerspectivesNotary() {
		this.c = Configuration.getInstance();
	}

	public String getHashalgo() {
		return "MD5";
	}

	@Override
	public String[] getDigestsFromTargetHost() {
		// TODO Auto-generated method stub

		Client client = Client.create();
		client.setConnectTimeout(Integer.parseInt(c
				.getAttribute("notary.timeout")));
		client.setReadTimeout(Integer.parseInt(c.getAttribute("notary.timeout")));

		try {
			NotaryReply reply = client.asyncResource(
					c.getAttribute("notary.prefix") + c.getAttribute("notary.hostname") + ":"
							+ c.getAttribute("instance.port"))
					.path("/.well-known/notary")
					.queryParam("host", this.host.getHostAddress())
					.queryParam("port", String.valueOf(this.port))
					.queryParam("service_type", "2").get(NotaryReply.class).get();
			client.getExecutorService().shutdown();
			client.destroy();
			
			long now = System.currentTimeMillis() / 1000;
			Observation lastObservation = null;
			long lastObservationTime = 0L;
			
			for(Observation o : reply.observations) { // Keys
				for(Timestamp t : o.timestamps) { // Timestamps
					if(lastObservation==null || (now - t.end) < (now - lastObservationTime)) {
							lastObservation = o;
							lastObservationTime = t.end;
					}
				}
			}
			
			if(lastObservation==null)
				return null;
			
			return new String[]{lastObservation.fingerprint};
			
		} catch (UniformInterfaceException | InterruptedException
				| ExecutionException e) {
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
