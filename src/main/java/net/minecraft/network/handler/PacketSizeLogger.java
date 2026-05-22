package net.minecraft.network.handler;

import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Потокобезопасный счётчик суммарного объёма входящих пакетов за один тик.
 * Накапливает байты через {@link #increment} и сбрасывает накопленное значение
 * в профилировщик через {@link #push}, вызываемый раз в тик.
 */
public class PacketSizeLogger {

	private final AtomicInteger packetSizeInBytes = new AtomicInteger();
	private final MultiValueDebugSampleLogImpl log;

	public PacketSizeLogger(MultiValueDebugSampleLogImpl log) {
		this.log = log;
	}

	public void increment(int bytes) {
		packetSizeInBytes.getAndAdd(bytes);
	}

	public void push() {
		log.push(packetSizeInBytes.getAndSet(0));
	}
}
