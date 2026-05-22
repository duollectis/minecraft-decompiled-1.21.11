package net.minecraft.util.thread;

import com.mojang.logging.LogUtils;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Вспомогательный класс для обнаружения одновременного доступа к ресурсу из нескольких потоков.
 * При попытке захвата уже занятого ресурса блокирует вызывающий поток и бросает {@link CrashException}
 * с дампами стеков обоих потоков для диагностики.
 */
public class LockHelper {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final String name;
	private final Semaphore semaphore = new Semaphore(1);
	private final Lock lock = new ReentrantLock();
	private volatile @Nullable Thread thread;
	private volatile @Nullable CrashException crashException;

	public LockHelper(String name) {
		this.name = name;
	}

	/**
	 * Захватывает ресурс. Если ресурс уже занят другим потоком, регистрирует текущий поток
	 * как ожидающего, освобождает внутренний замок и блокируется на семафоре до момента
	 * разблокировки — после чего бросает {@link CrashException} с диагностикой.
	 */
	public void lock() {
		boolean waiting = false;

		try {
			lock.lock();

			if (!semaphore.tryAcquire()) {
				thread = Thread.currentThread();
				waiting = true;
				lock.unlock();

				try {
					semaphore.acquire();
				}
				catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				}

				throw crashException;
			}
		}
		finally {
			if (!waiting) {
				lock.unlock();
			}
		}
	}

	/**
	 * Освобождает ресурс. Если есть ожидающий поток, формирует {@link CrashException}
	 * с дампами стеков, сохраняет его для ожидающего и бросает его же в текущем потоке.
	 */
	public void unlock() {
		try {
			lock.lock();
			Thread waitingThread = thread;

			if (waitingThread != null) {
				CrashException exception = crash(name, waitingThread);
				crashException = exception;
				semaphore.release();
				throw exception;
			}

			semaphore.release();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Формирует {@link CrashException} с дампами стеков текущего и указанного потоков,
	 * сигнализируя о конкурентном доступе к ресурсу {@code message}.
	 */
	public static CrashException crash(String message, @Nullable Thread thread) {
		String stackTraces = Stream.of(Thread.currentThread(), thread)
			.filter(Objects::nonNull)
			.map(LockHelper::formatStackTraceForThread)
			.collect(Collectors.joining("\n"));

		String description = "Accessing " + message + " from multiple threads";
		CrashReport crashReport = new CrashReport(description, new IllegalStateException(description));
		CrashReportSection section = crashReport.addElement("Thread dumps");
		section.add("Thread dumps", stackTraces);
		LOGGER.error("Thread dumps: \n{}", stackTraces);
		return new CrashException(crashReport);
	}

	private static String formatStackTraceForThread(Thread thread) {
		return thread.getName() + ": \n\tat " + Arrays
			.stream(thread.getStackTrace())
			.map(Object::toString)
			.collect(Collectors.joining("\n\tat "));
	}
}
