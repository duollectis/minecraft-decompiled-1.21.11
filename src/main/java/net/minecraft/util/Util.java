package net.minecraft.util;

import com.google.common.base.Ticker;
import com.google.common.collect.*;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.types.Type;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.Schemas;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Property;
import net.minecraft.util.annotation.SuppressLinter;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.ReportType;
import net.minecraft.util.function.CharPredicate;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.thread.NameableExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.*;

/**
 * {@code Util}.
 */
public class Util {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_PARALLELISM = 255;
	private static final int BACKUP_ATTEMPTS = 10;
	private static final String MAX_BG_THREADS_PROPERTY = "max.bg.threads";
	private static final NameableExecutor MAIN_WORKER_EXECUTOR = createWorker("Main");
	private static final NameableExecutor IO_WORKER_EXECUTOR = createIoWorker("IO-Worker-", false);
	private static final NameableExecutor DOWNLOAD_WORKER_EXECUTOR = createIoWorker("Download-", true);
	private static final DateTimeFormatter
			DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
	public static final int CPU_COUNT = 8;
	private static final Set<String> SUPPORTED_URI_PROTOCOLS = Set.of("http", "https");
	public static final long NANOS_PER_MILLI = 1000000L;
	public static TimeSupplier.Nanoseconds nanoTimeSupplier = System::nanoTime;
	public static final Ticker TICKER = new Ticker() {
		/**
		 * Read.
		 *
		 * @return long — результат операции
		 */
		public long read() {
			return Util.nanoTimeSupplier.getAsLong();
		}
	};
	public static final UUID NIL_UUID = new UUID(0L, 0L);
	public static final FileSystemProvider JAR_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders()
	                                                                                    .stream()
	                                                                                    .filter(fileSystemProvider -> fileSystemProvider
			                                                                                    .getScheme()
			                                                                                    .equalsIgnoreCase("jar"))
	                                                                                    .findFirst()
	                                                                                    .orElseThrow(() -> new IllegalStateException(
			                                                                                    "No jar file system provider found"));
	private static Consumer<String> missingBreakpointHandler = message -> {};

	/**
	 * To map.
	 *
	 * @return Collector, ?, Map> — результат операции
	 */
	public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
		return Collectors.toMap(Entry::getKey, Entry::getValue);
	}

	/**
	 * To array list.
	 *
	 * @return Collector> — результат операции
	 */
	public static <T> Collector<T, ?, List<T>> toArrayList() {
		return Collectors.toCollection(Lists::newArrayList);
	}

	public static <T extends Comparable<T>> String getValueAsString(Property<T> property, Object value) {
		return property.name((T) value);
	}

	/**
	 * Создаёт translation key.
	 *
	 * @param type type
	 * @param id id
	 *
	 * @return String — результат операции
	 */
	public static String createTranslationKey(String type, @Nullable Identifier id) {
		return id == null ? type + ".unregistered_sadface"
		                  : type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
	}

	public static long getMeasuringTimeMs() {
		return getMeasuringTimeNano() / 1000000L;
	}

	public static long getMeasuringTimeNano() {
		return nanoTimeSupplier.getAsLong();
	}

	public static long getEpochTimeMs() {
		return Instant.now().toEpochMilli();
	}

	public static String getFormattedCurrentTime() {
		return DATE_TIME_FORMATTER.format(ZonedDateTime.now());
	}

	private static NameableExecutor createWorker(String name) {
		int i = getAvailableBackgroundThreads();
		ExecutorService executorService;
		if (i <= 0) {
			executorService = MoreExecutors.newDirectExecutorService();
		}
		else {
			AtomicInteger atomicInteger = new AtomicInteger(1);
			executorService = new ForkJoinPool(
					i, pool -> {
				final String string2 = "Worker-" + name + "-" + atomicInteger.getAndIncrement();
				ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(pool) {
					@Override
					protected void onStart() {
						TracyClient.setThreadName(string2, name.hashCode());
						super.onStart();
					}

					@Override
					protected void onTermination(@Nullable Throwable throwable) {
						if (throwable != null) {
							Util.LOGGER.warn("{} died", this.getName(), throwable);
						}
						else {
							Util.LOGGER.debug("{} shutdown", this.getName());
						}

						super.onTermination(throwable);
					}
				};
				forkJoinWorkerThread.setName(string2);
				return forkJoinWorkerThread;
			}, Util::uncaughtExceptionHandler, true
			);
		}

		return new NameableExecutor(executorService);
	}

	public static int getAvailableBackgroundThreads() {
		return MathHelper.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxBackgroundThreads());
	}

	private static int getMaxBackgroundThreads() {
		String string = System.getProperty("max.bg.threads");
		if (string != null) {
			try {
				int i = Integer.parseInt(string);
				if (i >= 1 && i <= 255) {
					return i;
				}

				LOGGER.error(
						"Wrong {} property value '{}'. Should be an integer value between 1 and {}.",
						new Object[]{"max.bg.threads", string, 255}
				);
			}
			catch (NumberFormatException var2) {
				LOGGER.error(
						"Could not parse {} property value '{}'. Should be an integer value between 1 and {}.",
						new Object[]{"max.bg.threads", string, 255}
				);
			}
		}

		return 255;
	}

	public static NameableExecutor getMainWorkerExecutor() {
		return MAIN_WORKER_EXECUTOR;
	}

	public static NameableExecutor getIoWorkerExecutor() {
		return IO_WORKER_EXECUTOR;
	}

	public static NameableExecutor getDownloadWorkerExecutor() {
		return DOWNLOAD_WORKER_EXECUTOR;
	}

	/**
	 * Shutdown executors.
	 */
	public static void shutdownExecutors() {
		MAIN_WORKER_EXECUTOR.shutdown(3L, TimeUnit.SECONDS);
		IO_WORKER_EXECUTOR.shutdown(3L, TimeUnit.SECONDS);
	}

	private static NameableExecutor createIoWorker(String namePrefix, boolean daemon) {
		AtomicInteger atomicInteger = new AtomicInteger(1);
		return new NameableExecutor(Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable);
			String string2 = namePrefix + atomicInteger.getAndIncrement();
			TracyClient.setThreadName(string2, namePrefix.hashCode());
			thread.setName(string2);
			thread.setDaemon(daemon);
			thread.setUncaughtExceptionHandler(Util::uncaughtExceptionHandler);
			return thread;
		}));
	}

	/**
	 * Throw unchecked.
	 *
	 * @param t t
	 */
	public static void throwUnchecked(Throwable t) {
		throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
	}

	private static void uncaughtExceptionHandler(Thread thread, Throwable t) {
		getFatalOrPause(t);
		if (t instanceof CompletionException) {
			t = t.getCause();
		}

		if (t instanceof CrashException crashException) {
			Bootstrap.println(crashException.getReport().asString(ReportType.MINECRAFT_CRASH_REPORT));
			System.exit(-1);
		}

		LOGGER.error("Caught exception in thread {}", thread, t);
	}

	public static @Nullable Type<?> getChoiceType(TypeReference typeReference, String id) {
		return !SharedConstants.useChoiceTypeRegistrations ? null : getChoiceTypeInternal(typeReference, id);
	}

	private static @Nullable Type<?> getChoiceTypeInternal(TypeReference typeReference, String id) {
		Type<?> type = null;

		try {
			type =
					Schemas
							.getFixer()
							.getSchema(DataFixUtils.makeKey(SharedConstants.getGameVersion().dataVersion().id()))
							.getChoiceType(typeReference, id);
		}
		catch (IllegalArgumentException var4) {
			LOGGER.error("No data fixer registered for {}", id);
			if (SharedConstants.isDevelopment) {
				throw var4;
			}
		}

		return type;
	}

	/**
	 * Run in named zone.
	 *
	 * @param runnable runnable
	 * @param name name
	 */
	public static void runInNamedZone(Runnable runnable, String name) {
		if (SharedConstants.isDevelopment) {
			Thread thread = Thread.currentThread();
			String string = thread.getName();
			thread.setName(name);

			try {
				Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment);

				try {
					runnable.run();
				}
				catch (Throwable var16) {
					if (zone != null) {
						try {
							zone.close();
						}
						catch (Throwable var14) {
							var16.addSuppressed(var14);
						}
					}

					throw var16;
				}

				if (zone != null) {
					zone.close();
				}
			}
			finally {
				thread.setName(string);
			}
		}
		else {
			Zone zone2 = TracyClient.beginZone(name, SharedConstants.isDevelopment);

			try {
				runnable.run();
			}
			catch (Throwable var15) {
				if (zone2 != null) {
					try {
						zone2.close();
					}
					catch (Throwable var13) {
						var15.addSuppressed(var13);
					}
				}

				throw var15;
			}

			if (zone2 != null) {
				zone2.close();
			}
		}
	}

	/**
	 * Registry value to string.
	 *
	 * @param registry registry
	 * @param value value
	 *
	 * @return String — результат операции
	 */
	public static <T> String registryValueToString(Registry<T> registry, T value) {
		Identifier identifier = registry.getId(value);
		return identifier == null ? "[unregistered]" : identifier.toString();
	}

	/**
	 * And.
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> and() {
		return o -> true;
	}

	/**
	 * And.
	 *
	 * @param a a
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> and(Predicate<? super T> a) {
		return (Predicate<T>) a;
	}

	/**
	 * And.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> and(Predicate<? super T> a, Predicate<? super T> b) {
		return o -> a.test(o) && b.test(o);
	}

	/**
	 * And.
	 *
	 * @param a a
	 * @param b b
	 * @param c c
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> and(Predicate<? super T> a, Predicate<? super T> b, Predicate<? super T> c) {
		return o -> a.test(o) && b.test(o) && c.test(o);
	}

	public static <T> Predicate<T> and(
			Predicate<? super T> a,
			Predicate<? super T> b,
			Predicate<? super T> c,
			Predicate<? super T> d
	) {
		return o -> a.test(o) && b.test(o) && c.test(o) && d.test(o);
	}

	public static <T> Predicate<T> and(
			Predicate<? super T> a,
			Predicate<? super T> b,
			Predicate<? super T> c,
			Predicate<? super T> d,
			Predicate<? super T> e
	) {
		return o -> a.test(o) && b.test(o) && c.test(o) && d.test(o) && e.test(o);
	}

	@SafeVarargs
	/**
	 * And.
	 *
	 * @param predicates predicates
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> and(Predicate<? super T>... predicates) {
		return o -> {
			for (Predicate<? super T> predicate : predicates) {
				if (!predicate.test(o)) {
					return false;
				}
			}

			return true;
		};
	}

	/**
	 * All of.
	 *
	 * @param predicates predicates
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> predicates) {
		return switch (predicates.size()) {
			case 0 -> and();
			case 1 -> and((Predicate<? super T>) predicates.get(0));
			case 2 -> and((Predicate<? super T>) predicates.get(0), (Predicate<? super T>) predicates.get(1));
			case 3 -> and(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1),
					(Predicate<? super T>) predicates.get(2)
			);
			case 4 -> and(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1),
					(Predicate<? super T>) predicates.get(2),
					(Predicate<? super T>) predicates.get(3)
			);
			case 5 -> and(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1),
					(Predicate<? super T>) predicates.get(2),
					(Predicate<? super T>) predicates.get(3),
					(Predicate<? super T>) predicates.get(4)
			);
			default -> {
				Predicate<? super T>[] predicates2 = predicates.toArray(Predicate[]::new);
				yield and(predicates2);
			}
		};
	}

	/**
	 * Or.
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> or() {
		return o -> false;
	}

	/**
	 * Or.
	 *
	 * @param a a
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> or(Predicate<? super T> a) {
		return (Predicate<T>) a;
	}

	/**
	 * Or.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> or(Predicate<? super T> a, Predicate<? super T> b) {
		return o -> a.test(o) || b.test(o);
	}

	/**
	 * Or.
	 *
	 * @param a a
	 * @param b b
	 * @param c c
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> or(Predicate<? super T> a, Predicate<? super T> b, Predicate<? super T> c) {
		return o -> a.test(o) || b.test(o) || c.test(o);
	}

	public static <T> Predicate<T> or(
			Predicate<? super T> a,
			Predicate<? super T> b,
			Predicate<? super T> c,
			Predicate<? super T> d
	) {
		return o -> a.test(o) || b.test(o) || c.test(o) || d.test(o);
	}

	public static <T> Predicate<T> or(
			Predicate<? super T> a,
			Predicate<? super T> b,
			Predicate<? super T> c,
			Predicate<? super T> d,
			Predicate<? super T> e
	) {
		return o -> a.test(o) || b.test(o) || c.test(o) || d.test(o) || e.test(o);
	}

	@SafeVarargs
	/**
	 * Or.
	 *
	 * @param predicates predicates
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> or(Predicate<? super T>... predicates) {
		return o -> {
			for (Predicate<? super T> predicate : predicates) {
				if (predicate.test(o)) {
					return true;
				}
			}

			return false;
		};
	}

	/**
	 * Any of.
	 *
	 * @param predicates predicates
	 *
	 * @return Predicate — результат операции
	 */
	public static <T> Predicate<T> anyOf(List<? extends Predicate<? super T>> predicates) {
		return switch (predicates.size()) {
			case 0 -> or();
			case 1 -> or((Predicate<? super T>) predicates.get(0));
			case 2 -> or((Predicate<? super T>) predicates.get(0), (Predicate<? super T>) predicates.get(1));
			case 3 -> or(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1),
					(Predicate<? super T>) predicates.get(2)
			);
			case 4 -> or(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1),
					(Predicate<? super T>) predicates.get(2),
					(Predicate<? super T>) predicates.get(3)
			);
			case 5 -> or(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1),
					(Predicate<? super T>) predicates.get(2),
					(Predicate<? super T>) predicates.get(3),
					(Predicate<? super T>) predicates.get(4)
			);
			default -> {
				Predicate<? super T>[] predicates2 = predicates.toArray(Predicate[]::new);
				yield or(predicates2);
			}
		};
	}

	public static <T> boolean isSymmetrical(int width, int height, List<T> list) {
		if (width == 1) {
			return true;
		}
		else {
			int i = width / 2;

			for (int j = 0; j < height; j++) {
				for (int k = 0; k < i; k++) {
					int l = width - 1 - k;
					T object = list.get(k + j * width);
					T object2 = list.get(l + j * width);
					if (!object.equals(object2)) {
						return false;
					}
				}
			}

			return true;
		}
	}

	/**
	 * Next capacity.
	 *
	 * @param current current
	 * @param min min
	 *
	 * @return int — результат операции
	 */
	public static int nextCapacity(int current, int min) {
		return (int) Math.max(Math.min((long) current + (current >> 1), 2147483639L), (long) min);
	}

	@SuppressLinter(reason = "Intentional use of default locale for user-visible date")
	public static DateTimeFormatter getDefaultLocaleFormatter(FormatStyle style) {
		return DateTimeFormatter.ofLocalizedDateTime(style);
	}

	public static Util.OperatingSystem getOperatingSystem() {
		String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (string.contains("win")) {
			return Util.OperatingSystem.WINDOWS;
		}
		else if (string.contains("mac")) {
			return Util.OperatingSystem.OSX;
		}
		else if (string.contains("solaris")) {
			return Util.OperatingSystem.SOLARIS;
		}
		else if (string.contains("sunos")) {
			return Util.OperatingSystem.SOLARIS;
		}
		else if (string.contains("linux")) {
			return Util.OperatingSystem.LINUX;
		}
		else {
			return string.contains("unix") ? Util.OperatingSystem.LINUX : Util.OperatingSystem.UNKNOWN;
		}
	}

	public static boolean isOnAarch64() {
		String string = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		return string.equals("aarch64");
	}

	/**
	 * Валидирует uri.
	 *
	 * @param uri uri
	 *
	 * @return URI — результат операции
	 */
	public static URI validateUri(String uri) throws URISyntaxException {
		URI uRI = new URI(uri);
		String string = uRI.getScheme();
		if (string == null) {
			throw new URISyntaxException(uri, "Missing protocol in URI: " + uri);
		}
		else {
			String string2 = string.toLowerCase(Locale.ROOT);
			if (!SUPPORTED_URI_PROTOCOLS.contains(string2)) {
				throw new URISyntaxException(uri, "Unsupported protocol in URI: " + uri);
			}
			else {
				return uRI;
			}
		}
	}

	/**
	 * Next.
	 *
	 * @param iterable iterable
	 * @param object object
	 *
	 * @return T — результат операции
	 */
	public static <T> T next(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T object2 = iterator.next();
		if (object != null) {
			T object3 = object2;

			while (object3 != object) {
				if (iterator.hasNext()) {
					object3 = iterator.next();
				}
			}

			if (iterator.hasNext()) {
				return iterator.next();
			}
		}

		return object2;
	}

	/**
	 * Previous.
	 *
	 * @param iterable iterable
	 * @param object object
	 *
	 * @return T — результат операции
	 */
	public static <T> T previous(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T object2 = null;

		while (iterator.hasNext()) {
			T object3 = iterator.next();
			if (object3 == object) {
				if (object2 == null) {
					object2 = (T) (iterator.hasNext() ? Iterators.getLast(iterator) : object);
				}
				break;
			}

			object2 = object3;
		}

		return object2;
	}

	/**
	 * Make.
	 *
	 * @param factory factory
	 *
	 * @return T — результат операции
	 */
	public static <T> T make(Supplier<T> factory) {
		return factory.get();
	}

	/**
	 * Make.
	 *
	 * @param object object
	 * @param initializer initializer
	 *
	 * @return T — результат операции
	 */
	public static <T> T make(T object, Consumer<? super T> initializer) {
		initializer.accept(object);
		return object;
	}

	/**
	 * Map enum.
	 *
	 * @param enumClass enum class
	 * @param mapper mapper
	 *
	 * @return , V> Map — результат операции
	 */
	public static <K extends Enum<K>, V> Map<K, V> mapEnum(Class<K> enumClass, Function<K, V> mapper) {
		EnumMap<K, V> enumMap = new EnumMap<>(enumClass);

		for (K enum_ : (K[]) enumClass.getEnumConstants()) {
			enumMap.put(enum_, mapper.apply(enum_));
		}

		return enumMap;
	}

	/**
	 * Трансформирует map values.
	 *
	 * @param map map
	 * @param V1 v1
	 * @param transformer transformer
	 *
	 * @return Map — результат операции
	 */
	public static <K, V1, V2> Map<K, V2> transformMapValues(Map<K, V1> map, Function<? super V1, V2> transformer) {
		return map
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Entry::getKey, entry -> transformer.apply(entry.getValue())));
	}

	public static <K, V1, V2> Map<K, V2> transformMapValuesLazy(
			Map<K, V1> map,
			com.google.common.base.Function<V1, V2> transformer
	) {
		return Maps.transformValues(map, transformer);
	}

	/**
	 * Combine safe.
	 *
	 * @param futures futures
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public static <V> CompletableFuture<List<V>> combineSafe(List<? extends CompletableFuture<V>> futures) {
		if (futures.isEmpty()) {
			return CompletableFuture.completedFuture(List.of());
		}
		else if (futures.size() == 1) {
			return futures.getFirst().thenApply(ObjectLists::singleton);
		}
		else {
			CompletableFuture<Void>
					completableFuture =
					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			return completableFuture.thenApply(void_ -> futures.stream().map(CompletableFuture::join).toList());
		}
	}

	/**
	 * Combine.
	 *
	 * @param futures futures
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public static <V> CompletableFuture<List<V>> combine(List<? extends CompletableFuture<? extends V>> futures) {
		CompletableFuture<List<V>> completableFuture = new CompletableFuture<>();
		return combine(futures, completableFuture::completeExceptionally).applyToEither(
				completableFuture,
				Function.identity()
		);
	}

	/**
	 * Combine cancellable.
	 *
	 * @param futures futures
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public static <V> CompletableFuture<List<V>> combineCancellable(List<? extends CompletableFuture<? extends V>> futures) {
		CompletableFuture<List<V>> completableFuture = new CompletableFuture<>();
		return combine(
				futures, throwable -> {
					if (completableFuture.completeExceptionally(throwable)) {
						for (CompletableFuture<? extends V> completableFuture2 : futures) {
							completableFuture2.cancel(true);
						}
					}
				}
		).applyToEither(completableFuture, Function.identity());
	}

	private static <V> CompletableFuture<List<V>> combine(
			List<? extends CompletableFuture<? extends V>> futures,
			Consumer<Throwable> exceptionHandler
	) {
		ObjectArrayList<V> objectArrayList = new ObjectArrayList();
		objectArrayList.size(futures.size());
		CompletableFuture<?>[] completableFutures = new CompletableFuture[futures.size()];

		for (int i = 0; i < futures.size(); i++) {
			int j = i;
			completableFutures[i] = futures.get(i).whenComplete((value, throwable) -> {
				if (throwable != null) {
					exceptionHandler.accept(throwable);
				}
				else {
					objectArrayList.set(j, value);
				}
			});
		}

		return CompletableFuture.allOf(completableFutures).thenApply(void_ -> objectArrayList);
	}

	public static <T> Optional<T> ifPresentOrElse(
			Optional<T> optional,
			Consumer<T> presentAction,
			Runnable elseAction
	) {
		if (optional.isPresent()) {
			presentAction.accept(optional.get());
		}
		else {
			elseAction.run();
		}

		return optional;
	}

	/**
	 * Debug supplier.
	 *
	 * @param supplier supplier
	 * @param messageSupplier message supplier
	 *
	 * @return Supplier — результат операции
	 */
	public static <T> Supplier<T> debugSupplier(Supplier<T> supplier, Supplier<String> messageSupplier) {
		if (SharedConstants.NAMED_RUNNABLES) {
			final String string = messageSupplier.get();
			return new Supplier<T>() {
				@Override
				public T get() {
					return supplier.get();
				}

				@Override
				public String toString() {
					return string;
				}
			};
		}
		else {
			return supplier;
		}
	}

	/**
	 * Debug runnable.
	 *
	 * @param runnable runnable
	 * @param messageSupplier message supplier
	 *
	 * @return Runnable — результат операции
	 */
	public static Runnable debugRunnable(Runnable runnable, Supplier<String> messageSupplier) {
		if (SharedConstants.NAMED_RUNNABLES) {
			final String string = messageSupplier.get();
			return new Runnable() {
				@Override
				public void run() {
					runnable.run();
				}

				@Override
				public String toString() {
					return string;
				}
			};
		}
		else {
			return runnable;
		}
	}

	/**
	 * Логирует error or pause.
	 *
	 * @param message message
	 */
	public static void logErrorOrPause(String message) {
		LOGGER.error(message);
		if (SharedConstants.isDevelopment) {
			pause(message);
		}
	}

	/**
	 * Логирует error or pause.
	 *
	 * @param message message
	 * @param throwable throwable
	 */
	public static void logErrorOrPause(String message, Throwable throwable) {
		LOGGER.error(message, throwable);
		if (SharedConstants.isDevelopment) {
			pause(message);
		}
	}

	public static <T extends Throwable> T getFatalOrPause(T t) {
		if (SharedConstants.isDevelopment) {
			LOGGER.error("Trying to throw a fatal exception, pausing in IDE", t);
			pause(t.getMessage());
		}

		return t;
	}

	public static void setMissingBreakpointHandler(Consumer<String> missingBreakpointHandler) {
		Util.missingBreakpointHandler = missingBreakpointHandler;
	}

	private static void pause(String message) {
		Instant instant = Instant.now();
		LOGGER.warn("Did you remember to set a breakpoint here?");
		boolean bl = Duration.between(instant, Instant.now()).toMillis() > 500L;
		if (!bl) {
			missingBreakpointHandler.accept(message);
		}
	}

	public static String getInnermostMessage(Throwable t) {
		if (t.getCause() != null) {
			return getInnermostMessage(t.getCause());
		}
		else {
			return t.getMessage() != null ? t.getMessage() : t.toString();
		}
	}

	public static <T> T getRandom(T[] array, Random random) {
		return array[random.nextInt(array.length)];
	}

	public static int getRandom(int[] array, Random random) {
		return array[random.nextInt(array.length)];
	}

	public static <T> T getRandom(List<T> list, Random random) {
		return list.get(random.nextInt(list.size()));
	}

	public static <T> Optional<T> getRandomOrEmpty(List<T> list, Random random) {
		return list.isEmpty() ? Optional.empty() : Optional.of(getRandom(list, random));
	}

	private static BooleanSupplier renameTask(Path src, Path dest) {
		return new BooleanSupplier() {
			@Override
			public boolean getAsBoolean() {
				try {
					Files.move(src, dest);
					return true;
				}
				catch (IOException var2) {
					Util.LOGGER.error("Failed to rename", var2);
					return false;
				}
			}

			@Override
			public String toString() {
				return "rename " + src + " to " + dest;
			}
		};
	}

	private static BooleanSupplier deleteTask(Path path) {
		return new BooleanSupplier() {
			@Override
			public boolean getAsBoolean() {
				try {
					Files.deleteIfExists(path);
					return true;
				}
				catch (IOException var2) {
					Util.LOGGER.warn("Failed to delete", var2);
					return false;
				}
			}

			@Override
			public String toString() {
				return "delete old " + path;
			}
		};
	}

	private static BooleanSupplier deletionVerifyTask(Path path) {
		return new BooleanSupplier() {
			@Override
			public boolean getAsBoolean() {
				return !Files.exists(path);
			}

			@Override
			public String toString() {
				return "verify that " + path + " is deleted";
			}
		};
	}

	private static BooleanSupplier existenceCheckTask(Path path) {
		return new BooleanSupplier() {
			@Override
			public boolean getAsBoolean() {
				return Files.isRegularFile(path);
			}

			@Override
			public String toString() {
				return "verify that " + path + " is present";
			}
		};
	}

	private static boolean attemptTasks(BooleanSupplier... tasks) {
		for (BooleanSupplier booleanSupplier : tasks) {
			if (!booleanSupplier.getAsBoolean()) {
				LOGGER.warn("Failed to execute {}", booleanSupplier);
				return false;
			}
		}

		return true;
	}

	private static boolean attemptTasks(int retries, String taskName, BooleanSupplier... tasks) {
		for (int i = 0; i < retries; i++) {
			if (attemptTasks(tasks)) {
				return true;
			}

			LOGGER.error("Failed to {}, retrying {}/{}", new Object[]{taskName, i, retries});
		}

		LOGGER.error("Failed to {}, aborting, progress might be lost", taskName);
		return false;
	}

	/**
	 * Backup and replace.
	 *
	 * @param current current
	 * @param newPath new path
	 * @param backup backup
	 */
	public static void backupAndReplace(Path current, Path newPath, Path backup) {
		backupAndReplace(current, newPath, backup, false);
	}

	/**
	 * Backup and replace.
	 *
	 * @param current current
	 * @param newPath new path
	 * @param backup backup
	 * @param noRestoreOnFail no restore on fail
	 *
	 * @return boolean — результат операции
	 */
	public static boolean backupAndReplace(Path current, Path newPath, Path backup, boolean noRestoreOnFail) {
		if (Files.exists(current) && !attemptTasks(
				10,
				"create backup " + backup,
				deleteTask(backup),
				renameTask(current, backup),
				existenceCheckTask(backup)
		)) {
			return false;
		}
		else if (!attemptTasks(10, "remove old " + current, deleteTask(current), deletionVerifyTask(current))) {
			return false;
		}
		else if (!attemptTasks(
				10,
				"replace " + current + " with " + newPath,
				renameTask(newPath, current),
				existenceCheckTask(current)
		) && !noRestoreOnFail) {
			attemptTasks(
					10,
					"restore " + current + " from " + backup,
					renameTask(backup, current),
					existenceCheckTask(current)
			);
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Перемещает cursor.
	 *
	 * @param string string
	 * @param cursor cursor
	 * @param delta delta
	 *
	 * @return int — результат операции
	 */
	public static int moveCursor(String string, int cursor, int delta) {
		int i = string.length();
		if (delta >= 0) {
			for (int j = 0; cursor < i && j < delta; j++) {
				if (Character.isHighSurrogate(string.charAt(cursor++)) && cursor < i
						&& Character.isLowSurrogate(string.charAt(cursor))) {
					cursor++;
				}
			}
		}
		else {
			for (int jx = delta; cursor > 0 && jx < 0; jx++) {
				cursor--;
				if (Character.isLowSurrogate(string.charAt(cursor)) && cursor > 0
						&& Character.isHighSurrogate(string.charAt(cursor - 1))) {
					cursor--;
				}
			}
		}

		return cursor;
	}

	/**
	 * Добавляет prefix.
	 *
	 * @param prefix prefix
	 * @param consumer consumer
	 *
	 * @return Consumer — результат операции
	 */
	public static Consumer<String> addPrefix(String prefix, Consumer<String> consumer) {
		return string -> consumer.accept(prefix + string);
	}

	/**
	 * Декодирует fixed length array.
	 *
	 * @param stream stream
	 * @param length length
	 *
	 * @return DataResult — результат операции
	 */
	public static DataResult<int[]> decodeFixedLengthArray(IntStream stream, int length) {
		int[] is = stream.limit(length + 1).toArray();
		if (is.length != length) {
			Supplier<String> supplier = () -> "Input is not a list of " + length + " ints";
			return is.length >= length ? DataResult.error(supplier, Arrays.copyOf(is, length))
			                           : DataResult.error(supplier);
		}
		else {
			return DataResult.success(is);
		}
	}

	/**
	 * Декодирует fixed length array.
	 *
	 * @param stream stream
	 * @param length length
	 *
	 * @return DataResult — результат операции
	 */
	public static DataResult<long[]> decodeFixedLengthArray(LongStream stream, int length) {
		long[] ls = stream.limit(length + 1).toArray();
		if (ls.length != length) {
			Supplier<String> supplier = () -> "Input is not a list of " + length + " longs";
			return ls.length >= length ? DataResult.error(supplier, Arrays.copyOf(ls, length))
			                           : DataResult.error(supplier);
		}
		else {
			return DataResult.success(ls);
		}
	}

	/**
	 * Декодирует fixed length list.
	 *
	 * @param list list
	 * @param length length
	 *
	 * @return DataResult> — результат операции
	 */
	public static <T> DataResult<List<T>> decodeFixedLengthList(List<T> list, int length) {
		if (list.size() != length) {
			Supplier<String> supplier = () -> "Input is not a list of " + length + " elements";
			return list.size() >= length ? DataResult.error(supplier, list.subList(0, length))
			                             : DataResult.error(supplier);
		}
		else {
			return DataResult.success(list);
		}
	}

	/**
	 * Запускает timer hack.
	 */
	public static void startTimerHack() {
		Thread thread = new Thread("Timer hack thread") {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(2147483647L);
					}
					catch (InterruptedException var2) {
						Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
						return;
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
		thread.start();
	}

	/**
	 * Relative copy.
	 *
	 * @param src src
	 * @param dest dest
	 * @param toCopy to copy
	 */
	public static void relativeCopy(Path src, Path dest, Path toCopy) throws IOException {
		Path path = src.relativize(toCopy);
		Path path2 = dest.resolve(path);
		Files.copy(toCopy, path2);
	}

	/**
	 * Replace invalid chars.
	 *
	 * @param string string
	 * @param predicate predicate
	 *
	 * @return String — результат операции
	 */
	public static String replaceInvalidChars(String string, CharPredicate predicate) {
		return string.toLowerCase(Locale.ROOT)
		             .chars()
		             .mapToObj(charCode -> predicate.test((char) charCode) ? Character.toString((char) charCode) : "_")
		             .collect(Collectors.joining());
	}

	/**
	 * Cached mapper.
	 *
	 * @param mapper mapper
	 *
	 * @return CachedMapper — результат операции
	 */
	public static <K, V> CachedMapper<K, V> cachedMapper(Function<K, V> mapper) {
		return new CachedMapper<>(mapper);
	}

	/**
	 * Memoize.
	 *
	 * @param function function
	 *
	 * @return Function — результат операции
	 */
	public static <T, R> Function<T, R> memoize(Function<T, R> function) {
		return new Function<T, R>() {
			private final Map<T, R> cache = new ConcurrentHashMap<>();

			@Override
			public R apply(T object) {
				return this.cache.computeIfAbsent(object, function);
			}

			@Override
			public String toString() {
				return "memoize/1[function=" + function + ", size=" + this.cache.size() + "]";
			}
		};
	}

	/**
	 * Memoize.
	 *
	 * @param biFunction bi function
	 *
	 * @return BiFunction — результат операции
	 */
	public static <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> biFunction) {
		return new BiFunction<T, U, R>() {
			private final Map<com.mojang.datafixers.util.Pair<T, U>, R> cache = new ConcurrentHashMap<>();

			@Override
			public R apply(T a, U b) {
				return this.cache.computeIfAbsent(
						com.mojang.datafixers.util.Pair.of(a, b),
						pair -> biFunction.apply((T) pair.getFirst(), (U) pair.getSecond())
				);
			}

			@Override
			public String toString() {
				return "memoize/2[function=" + biFunction + ", size=" + this.cache.size() + "]";
			}
		};
	}

	/**
	 * Создаёт копию shuffled.
	 *
	 * @param stream stream
	 * @param random random
	 *
	 * @return List — результат операции
	 */
	public static <T> List<T> copyShuffled(Stream<T> stream, Random random) {
		ObjectArrayList<T> objectArrayList = stream.collect(ObjectArrayList.toList());
		shuffle(objectArrayList, random);
		return objectArrayList;
	}

	/**
	 * Shuffle.
	 *
	 * @param stream stream
	 * @param random random
	 *
	 * @return IntArrayList — результат операции
	 */
	public static IntArrayList shuffle(IntStream stream, Random random) {
		IntArrayList intArrayList = IntArrayList.wrap(stream.toArray());
		int i = intArrayList.size();

		for (int j = i; j > 1; j--) {
			int k = random.nextInt(j);
			intArrayList.set(j - 1, intArrayList.set(k, intArrayList.getInt(j - 1)));
		}

		return intArrayList;
	}

	/**
	 * Создаёт копию shuffled.
	 *
	 * @param array array
	 * @param random random
	 *
	 * @return List — результат операции
	 */
	public static <T> List<T> copyShuffled(T[] array, Random random) {
		ObjectArrayList<T> objectArrayList = new ObjectArrayList(array);
		shuffle(objectArrayList, random);
		return objectArrayList;
	}

	/**
	 * Создаёт копию shuffled.
	 *
	 * @param list list
	 * @param random random
	 *
	 * @return List — результат операции
	 */
	public static <T> List<T> copyShuffled(ObjectArrayList<T> list, Random random) {
		ObjectArrayList<T> objectArrayList = new ObjectArrayList(list);
		shuffle(objectArrayList, random);
		return objectArrayList;
	}

	/**
	 * Shuffle.
	 *
	 * @param list list
	 * @param random random
	 *
	 * @return void — результат операции
	 */
	public static <T> void shuffle(List<T> list, Random random) {
		int i = list.size();

		for (int j = i; j > 1; j--) {
			int k = random.nextInt(j);
			list.set(j - 1, list.set(k, list.get(j - 1)));
		}
	}

	/**
	 * Wait and apply.
	 *
	 * @param resultFactory result factory
	 *
	 * @return CompletableFuture — результат операции
	 */
	public static <T> CompletableFuture<T> waitAndApply(Function<Executor, CompletableFuture<T>> resultFactory) {
		return waitAndApply(resultFactory, CompletableFuture::isDone);
	}

	/**
	 * Wait and apply.
	 *
	 * @param resultFactory result factory
	 * @param donePredicate done predicate
	 *
	 * @return T — результат операции
	 */
	public static <T> T waitAndApply(Function<Executor, T> resultFactory, Predicate<T> donePredicate) {
		BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
		T object = resultFactory.apply(blockingQueue::add);

		while (!donePredicate.test(object)) {
			try {
				Runnable runnable = blockingQueue.poll(100L, TimeUnit.MILLISECONDS);
				if (runnable != null) {
					runnable.run();
				}
			}
			catch (InterruptedException var5) {
				LOGGER.warn("Interrupted wait");
				break;
			}
		}

		int i = blockingQueue.size();
		if (i > 0) {
			LOGGER.warn("Tasks left in queue: {}", i);
		}

		return object;
	}

	/**
	 * Last index getter.
	 *
	 * @param values values
	 *
	 * @return ToIntFunction — результат операции
	 */
	public static <T> ToIntFunction<T> lastIndexGetter(List<T> values) {
		int i = values.size();
		if (i < 8) {
			return values::indexOf;
		}
		else {
			Object2IntMap<T> object2IntMap = new Object2IntOpenHashMap(i);
			object2IntMap.defaultReturnValue(-1);

			for (int j = 0; j < i; j++) {
				object2IntMap.put(values.get(j), j);
			}

			return object2IntMap;
		}
	}

	/**
	 * Last identity index getter.
	 *
	 * @param values values
	 *
	 * @return ToIntFunction — результат операции
	 */
	public static <T> ToIntFunction<T> lastIdentityIndexGetter(List<T> values) {
		int i = values.size();
		if (i < 8) {
			ReferenceList<T> referenceList = new ReferenceImmutableList(values);
			return referenceList::indexOf;
		}
		else {
			Reference2IntMap<T> reference2IntMap = new Reference2IntOpenHashMap(i);
			reference2IntMap.defaultReturnValue(-1);

			for (int j = 0; j < i; j++) {
				reference2IntMap.put(values.get(j), j);
			}

			return reference2IntMap;
		}
	}

	/**
	 * Apply.
	 *
	 * @param typed typed
	 * @param type type
	 * @param modifier modifier
	 *
	 * @return Typed — результат операции
	 */
	public static <A, B> Typed<B> apply(Typed<A> typed, Type<B> type, UnaryOperator<Dynamic<?>> modifier) {
		Dynamic<?> dynamic = (Dynamic<?>) typed.write().getOrThrow();
		return readTyped(type, modifier.apply(dynamic), true);
	}

	/**
	 * Читает typed.
	 *
	 * @param type type
	 * @param value value
	 *
	 * @return Typed — результат операции
	 */
	public static <T> Typed<T> readTyped(Type<T> type, Dynamic<?> value) {
		return readTyped(type, value, false);
	}

	/**
	 * Читает typed.
	 *
	 * @param type type
	 * @param value value
	 * @param allowPartial allow partial
	 *
	 * @return Typed — результат операции
	 */
	public static <T> Typed<T> readTyped(Type<T> type, Dynamic<?> value, boolean allowPartial) {
		DataResult<Typed<T>> dataResult = type.readTyped(value).map(com.mojang.datafixers.util.Pair::getFirst);

		try {
			return allowPartial ? (Typed) dataResult.getPartialOrThrow(IllegalStateException::new)
			                    : (Typed) dataResult.getOrThrow(IllegalStateException::new);
		}
		catch (IllegalStateException var7) {
			CrashReport crashReport = CrashReport.create(var7, "Reading type");
			CrashReportSection crashReportSection = crashReport.addElement("Info");
			crashReportSection.add("Data", value);
			crashReportSection.add("Type", type);
			throw new CrashException(crashReport);
		}
	}

	/**
	 * With appended.
	 *
	 * @param list list
	 * @param valueToAppend value to append
	 *
	 * @return List — результат операции
	 */
	public static <T> List<T> withAppended(List<T> list, T valueToAppend) {
		return ImmutableList.<T>builderWithExpectedSize(list.size() + 1).addAll(list).add(valueToAppend).build();
	}

	/**
	 * With prepended.
	 *
	 * @param valueToPrepend value to prepend
	 * @param list list
	 *
	 * @return List — результат операции
	 */
	public static <T> List<T> withPrepended(T valueToPrepend, List<T> list) {
		return ImmutableList.<T>builderWithExpectedSize(list.size() + 1).add(valueToPrepend).addAll(list).build();
	}

	/**
	 * Map with.
	 *
	 * @param map map
	 * @param keyToAppend key to append
	 * @param valueToAppend value to append
	 *
	 * @return Map — результат операции
	 */
	public static <K, V> Map<K, V> mapWith(Map<K, V> map, K keyToAppend, V valueToAppend) {
		return ImmutableMap
				.<K, V>builderWithExpectedSize(map.size() + 1)
				.putAll(map)
				.put(keyToAppend, valueToAppend)
				.buildKeepingLast();
	}

	/**
	 * {@code OperatingSystem}.
	 */
	public static enum OperatingSystem {
		LINUX("linux"),
		SOLARIS("solaris"),
		WINDOWS("windows") {
			@Override
			protected String[] getURIOpenCommand(URI uri) {
				return new String[]{"rundll32", "url.dll,FileProtocolHandler", uri.toString()};
			}
		},
		OSX("mac") {
			@Override
			protected String[] getURIOpenCommand(URI uri) {
				return new String[]{"open", uri.toString()};
			}
		},
		UNKNOWN("unknown");

		private final String name;

		OperatingSystem(final String name) {
			this.name = name;
		}

		/**
		 * Open.
		 *
		 * @param uri uri
		 */
		public void open(URI uri) {
			try {
				Process process = Runtime.getRuntime().exec(this.getURIOpenCommand(uri));
				process.getInputStream().close();
				process.getErrorStream().close();
				process.getOutputStream().close();
			}
			catch (IOException var3) {
				Util.LOGGER.error("Couldn't open location '{}'", uri, var3);
			}
		}

		/**
		 * Open.
		 *
		 * @param file file
		 */
		public void open(File file) {
			this.open(file.toURI());
		}

		/**
		 * Open.
		 *
		 * @param path path
		 */
		public void open(Path path) {
			this.open(path.toUri());
		}

		protected String[] getURIOpenCommand(URI uri) {
			String string = uri.toString();
			if ("file".equals(uri.getScheme())) {
				string = string.replace("file:", "file://");
			}

			return new String[]{"xdg-open", string};
		}

		/**
		 * Open.
		 *
		 * @param uri uri
		 */
		public void open(String uri) {
			try {
				this.open(new URI(uri));
			}
			catch (IllegalArgumentException | URISyntaxException var3) {
				Util.LOGGER.error("Couldn't open uri '{}'", uri, var3);
			}
		}

		public String getName() {
			return this.name;
		}
	}
}
