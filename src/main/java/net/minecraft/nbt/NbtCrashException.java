package net.minecraft.nbt;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

/**
 * {@code NbtCrashException}.
 */
public class NbtCrashException extends CrashException {

	public NbtCrashException(CrashReport crashReport) {
		super(crashReport);
	}
}
