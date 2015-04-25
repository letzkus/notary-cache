package de.donotconnect.notary_cache.client.Evaluation;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 
 * @author Marco13
 * @url http://stackoverflow.com/questions/27105677/zipfs-law-in-java-for-text-
 *      generation-too-slow
 * @info last access: 19.04.2015
 *
 */
class FastZipfGenerator {
	private Random random = new Random(System.currentTimeMillis());
	private NavigableMap<Double, Integer> map;

	FastZipfGenerator(int size, double skew) {
		map = computeMap(size, skew);
	}

	private static NavigableMap<Double, Integer> computeMap(int size,
			double skew) {
		NavigableMap<Double, Integer> map = new TreeMap<Double, Integer>();

		double div = 0;
		for (int i = 1; i <= size; i++) {
			div += (1 / Math.pow(i, skew));
		}

		double sum = 0;
		for (int i = 1; i <= size; i++) {
			double p = (1.0d / Math.pow(i, skew)) / div;
			sum += p;
			map.put(sum, i - 1);
		}
		return map;
	}

	public int next() {
		double value = random.nextDouble();
		return map.ceilingEntry(value).getValue() + 1;
	}

}