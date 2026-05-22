package net.minecraft.nbt;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

/**
 * Оборачивает {@link CrashReport} при критической ошибке чтения NBT-данных.
 * Позволяет передать подробный отчёт о сбое вверх по стеку вызовов.
 */
public class NbtCrashException extends CrashException {

	public NbtCrashException(CrashReport crashReport) {
		super(crashReport);
	}
}
