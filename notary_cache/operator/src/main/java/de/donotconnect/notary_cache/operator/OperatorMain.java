package de.donotconnect.notary_cache.operator;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import de.donotconnect.notary_cache.operator.CacheImpl.InMemoryCache;
import de.donotconnect.notary_cache.operator.CacheStrategyImpl.SimpleCachingStrategy;
import de.donotconnect.notary_cache.operator.Interfaces.ICache;
import de.donotconnect.notary_cache.operator.Interfaces.ICacheStrategy;
import de.donotconnect.notary_cache.operator.Interfaces.IListener;
import de.donotconnect.notary_cache.operator.NotaryImpl.DefaultNotary;

public class OperatorMain extends AbstractHandler implements IListener {

	public final int _NOTARY_CACHE_VERSION_ = 1;

	public static void main(String[] args) {
		OperatorMain op = new OperatorMain();
		op.startOperator();
	}

	private final static Logger log = LogManager.getLogger("OperatorMain");
	private HwMonitor hwmon;
	private int hwmonPolling = 10000;
	private EventMgr evntMgr;
	private Server server;
	private ICacheStrategy cachingStrategy;
	private ICache cache;
	private DefaultNotary notary;

	public OperatorMain() {
		/**
		 * Initialize Modules
		 */
		this.evntMgr = EventMgr.getInstance();
		this.hwmon = new HwMonitor(this.hwmonPolling);
		this.server = new Server(this);
		this.notary = new DefaultNotary();

		/**
		 * Register potential events
		 */
		this.evntMgr.registerEventListener("quit", this);
		this.evntMgr.registerEventListener("hwmon-notify", this);

		/**
		 * Initialize Cache Elements
		 */
		this.cache = new InMemoryCache();
		this.cachingStrategy = new SimpleCachingStrategy();
	}

	public void startOperator() {
		this.hwmon.start();
		this.server.start();
		this.evntMgr.interactive();
		this.cachingStrategy.manage(this.cache, this.notary);
	}

	public void stopOperator() {
		server.stop();
	}

	/**
	 * If someone puts "quit" in the console...
	 */
	public void doAction(String evnt) {

		log.debug("Received Event: " + evnt);

		if (evnt.startsWith("quit")) {
			this.stopOperator();
		}
		if (evnt.startsWith("hwmon-notify")) {
			manageHwmonPolling(evnt);
		}
	}

	private void manageHwmonPolling(String evnt) {
		String[] attrs = evnt.split(" ");
		for (String attr : attrs) {
			if (attr.startsWith("load=")) {
				String[] load = attr.split("=")[1].split(",");
				if (Double.valueOf(load[0]) < Double.valueOf(load[1])) {
					if (Double.valueOf(load[1]) < Double.valueOf(load[2])) {
						// load is going down for 15min -> poll more often
						if (this.hwmonPolling - 2000 > this.hwmon._POLLING_MIN_)
							this.hwmonPolling -= 2000;
						else
							this.hwmonPolling = this.hwmon._POLLING_MIN_;
					} else {
						// load is going down for 5min -> poll more often
						if (this.hwmonPolling - 1000 > this.hwmon._POLLING_MIN_)
							this.hwmonPolling -= 1000;
						else
							this.hwmonPolling = this.hwmon._POLLING_MIN_;
					}
				} else {
					if (Double.valueOf(load[1]) < Double.valueOf(load[2])) {
						// load is going up for 15min - pull less often
						this.hwmonPolling += 2000;
					} else {
						// load is going up for 5min - pull less often
						this.hwmonPolling += 1000;
					}
				}

				// TODO remove comments to adapt hwmon polling

				// log.debug("Sending event: " + "hwmon-change-polling "
				// + this.hwmonPolling);

				// this.evntMgr.newEvent("hwmon-change-polling "
				// + this.hwmonPolling);

			}
		}
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {

		// TODO implement interface

		if (target.equals("/.well-known/notary-cache")) {
			
			resp.setContentType("text/html; charset=utf-8");
			resp.setStatus(HttpServletResponse.SC_OK);
			
			PrintWriter out = resp.getWriter();
			out.println(this.cachingStrategy.getCacheAsString());
			
			baseRequest.setHandled(true);

		} else if (target.equals("/.well-known/notary-cache-config")) {

		} else if (target.equals("/.well-known/notary")) {

			this.notary.handleRequest(target, baseRequest, req, resp);

		}

		resp.setContentType("text/html; charset=utf-8");
		resp.setStatus(HttpServletResponse.SC_OK);

		PrintWriter out = resp.getWriter();

		out.println(target + " --- " + baseRequest.toString());

		baseRequest.setHandled(true);

	}
}
