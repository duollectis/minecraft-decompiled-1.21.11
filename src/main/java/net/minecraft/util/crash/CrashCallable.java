package net.minecraft.util.crash;

import java.util.concurrent.Callable;

/**
 * Специализация {@link Callable} для использования в секциях отчёта о сбое.
 * Позволяет лямбдам и ссылкам на методы напрямую передаваться в {@link CrashReportSection#add}.
 */
public interface CrashCallable<V> extends Callable<V> {
}
