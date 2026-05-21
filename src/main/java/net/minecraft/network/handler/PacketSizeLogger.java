package net.minecraft.network.handler;

import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;

import java.util.concurrent.atomic.AtomicInteger;

public class PacketSizeLogger {

	private final AtomicInteger packetSizeInBytes = new AtomicInteger();
	private final MultiValueDebugSampleLogImpl log;

	public PacketSizeLogger(MultiValueDebugSampleLogImpl log) {
		this.log = log;
	}

	public void increment(int bytes) {
		this.packetSizeInBytes.getAndAdd(bytes);
	}

	public void push() {
		this.log.push(this.packetSizeInBytes.getAndSet(0));
	}
}
