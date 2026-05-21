package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;

/**
 * {@code NbtSizeTracker}.
 */
public class NbtSizeTracker {

	public static final int PACKET_MAX_SIZE = 2097152;
	public static final int LEVEL_MAX_SIZE = 104857600;
	private static final int DEFAULT_MAX_DEPTH = 512;
	private final long maxBytes;
	private long allocatedBytes;
	private final int maxDepth;
	private int depth;

	public NbtSizeTracker(long maxBytes, int maxDepth) {
		this.maxBytes = maxBytes;
		this.maxDepth = maxDepth;
	}

	public static NbtSizeTracker of(long maxBytes) {
		return new NbtSizeTracker(maxBytes, 512);
	}

	public static NbtSizeTracker forPacket() {
		return new NbtSizeTracker(2097152L, 512);
	}

	public static NbtSizeTracker forLevel() {
		return new NbtSizeTracker(104857600L, 512);
	}

	public static NbtSizeTracker ofUnlimitedBytes() {
		return new NbtSizeTracker(Long.MAX_VALUE, 512);
	}

	public void add(long multiplier, long bytes) {
		this.add(multiplier * bytes);
	}

	public void add(long bytes) {
		if (bytes < 0L) {
			throw new IllegalArgumentException("Tried to account NBT tag with negative size: " + bytes);
		}
		else if (this.allocatedBytes + bytes > this.maxBytes) {
			throw new NbtSizeValidationException(
					"Tried to read NBT tag that was too big; tried to allocate: " + this.allocatedBytes + " + " + bytes
							+ " bytes where max allowed: " + this.maxBytes
			);
		}
		else {
			this.allocatedBytes += bytes;
		}
	}

	public void pushStack() {
		if (this.depth >= this.maxDepth) {
			throw new NbtSizeValidationException(
					"Tried to read NBT tag with too high complexity, depth > " + this.maxDepth);
		}
		else {
			this.depth++;
		}
	}

	public void popStack() {
		if (this.depth <= 0) {
			throw new NbtSizeValidationException("NBT-Accounter tried to pop stack-depth at top-level");
		}
		else {
			this.depth--;
		}
	}

	@VisibleForTesting
	public long getAllocatedBytes() {
		return this.allocatedBytes;
	}

	@VisibleForTesting
	public int getDepth() {
		return this.depth;
	}
}
