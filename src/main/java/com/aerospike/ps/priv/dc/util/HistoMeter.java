package com.aerospike.ps.priv.dc.util;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MovingAverages;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;

public class HistoMeter extends Meter {

	private Histogram histogram;

	public HistoMeter() {
		super();
		histogram = new Histogram(new com.codahale.metrics.UniformReservoir());
	}
	
	public HistoMeter(Reservoir reservoir) {
		super();
		histogram = new Histogram(reservoir);
	}

	public HistoMeter(Clock clock) {
		super(clock);
		histogram = new Histogram(new com.codahale.metrics.UniformReservoir());
	}

	public HistoMeter(MovingAverages movingAverages, Clock clock) {
		super(movingAverages, clock);
		histogram = new Histogram(new com.codahale.metrics.UniformReservoir());
	}

	public HistoMeter(MovingAverages movingAverages) {
		super(movingAverages);
		histogram = new Histogram(new com.codahale.metrics.UniformReservoir());
	}

	@Override
	public void mark() {
		super.mark();
		histogram.update(1);
	}

	@Override
	public void mark(long n) {
		super.mark(n);
		histogram.update(n);
	}

	public Snapshot getSnapshot() {
		return histogram.getSnapshot();
	}
}
