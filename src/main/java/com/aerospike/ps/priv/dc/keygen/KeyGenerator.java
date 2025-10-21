package com.aerospike.ps.priv.dc.keygen;

import com.aerospike.client.Key;

public interface KeyGenerator {
	public Key generate(String name, int keyLength, String namespace, String set);
}
