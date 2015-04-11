package de.donotconnect.notary_cache.operator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.donotconnect.notary_cache.operator.Interfaces.IListener;

public class EventMgr extends Thread implements IListener {

	private HashMap<String, ArrayList<IListener>> eventListeners = new HashMap<String, ArrayList<IListener>>();
	private boolean interactiveQuit = false;
	private static EventMgr instance = null;
	private final static Logger log = LogManager.getLogger("EventMgr");

	public static synchronized EventMgr getInstance() {
		if (EventMgr.instance == null) {
			EventMgr.instance = new EventMgr();
		}
		return EventMgr.instance;
	}

	public void registerEventListener(String cmd, IListener listener) {
		ArrayList<IListener> l;
		if (this.eventListeners.containsKey(cmd))
			l = this.eventListeners.get(cmd);
		else
			l = new ArrayList<IListener>();
		l.add(listener);
		this.eventListeners.put(cmd, l);
	}

	private void processEvent(String fullEventCode) {
		if (fullEventCode != null && !fullEventCode.trim().equals("")) {
			String event = fullEventCode.split(" ")[0];
			if (this.eventListeners.containsKey(event)) {
				ArrayList<IListener> listeners = this.eventListeners.get(event);
				for (IListener l : listeners) {
					l.doAction(fullEventCode);
				}
			}
		}
	}

	public void newEvent(String eventcode) {
		this.processEvent(eventcode);
	}

	public void interactive() {
		this.start();
	}

	public void run() {
		this.registerEventListener("quit", this);
		BufferedReader cmdline = new BufferedReader(new InputStreamReader(
				System.in));

		try {
			while (!this.interactiveQuit) {
				System.out.print("> ");
				String fullEventCode = cmdline.readLine();
				processEvent(fullEventCode);
			}
		} catch (IOException e) {
			log.debug("Exception: " + e);
		}
		log.debug("Received event: quit");
	}

	public void doAction(String eventcode) {
		this.interactiveQuit = true;
	}

}
