package com.aerospike.ps.priv.dc;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.ps.priv.dc.keygen.LongAdderSequentialKeyGenerator;

public class Config {

	public static class Write {
		public String id;
		public String namespace = "test";
		public String set = "csd";
		public int threads = 1;	
		public int preGenerate = 0;
		public int keyLength = 32;
		public long limit = Long.MAX_VALUE;
		public String keyGenerator = LongAdderSequentialKeyGenerator.class.getName();
		public double rateLimit = 0;
		public boolean sameRecordDifferentKey;
		public BinSpec[] binSpecs;
		public boolean useOperations;
		
		
		LongAdder counter = new LongAdder();
	}
	
	
	public static class Read {
		public String id;
		public String namespace = "test";
		public String set = "csd";
		public int threads = 1;	
		public int keyLength = 32;
		public long limit = Long.MAX_VALUE;
		public String keyGenerator = LongAdderSequentialKeyGenerator.class.getName();
		public double rateLimit = 0;
		public boolean recycleKeys = true;
		
		LongAdder counter = new LongAdder();
	}
	
	
	
	public static class BinSpec {
		public String key;
		public Map<String, Bin> bins;
	}
		
	public static class Bin {
		public DataType type;
		public long size;
		public DataType keyType;
		public int keyLength;
		public DataType elementType;
		public int elementLength;
		public String mask;
		public Object[] values;
	}

	private AerospikeClient mClient = null;
	
	public ClientPolicy clientPolicy;
	public String host = "127.0.0.1";
	public String[] hosts = {};
	public int port = 3000;
	
	public long duration = Long.MAX_VALUE;
	public int reportInterval = 0;
	public int infoInterval = 2;	
	public String name;
	public Write[] writes;
	public Read[] reads;
	public int keyBucketSize = 10000;
	public int maxKeyQueueSize = Integer.MAX_VALUE;
	

	public AerospikeClient getClient() {
		if ( mClient == null || !mClient.isConnected() ) {
			mClient = new AerospikeClient(clientPolicy, host, port);
		}
		
		return mClient;
	}
}
