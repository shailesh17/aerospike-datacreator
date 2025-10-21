package com.aerospike.ps.priv.dc;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.ps.priv.dc.Config.Read;
import com.aerospike.ps.priv.dc.keygen.KeyGenerator;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.util.concurrent.RateLimiter;

public class Reader implements Runnable {

	private Config mConfig;
	private Read mRead;
	private boolean mCancel;
	private KeyGenerator mKeyGenerator; 
	private Semaphore mCompleted;
	private AerospikeClient mClient;
	private RateLimiter mRateLimiter;
	private Listener mListener;

	private Timer mReadTimes;
	
	private Logger mLogger = Logger.getLogger(getClass().getSimpleName());
	
	public Reader(KeyGenerator keyGenerator, Config config, Read read, Listener listener, Semaphore completed) {
		this.mConfig = config;
		this.mRead = read;
		this.mKeyGenerator = keyGenerator;
		this.mCompleted = completed;
		this.mClient = config.getClient();
		this.mListener = listener;
		
		if ( mRead.rateLimit > 0 ) {
			this.mRateLimiter = RateLimiter.create(mRead.rateLimit);
		}
	}
	
	public void run() {

		try {
			String id = String.format("%s.%s", Thread.currentThread().getName(),mRead.id);
			

			mReadTimes = new Timer(new com.codahale.metrics.UniformReservoir());
			GenericDataCreator.metrics.register(id  + ".reads", mReadTimes);

			Key key;
			Record record;
			while (!mCancel && mRead.counter.longValue() < mRead.limit) {
				mRead.counter.increment();
				
				
				if ( mRateLimiter != null ) {
					 GenericDataCreator.WAIT.inc((long)mRateLimiter.acquire());
				}
				
				key = mKeyGenerator.generate(mConfig.name, mRead.keyLength, mRead.namespace, mRead.set);
				
				try (Context context = GenericDataCreator.READ_RECORD_TIMES.time()) {					
					try (Context c = mReadTimes.time()) {					
						record = mClient.get(null, key);
					}
					
					if ( record == null ) {
						
					} else {
						GenericDataCreator.READ_HITS.inc();
						
						mListener.recordRead(key, record);
						if ( mRead.recycleKeys ) {
							mListener.recycle(key);
						}
						if ( record.bins != null ) {
							long estimatedSize = record.bins.values().stream().mapToInt((v) -> Value.get(v).estimateSize()).sum();
							GenericDataCreator.NETWORK_READ.inc(estimatedSize);
							GenericDataCreator.OVERALL_READ_RATE.mark(estimatedSize);
						}
					}
					
				} catch (AerospikeException ae) {
					mLogger.log(Level.ALL, ae.getMessage(), ae);
					if ( ae.getResultCode() != 8 ) {
						break;
					}
				}
			}
			
		} catch (Exception e) {
			mLogger.log(Level.ALL, e.getMessage(), e);
		} finally {
			mCompleted.release();
		}
	}
	
	

	public void cancel() {
		mCancel = true;
	}
}