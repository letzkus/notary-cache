package de.donotconnect.notary_cache.client.Evaluation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class MonteCarloSimulation {

	public static void main(String[] args) {

		// Input
		int hosts = 100000; // Number of hosts on the internet
		long[] numberOfUsers = new long[] { (long) (hosts * 0.001),
				(long) (hosts * 0.005), (long) (hosts * 0.01),
				(long) (hosts * 0.05), (long) (hosts * 0.1),
				(long) (hosts * 0.5), hosts * 1, hosts * 5, hosts * 10,
				hosts * 50, hosts * 100 };

		// Advanced Input
		int reqPerHost = 1000; // Hosts per host
		long requests = hosts * reqPerHost; // 1000 requests per host on average
		double tlsHosts = 1; // Fraction of hosts which are capable of TLS
		double zipfExponent = 0.7; // Exponent for the Zipf Distribution
		double averageEntrySize = 204; // Average size of an entry
		double averageTrafficPerRequest = 3700; // Average traffic generated per
												// request
		int outputMode = 5; // 0->debug, 1->entries, 2->benefits,
							// 3->1+2, 4->remainingRequests*averageSize

		// Time Measurements for each step
		long[] durations = new long[3];

		// Step 1: Generate Data
		durations[0] = System.nanoTime();
		HashMap<Long, Long> req = _generateAndCorrelateRequests(requests,
				(int) (hosts * tlsHosts), zipfExponent);
		durations[0] = (long) ((durations[0] - System.nanoTime()) / 1e6);

		// Debug
		System.out.println("[DEBUG] Start - Sum of Requests: "
				+ req.values().stream().reduce(0L, (a, b) -> a + b) + " to "
				+ req.size() + " hosts");

		// Calculate benefits for given number of entries
		int[] hostsInCache = { 100, 250, 500, 750, 1000, 2500, 5000, 7500,
				10000 }; // number of hosts in cache
		for (int i = 0; i < hostsInCache.length; i++) {
			System.out.println(hostsInCache[i] + "\t"
					+ _calculateBenefits(req, hostsInCache[i], requests));
		}
		System.out.println();

		// Calculate average number of entries related to number of users
		for (long users : numberOfUsers) {

			// System.out.println("users=" + users + " users/hosts="
			// + ((double) users / (double) hosts));

			// Filter entries

			HashMap<Long, Long> foundHosts = new HashMap<Long, Long>();
			HashMap<Long, Long> remainingHosts = new HashMap<Long, Long>();

			_filterUniform(req, users, foundHosts, remainingHosts);

			long remainingRequests = remainingHosts.values().stream()
					.reduce(0L, (a, b) -> a + b);

			long savedRequests = foundHosts.values().stream()
					.reduce(0L, (a, b) -> a + b);

			if (outputMode == 0) // Debug
				System.out
						.println("#entries="
								+ foundHosts.size()
								+ "; savedRequests="
								+ savedRequests
								+ "; benefit="
								+ ((double) savedRequests / ((double) (remainingRequests + savedRequests)))
								* 100
								+ "%; remainingRequests="
								+ remainingRequests
								+ "; remainingHosts="
								+ remainingHosts.size()
								+ "; loss="
								+ ((double) remainingRequests / ((double) (remainingRequests + savedRequests)))
								* 100 + "%; cacheSize=" + foundHosts.size()
								* averageEntrySize);
			if (outputMode == 1) // users entries
				System.out.println(users + "\t" + foundHosts.size());
			if (outputMode == 2) // users benefit
				System.out
						.println(users
								+ "\t"
								+ ((double) savedRequests / ((double) (remainingRequests + savedRequests))));
			if (outputMode == 3)
				System.out
						.println(users
								+ "\t"
								+ foundHosts.size()
								+ "\t"
								+ ((double) savedRequests / ((double) (remainingRequests + savedRequests))));
			if (outputMode == 4)
				System.out.println(users
						+ "\t"
						// + "\t" + (remainingRequests *
						// averageTrafficPerRequest) / 1024 / 1024)
						// + "\t" + (savedRequests * averageTrafficPerRequest) /
						// 1024 / 1024)
						+ "\t" + (foundHosts.size() * averageEntrySize * users)
						/ 1024 / 1024);
			if (outputMode == 5)
				System.out
						.println(users
								+ "\t"
								+ "\t"
								+ (((remainingRequests * averageTrafficPerRequest) / 1024 / 1024) + (foundHosts
										.size() * averageEntrySize * users) / 1024 / 1024));
		}

	}

	/**
	 * Filters all hosts smaller than factor*users. Returns the number or
	 * requests of the hosts that were filtered.
	 * 
	 * @param res
	 * @param factor
	 * @param users
	 * @param foundHosts
	 * @return
	 */
	private static void _filterUniform(HashMap<Long, Long> res, long users,
			HashMap<Long, Long> foundHosts, HashMap<Long, Long> dismissedHosts) {

		Iterator<Entry<Long, Long>> it = res.entrySet().iterator();

		while (it.hasNext()) {
			Entry<Long, Long> e = it.next();
			if (e.getValue() < users) {
				dismissedHosts.put(e.getKey(), e.getValue());
			} else {
				foundHosts.put(e.getKey(), e.getValue());
			}
		}
	}

	@SuppressWarnings("unused")
	private static void _filterZipf(HashMap<Long, Long> res, long users,
			double zipf, HashMap<Long, Long> foundHosts,
			HashMap<Long, Long> dismissedHosts) {

		Iterator<Entry<Long, Long>> it = res.entrySet().iterator();

		while (it.hasNext()) {
			Entry<Long, Long> e = it.next();
			if (e.getValue() < Math.pow(users, zipf)) {
				dismissedHosts.put(e.getKey(), e.getValue());
			} else {
				foundHosts.put(e.getKey(), e.getValue());
			}
		}
	}

	private static double _calculateBenefits(HashMap<Long, Long> res,
			int hostsInCache, long requests) {

		// System.out.println("Req="+requests+" res.size()="+res.size()+" res="+res.toString());

		ValueComparator bvc = new ValueComparator(res);
		TreeMap<Long, Long> sortedMap = new TreeMap<Long, Long>(bvc);
		sortedMap.putAll(res);

		long sum = 0;
		Entry<Long, Long> e = null;
		for (int i = 0; i < hostsInCache; i++) {
			e = sortedMap.pollFirstEntry();
			if (e == null)
				break;
			sum += e.getValue();
		}

		double result = ((double) sum) / ((double) requests);
		return result;
	}

	private static HashMap<Long, Long> _generateAndCorrelateRequests(long n,
			int h, double zipfExp) {

		HashMap<Long, Long> res = new HashMap<Long, Long>();

		System.out.print("Generating: ");
		FastZipfGenerator z1 = new FastZipfGenerator(h, zipfExp);
		for (long i = 0; i < n; i++) {

			// Progress bar
			if ((((double) i / n) * 100) % 10 == 0)
				System.out.print(".");

			long z = z1.next();
			res.compute(z, (k, v) -> (v == null) ? 1 : v + 1);

		}
		System.out.println("done");

		return res;

	}
}
