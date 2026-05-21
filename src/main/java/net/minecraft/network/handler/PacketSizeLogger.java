package net.minecraft.network.handler;

import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс packet size logger.
 */
public class PacketSizeLogger {

	private final AtomicInteger packetSizeInBytes = new AtomicInteger();
	private final MultiValueDebugSampleLogImpl log;

	public PacketSizeLogger(MultiValueDebugSampleLogImpl log) {
		this.log = log;
	}

	/**
	 * Increment.
	 *
	 * @param bytes bytes
	 */
	public void increment(int bytes) {
		this.packetSizeInBytes.getAndAdd(bytes);
	}

	/**
	 * Push.
	 */
	public void push() {
		this.log.push(this.packetSizeInBytes.getAndSet(0));
	}
}
