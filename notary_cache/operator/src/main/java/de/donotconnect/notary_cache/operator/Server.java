package de.donotconnect.notary_cache.operator;

import java.net.InetSocketAddress;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

public class Server {

	org.eclipse.jetty.server.Server jetty;

	public Server(OperatorMain op) {

		try {

			Configuration conf = Configuration.getInstance();

			/**
			 * Configure jetty server
			 */
			ContextHandler cacheHandler = new ContextHandler(
					"/.well-known/notary-cache");
			cacheHandler.setHandler(op);
			ContextHandler cacheConfigHandler = new ContextHandler(
					"/.well-known/notary-cache-config");
			cacheConfigHandler.setHandler(op);
			ContextHandler notaryHandler = new ContextHandler(
					"/.well-known/notary");
			notaryHandler.setHandler(op);
			ContextHandlerCollection contextHandlerList = new ContextHandlerCollection();
			contextHandlerList.setHandlers(new Handler[] { cacheHandler,
					cacheConfigHandler, notaryHandler });

			/**
			 * Initialize jetty
			 */
			this.jetty = new org.eclipse.jetty.server.Server(
					new InetSocketAddress(
							conf.getAttribute("instance.ip"),
							Integer.parseInt(conf.getAttribute("instance.port"))));
			this.jetty.setHandler(contextHandlerList);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			if (this.jetty.isRunning())
				this.jetty.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void start() {
		try {
			if (jetty != null)
				this.jetty.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
