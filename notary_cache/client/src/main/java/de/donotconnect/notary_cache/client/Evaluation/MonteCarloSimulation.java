package de.donotconnect.notary_cache.client.Evaluation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class MonteCarloSimulation {

	public static void main(String[] args) {

		// Step 0: Define input
		int n = 500000; // Number of histories
		int r = 1000; // Number of requests
		int h = n/8; // Number of hosts on the internet
		double tlsHosts = 0.4; // Fraction of hosts which are capable of TLS
		double zipfExponent = 0.7; // Exponent for the Zipf Distribution
		double averageEntrySize = 204; // Average size of an entry in the cache

		// Step 1: Generation
		int[][] histories = _generateHistories(n, r, (int) (h * tlsHosts),
				zipfExponent);

		// Step 2: Computation of caches
		int[] cache = _getCommonHosts(histories, 0.75);

		// Result a:
		System.out.println("Number of entries: " + cache.length);
		System.out.println("Size of cache: "
				+ (cache.length * averageEntrySize) / 1000 + "kb");
		System.out.println("Not in cache: "
				+ (((int) (h * tlsHosts)) - cache.length));
	}

	private static int[][] _generateHistories(int n, int r, int h,
			double zipfExponent) {

		System.out.print("Generating histories...");

		int[][] result = new int[n][r];

		FastZipfGenerator z1 = new FastZipfGenerator(h, zipfExponent);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < r; j++) {
				result[i][j] = z1.next();
			}
		}

		System.out.println("... done.");

		return result;

	}

	private static int[] _getCommonHosts(int[][] histories, double factor) {

		// 1. Count appearances
		System.out.print("Calculating appearances...");
		HashMap<Integer, Integer> res = _countAppearances(histories);
		System.out.println("... done.");

		// 2. Sort out entries according to factor
		System.out.print("Sorting out entries according to factor...");
		_filterFactor(res, factor, histories.length);
		System.out.println("... done.");

		// 3. Return result;
		int[] result = new int[res.size()];
		int i = 0;
		for (Integer in : res.keySet()) {
			result[i] = in;
			i++;
		}

		return result;
	}

	private static void _filterFactor(HashMap<Integer, Integer> res,
			double factor, int numberOfHistories) {

		res.replaceAll((k, v) -> (v >= factor * numberOfHistories) ? 1 : 0);

		Iterator<Entry<Integer, Integer>> it = res.entrySet().iterator();

		while (it.hasNext()) {

			Entry<Integer, Integer> e = it.next();
			if (e.getValue() == 0)
				it.remove();

		}
	}

	private static HashMap<Integer, Integer> _countAppearances(int[][] histories) {
		HashMap<Integer, Integer> res = new HashMap<Integer, Integer>(); // Size
																			// of
																			// unknown...

		// Count appearances
		for (int i = 0; i < histories.length; i++) {

			for (int j = 0; j < histories[i].length; j++) {

				res.compute(histories[i][j], (k, v) -> (v == null) ? 1 : v + 1);

			}

		}
		return res;
	}

	/*-----------------------------------------------------------------------------------------
	 * 
	 * Other Zipf-Implementations
	 * 
	 *-----------------------------------------------------------------------------------------
	 */
	@SuppressWarnings("unused")
	private static int[][] _generateHistoriesApache(int n, int r, int h,
			double zipfExponent) {

		System.out.print("Generating histories...");

		int[][] result = new int[n][r];

		RandomGenerator rand = new JDKRandomGenerator();
		rand.setSeed(0);
		ZipfDistribution zipf = new ZipfDistribution(rand, h, zipfExponent);

		for (int i = 0; i < n; i++) { // for each history...
			result[i] = zipf.sample(r); // ...generate r requests

		}

		System.out.println("... done.");

		return result;

	}

}
