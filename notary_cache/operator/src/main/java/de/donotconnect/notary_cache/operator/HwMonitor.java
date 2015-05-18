package de.donotconnect.notary_cache.operator;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import de.donotconnect.notary_cache.operator.Interfaces.IListener;

/**
 * 
 * HwMonitor monitors various hardware parameters and publishes them via
 * EventMgr to other modules.
 * 
 * @author fabianletzkus
 *
 */
public class HwMonitor extends Thread implements IListener {

	private boolean quit = false;
	private EventMgr eventmgr = null;
	private Sigar sigar = null;
	private int pollingTime = 900000;
	public final int _POLLING_MIN_ = 60000;
	private final static Logger log = LogManager.getLogger("HwMonitor");

	private DB db;
	private BTreeMap<Double, Float> stats;
	private long lastMeasurement = 0;
	private float highestMeasurement = 0;
	private double measurementStartPeriod = 0;
	private double measurementEndPeriod = 0;

	public HwMonitor(int polling) {
		this.sigar = new Sigar();
		this.pollingTime = polling;
		this.eventmgr = EventMgr.getInstance();
		this.eventmgr.registerEventListener("quit", this);
		this.eventmgr.registerEventListener("hwmon-change-polling", this);
		this.eventmgr.registerEventListener("hwmon-notify", this);
	}

	public void run() {
		try {

			double rxtx_then = 0;
			DecimalFormat df = new DecimalFormat("0.0##",
					new DecimalFormatSymbols(new Locale("EN_us")));
			CpuPerc cpu = sigar.getCpuPerc();
			Mem mem = sigar.getMem();
			NetInterfaceStat ifstat = sigar.getNetInterfaceStat(Configuration
					.getInstance().getAttribute("instance.interface"));

			while (!this.quit) {

				double memFree = (mem.getActualFree() / 1024 / 1024);
				cpu = sigar.getCpuPerc();
				mem = sigar.getMem();
				ifstat = sigar.getNetInterfaceStat(Configuration.getInstance()
						.getAttribute("instance.interface"));

				double rxtx_now = (ifstat.getRxBytes() + ifstat.getTxBytes()) / 1024;
				double[] load = sigar.getLoadAverage(); // %-1, %-5, %-15
				long time = System.currentTimeMillis() / 1000;

				StringBuilder evnt = new StringBuilder();
				evnt.append("hwmon-notify");

				// Add various parameters
				evnt.append(" load=" + df.format(load[0]) + ","
						+ df.format(load[1]) + "," + df.format(load[2]));
				evnt.append(" cpu_idle=" + df.format(cpu.getIdle()));
				evnt.append(" mem=" + df.format(memFree));

				if (rxtx_then != 0)
					evnt.append(" bw="
							+ df.format(Math.abs(rxtx_now - rxtx_then)));

				evnt.append(" time=" + String.valueOf(time));

				log.debug("Sending event: " + evnt.toString());

				this.eventmgr.newEvent(evnt.toString());

				rxtx_then = rxtx_now;

				Thread.sleep(this.pollingTime);

				if (this.measurementStartPeriod != 0
						&& this.measurementEndPeriod != 0) {
					evnt = new StringBuilder();
					evnt.append("hwmon-measure-load");
					evnt.append(" start=" + this.measurementStartPeriod);
					evnt.append(" end=" + this.measurementStartPeriod);
					log.debug("Sending event: " + evnt.toString());
					this.eventmgr.newEvent(evnt.toString());
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (SigarException e) {
			log.error("Sigar could not be initialized. "
					+ "System is probably not supported or libs were not found in classpath. "
					+ "Please remove hwmon module to run NotaryCache.");
		}

	}

	public void doAction(String eventcode) {
		log.debug("Received event: " + eventcode);
		if (eventcode.trim().startsWith("hwmon-change-polling")) {
			String[] args = eventcode.trim().split(" ");
			if (args.length == 2) {
				if (Integer.parseInt(args[1]) > this._POLLING_MIN_)
					this.pollingTime = Integer.parseInt(args[1]);
			}
		}
		if (eventcode.trim().startsWith("quit")) {
			this.quit = true;
		}
		if (eventcode.startsWith("hwmon-notify")) {

			if (this.db == null || this.stats == null) {
				this.db = DBMaker.newMemoryDB().make();
				this.stats = this.db.createTreeMap("stats").make();
			}

			Calendar c = Calendar.getInstance();

			// 1. Für jede viertelstunde (4*24) einen Load-Wert speichern
			String[] attrs = eventcode.split(" ");
			for (String attr : attrs) {
				if (attr.startsWith("load=")) {
					if ((c.getTimeInMillis() - lastMeasurement) >= 900000) {
						float load = Float.parseFloat(attr.split("=")[1]
								.split(",")[2]);
						double quarterHour = Math
								.ceil(c.get(Calendar.MINUTE) / 15);
						if (this.stats.containsKey(quarterHour
								* c.get(Calendar.HOUR)))
							this.stats.remove(quarterHour
									* c.get(Calendar.HOUR));
						this.stats
								.put(quarterHour * c.get(Calendar.HOUR), load);
						lastMeasurement = c.getTimeInMillis();
						// 2. höchsten Wert bestimmen: highest
						if (this.stats.size() >= 24 * 4) {
							for (float l : this.stats.values()) {
								if (l > this.highestMeasurement) {
									this.highestMeasurement = l;
								}
							}
							// 3. Alle keys auslesen, deren value <0,25*highest
							ArrayList<Double> keyList = new ArrayList<Double>();
							for (double key : this.stats.keySet()) {
								if (this.stats.get(key) <= 0.25 * this.highestMeasurement)
									keyList.add(key);
							}
							// 4. größten zusammenhängenden part bestimmen (keys
							// sortieren, mit for-schleife ablaufen)qas
							Collections.sort(keyList);
							double startKey = 0;
							double endKey = 0;
							double maxPeriod = 0;
							double maxPeriodStartKey = 0;
							for (double key : keyList) {
								if (key == startKey + 1) {
									endKey = key;
								}
								{
									if (endKey - startKey >= maxPeriod) {
										maxPeriod = endKey - startKey;
										maxPeriodStartKey = startKey;
									}
								}
							}
							this.measurementStartPeriod = maxPeriodStartKey;
							this.measurementEndPeriod = maxPeriodStartKey
									+ maxPeriod;
						}
					}
				}
			}
		}
	}
}
