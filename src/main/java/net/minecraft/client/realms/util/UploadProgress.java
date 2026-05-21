package net.minecraft.client.realms.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
/**
 * {@code UploadProgress}.
 */
public class UploadProgress {

	private volatile long bytesWritten;
	private volatile long totalBytes;
	private long startTimeMs = Util.getMeasuringTimeMs();
	private long lastBytesWritten;
	private long bytesPerSecond;

	public void setTotalBytes(long totalBytes) {
		this.totalBytes = totalBytes;
	}

	public void clear() {
		this.bytesWritten = 0L;
		this.startTimeMs = Util.getMeasuringTimeMs();
		this.lastBytesWritten = 0L;
		this.bytesPerSecond = 0L;
	}

	public long getTotalBytes() {
		return this.totalBytes;
	}

	public long getBytesWritten() {
		return this.bytesWritten;
	}

	public void addBytesWritten(long bytesWritten) {
		this.bytesWritten = bytesWritten;
	}

	public boolean hasWrittenBytes() {
		return this.bytesWritten > 0L;
	}

	public boolean hasWrittenAllBytes() {
		return this.bytesWritten >= this.totalBytes;
	}

	public double getFractionBytesWritten() {
		return Math.min((double) this.getBytesWritten() / this.getTotalBytes(), 1.0);
	}

	public void tick() {
		long l = Util.getMeasuringTimeMs();
		long m = l - this.startTimeMs;
		if (m >= 1000L) {
			long n = this.bytesWritten;
			this.bytesPerSecond = 1000L * (n - this.lastBytesWritten) / m;
			this.lastBytesWritten = n;
			this.startTimeMs = l;
		}
	}

	public long getBytesPerSecond() {
		return this.bytesPerSecond;
	}
}
