package de.donotconnect.notary_cache.client.Evaluation;

import java.io.IOException;
import java.util.Collection;

import de.donotconnect.notary_cache.client.Cache;
import de.donotconnect.notary_cache.client.CacheStructures.DefaultEntry;
import de.donotconnect.notary_cache.client.CacheStructures.ICache;

public class Metrics {

	public static void main(String[] args) throws IOException {

		System.out.println(System.getProperty("user.dir"));
		
		Cache c1 = Cache.fromFile("example", "example-cache.txt");
		ICache ca1 = c1.getCache();
		Collection<DefaultEntry> co1 = ca1.getCollection();

		float averageSize = 0; // in bytes
		int numberOfEntries = 0;

		for (DefaultEntry e : co1) {
			//System.out.println(e.toString());
			averageSize += e.toString().length();
			numberOfEntries++;
		}
		
		System.out.println("Number: "+averageSize+" for "+numberOfEntries+" entries.");
		
		averageSize /= numberOfEntries;
		
		System.out.println("Average size of an entry: "+averageSize);

	}

}
