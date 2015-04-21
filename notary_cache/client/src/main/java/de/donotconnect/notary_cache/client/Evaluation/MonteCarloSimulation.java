package de.donotconnect.notary_cache.client.Evaluation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class MonteCarloSimulation {

	public static void main(String[] args) {

		// Step 0: Define input
		int hosts = 10000; // Number of hosts on the internet
		long users = hosts * 8; // Number of users
		long requests = users * 1000; // Each user makes 1000 requests
		double tlsHosts = 0.4; // Fraction of hosts which are capable of TLS
		double zipfExponent = 0.7; // Exponent for the Zipf Distribution
		double averageEntrySize = 204; // Average size of an entry in the cache
		double historyFactor = 0.25; // hosts belonging to at least x percent of
										// all users
		int[] hostsInCache = { 100, 250, 500, 750, 1000, 2500, 5000, 7500,
				10000 }; // number of hosts in cache

		// Time Measurements
		long before = 0;
		long after = 0;

		// Step 1: Generation and Step 2: Computation
		before = System.nanoTime();
		HashMap<Long, Long> req = _generateAndCorrelateRequests(
				requests, (int) (hosts * tlsHosts), zipfExponent);
		after = System.nanoTime();
		System.out.println("Generation duration " + (after - before) / 1e6);
		System.out.println("Sum of Requests: "
				+ req.values().stream().reduce(0L, (a, b) -> a + b) + " to "
				+ req.size() + " hosts");
		// Step 3: Aggregating
		before = System.nanoTime();

		long statHosts = req.size();

		// Calculate benefits with fixed number of entries in cache
		double[] benefits = new double[hostsInCache.length];
		for (int i = 0; i < hostsInCache.length; i++) {
			benefits[i] = _calculateBenefits(req, hostsInCache[i], requests);
		}

		// Calculate number of entries with given fraction of hosts in user
		// caches
		long remainingRequests = _filter(req, historyFactor, users);
		System.out.println("After: Sum of Requests: "
				+ req.values().stream().reduce(0L, (a, b) -> a + b) + " to "
				+ req.size() + " hosts");

		after = System.nanoTime();
		System.out.println("Filtering duration " + (after - before) / 1e6);

		// Result a:
		System.out.println();
		System.out.println("Input: users=" + users + " hosts=" + hosts
				+ " requests=" + requests + " tlsHosts=" + tlsHosts
				+ " zipfExp=" + zipfExponent);
		System.out.println("Distribution: hosts=" + statHosts + " ("
				+ (statHosts / (hosts * tlsHosts)) * 100 + "%) ignored="
				+ ((hosts * tlsHosts) - statHosts));
		System.out.println("Number of entries for history factor "
				+ historyFactor + ": " + req.size());
		System.out.println("Size of cache: " + (req.size() * averageEntrySize)
				/ 1000 + "kb");
		System.out.println("Not in cache: " + (statHosts - req.size()));
		System.out.println("Remaining requests: " + remainingRequests + " or "
				+ ((double) remainingRequests / (double) requests) * 100 + "%");
		System.out.println("Caching benefits: "
				+ (requests - remainingRequests) + " or "
				+ ((double) (requests - remainingRequests) / (double) requests)
				* 100 + "%");
		System.out.println();
		System.out.println("Caching benefits for fixed size caches: ");
		for (int i = 0; i < hostsInCache.length; i++)
			System.out.println(" " + hostsInCache[i] + " entries: "
					+ benefits[i]);
	}

	/**
	 * Filters all hosts smaller than factor*users. Returns the number or
	 * requests of the hosts that were filtered.
	 * 
	 * @param res
	 * @param factor
	 * @param users
	 * @return
	 */
	private static long _filter(HashMap<Long, Long> res, double factor,
			long users) {

		Iterator<Entry<Long, Long>> it = res.entrySet().iterator();

		double limit = factor * users;
		int i = 0;
		long requests = 0;
		int max = res.size();
		System.out.print("Filtering: ");
		while (it.hasNext()) {
			if ((((double) i / (double) max) * 100) % 10 == 0)
				System.out.print(".");
			Entry<Long, Long> e = it.next();
			if (e.getValue() < limit) {
				requests += e.getValue();
				it.remove();
			}
			i++;
		}
		System.out.println("done");

		return requests;
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
