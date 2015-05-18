package de.donotconnect.notary_cache.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class CacheTest {

	@Test
	public void fromStringTest() throws CacheException {

		String cache = "109.230.236.173;80;z.donotconnect.de;1431973272;1432578072;SHA-256;SHA256withRSA\n"
				+ "173.194.113.68;443;google.com;RSA;eab431be73ce632f233cce73422d4a5ad00aad69065a7fc76d45e567a8a3b055,c3f697a92a293d86f9a3ee7ccb970e20e0050b8728cc83ed1b996ce9005d4c36,3c35cc963eb004451323d3275d05b353235053490d9cd83729a2faf5e7ca1cc0;0\n"
				+ "173.194.44.66;443;google.com;RSA;eab431be73ce632f233cce73422d4a5ad00aad69065a7fc76d45e567a8a3b055,c3f697a92a293d86f9a3ee7ccb970e20e0050b8728cc83ed1b996ce9005d4c36,3c35cc963eb004451323d3275d05b353235053490d9cd83729a2faf5e7ca1cc0;0\n"
				+ "173.194.44.67;443;google.com;RSA;eab431be73ce632f233cce73422d4a5ad00aad69065a7fc76d45e567a8a3b055,c3f697a92a293d86f9a3ee7ccb970e20e0050b8728cc83ed1b996ce9005d4c36,3c35cc963eb004451323d3275d05b353235053490d9cd83729a2faf5e7ca1cc0;0\n"
				+ "173.194.113.69;443;google.com;RSA;eab431be73ce632f233cce73422d4a5ad00aad69065a7fc76d45e567a8a3b055,c3f697a92a293d86f9a3ee7ccb970e20e0050b8728cc83ed1b996ce9005d4c36,3c35cc963eb004451323d3275d05b353235053490d9cd83729a2faf5e7ca1cc0;0\n"
				+ "173.194.113.66;443;google.com;RSA;eab431be73ce632f233cce73422d4a5ad00aad69065a7fc76d45e567a8a3b055,c3f697a92a293d86f9a3ee7ccb970e20e0050b8728cc83ed1b996ce9005d4c36,3c35cc963eb004451323d3275d05b353235053490d9cd83729a2faf5e7ca1cc0;0\n"
				+ "173.194.44.73;443;google.com;RSA;eab431be73ce632f233cce73422d4a5ad00aad69065a7fc76d45e567a8a3b055,c3f697a92a293d86f9a3ee7ccb970e20e0050b8728cc83ed1b996ce9005d4c36,3c35cc963eb004451323d3275d05b353235053490d9cd83729a2faf5e7ca1cc0;0\n"
				+ "3DA09G9zKz2lwR86mfgbxntRR5u4eFlbKQkSox5g+raFccTnbJaWxYDb8yVtvdxWyEImZ7Cl4yBt0F33UD+xFtufZ6b2nQNELJW1/vC4kyQ6xIZ9sHxyOmuWksq3XJByTT9gOW6YSzSbCxCxlsea17R/weApmz4Pc2+Jr3FUjBDvD+U8Llsc9oQsP5ykgzIMGppvROaCYzI8wCmX499UaJUaYPw/aRDz80BxrW/Dqm+5OeACrKiZ3WJXz4H/iZztmQQyz6/dk3sgOHn1iIbytkv7b4cA2FU2ifmlQvi7zISUy1zkoZJc+1fkZXwLD+xG2hADeP6uuwEuDA0qGXFgUMREXMGZCIIM8h34VwMRqCh901lgblGTQeuwWTA2C+nWFErKe77dvt3NeAWpKgU0s/Jsm7e0XPlA10+9Kubtd/mfV3Uv0kflLHZiBpmoeuIvQFC22JSNWhnkAnEVxC9MgMhItWgrV2txDoZz1I45xDOMfhjOd960U+Lkd8TMiegCNl6uzxbZC5dJiBz4BtqU8Q9QZm3J8woNh0MRKdtDUOeS98+BGIYGvHWfYp+fW8HTjZnBjKG165pScRDeFN26mv3+9RgCLC/nILyV0P7gGKf4O6cRxlrpQ9seZnsr3ytjBaFIqCi7IvmmzVdsYWDfVDOhyU1v/a0LwXs3XRSCnHQ=";
	
		Cache c = new Cache(cache);
		
		assertTrue(c.getCache().size()==6);
	
	}
	
	
}
