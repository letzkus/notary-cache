package de.donotconnect.notary_cache.operator;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import de.donotconnect.notary_cache.operator.Interfaces.IListener;

public class HwMonitor extends Thread implements IListener {

	private boolean quit = false;
	private EventMgr eventmgr = null;
	private Sigar sigar = null;
	private int pollingTime = 1000;
	private final static Logger log = LogManager.getLogger("HwMonitor");

	public final int _POLLING_MIN_ = 1000;

	public HwMonitor(int polling) {
		this.sigar = new Sigar();
		this.pollingTime = polling;
		this.eventmgr = EventMgr.getInstance();
		this.eventmgr.registerEventListener("quit", this);
		this.eventmgr.registerEventListener("hwmon-change-polling", this);
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
				ifstat = sigar.getNetInterfaceStat("en0");
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
				
				if(rxtx_then != 0)
					evnt.append(" bw=" + df.format(Math.abs(rxtx_now - rxtx_then)));
				
				evnt.append(" time="+String.valueOf(time));

				log.debug("Sending event: " + evnt.toString());

				this.eventmgr.newEvent(evnt.toString());

				rxtx_then = rxtx_now;

				Thread.sleep(this.pollingTime);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (SigarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	}

}
