package net.minecraft.util;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Центральный утилитарный класс Minecraft: управление пулами потоков, файловые операции,
 * работа с коллекциями, предикатами, CompletableFuture и DataFixer.
 */
public class Util {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final int MAX_PARALLELISM = 255;
	private static final int BACKUP_ATTEMPTS = 10;
	private static final long BREAKPOINT_DETECTION_THRESHOLD_MS = 500L;
	private static final long QUEUE_POLL_TIMEOUT_MS = 100L;
	private static final int LINEAR_SEARCH_THRESHOLD = 8;
	private static final String MAX_BG_THREADS_PROPERTY = "max.bg.threads";

	private static final NameableExecutor MAIN_WORKER_EXECUTOR = createWorker("Main");
	private static final NameableExecutor IO_WORKER_EXECUTOR = createIoWorker("IO-Worker-", false);
	private static final NameableExecutor DOWNLOAD_WORKER_EXECUTOR = createIoWorker("Download-", true);
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
	private static final Set<String> SUPPORTED_URI_PROTOCOLS = Set.of("http", "https");

	public static final int CPU_COUNT = 8;
	public static final long NANOS_PER_MILLI = 1_000_000L;
	public static TimeSupplier.Nanoseconds nanoTimeSupplier = System::nanoTime;

	public static final Ticker TICKER = new Ticker() {
		public long read() {
			return nanoTimeSupplier.getAsLong();
		}
	};

	public static final java.util.UUID NIL_UUID = new java.util.UUID(0L, 0L);

	public static final FileSystemProvider JAR_FILE_SYSTEM_PROVIDER = FileSystemProvider
			.installedProviders()
			.stream()
			.filter(provider -> provider.getScheme().equalsIgnoreCase("jar"))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No jar file system provider found"));

	private static Consumer<String> missingBreakpointHandler = message -> {};

	public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
		return Collectors.toMap(Entry::getKey, Entry::getValue);
	}

	public static <T> Collector<T, ?, List<T>> toArrayList() {
		return Collectors.toCollection(Lists::newArrayList);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> String getValueAsString(Property<T> property, Object value) {
		return property.name((T) value);
	}

	public static String createTranslationKey(String type, @Nullable Identifier id) {
		return id == null
				? type + ".unregistered_sadface"
				: type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
	}

	public static long getMeasuringTimeMs() {
		return getMeasuringTimeNano() / NANOS_PER_MILLI;
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
		int threadCount = getAvailableBackgroundThreads();

		if (threadCount <= 0) {
			return new NameableExecutor(MoreExecutors.newDirectExecutorService());
		}

		AtomicInteger workerIndex = new AtomicInteger(1);
		ForkJoinPool pool = new ForkJoinPool(
				threadCount,
				forkJoinPool -> {
					String threadName = "Worker-" + name + "-" + workerIndex.getAndIncrement();
					ForkJoinWorkerThread thread = new ForkJoinWorkerThread(forkJoinPool) {
						@Override
						protected void onStart() {
							TracyClient.setThreadName(threadName, name.hashCode());
							super.onStart();
						}

						@Override
						protected void onTermination(@Nullable Throwable throwable) {
							if (throwable != null) {
								LOGGER.warn("{} died", getName(), throwable);
							}
							else {
								LOGGER.debug("{} shutdown", getName());
							}

							super.onTermination(throwable);
						}
					};
					thread.setName(threadName);
					return thread;
				},
				Util::uncaughtExceptionHandler,
				true
		);

		return new NameableExecutor(pool);
	}

	public static int getAvailableBackgroundThreads() {
		return MathHelper.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxBackgroundThreads());
	}

	private static int getMaxBackgroundThreads() {
		String property = System.getProperty(MAX_BG_THREADS_PROPERTY);

		if (property == null) {
			return MAX_PARALLELISM;
		}

		try {
			int count = Integer.parseInt(property);

			if (count >= 1 && count <= MAX_PARALLELISM) {
				return count;
			}

			LOGGER.error(
					"Wrong {} property value '{}'. Should be an integer value between 1 and {}.",
					new Object[]{MAX_BG_THREADS_PROPERTY, property, MAX_PARALLELISM}
			);
		}
		catch (NumberFormatException exception) {
			LOGGER.error(
					"Could not parse {} property value '{}'. Should be an integer value between 1 and {}.",
					new Object[]{MAX_BG_THREADS_PROPERTY, property, MAX_PARALLELISM}
			);
		}

		return MAX_PARALLELISM;
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

	public static void shutdownExecutors() {
		MAIN_WORKER_EXECUTOR.shutdown(3L, TimeUnit.SECONDS);
		IO_WORKER_EXECUTOR.shutdown(3L, TimeUnit.SECONDS);
	}

	private static NameableExecutor createIoWorker(String namePrefix, boolean daemon) {
		AtomicInteger workerIndex = new AtomicInteger(1);
		return new NameableExecutor(Executors.newCachedThreadPool(runnable -> {
			String threadName = namePrefix + workerIndex.getAndIncrement();
			Thread thread = new Thread(runnable);
			TracyClient.setThreadName(threadName, namePrefix.hashCode());
			thread.setName(threadName);
			thread.setDaemon(daemon);
			thread.setUncaughtExceptionHandler(Util::uncaughtExceptionHandler);
			return thread;
		}));
	}

	public static void throwUnchecked(Throwable t) {
		throw t instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(t);
	}

	private static void uncaughtExceptionHandler(Thread thread, Throwable t) {
		getFatalOrPause(t);

		Throwable cause = t instanceof CompletionException ? t.getCause() : t;

		if (cause instanceof CrashException crashException) {
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
			type = Schemas
					.getFixer()
					.getSchema(DataFixUtils.makeKey(SharedConstants.getGameVersion().dataVersion().id()))
					.getChoiceType(typeReference, id);
		}
		catch (IllegalArgumentException exception) {
			LOGGER.error("No data fixer registered for {}", id);

			if (SharedConstants.isDevelopment) {
				throw exception;
			}
		}

		return type;
	}

	/**
	 * Выполняет {@code runnable} внутри именованной зоны профилировщика Tracy.
	 * В режиме разработки дополнительно переименовывает текущий поток на время выполнения.
	 *
	 * @param runnable задача для выполнения
	 * @param name     имя зоны профилировщика
	 */
	public static void runInNamedZone(Runnable runnable, String name) {
		if (SharedConstants.isDevelopment) {
			Thread thread = Thread.currentThread();
			String originalName = thread.getName();
			thread.setName(name);

			try (Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment)) {
				runnable.run();
			}
			finally {
				thread.setName(originalName);
			}
		}
		else {
			try (Zone zone = TracyClient.beginZone(name, SharedConstants.isDevelopment)) {
				runnable.run();
			}
		}
	}

	public static <T> String registryValueToString(Registry<T> registry, T value) {
		Identifier identifier = registry.getId(value);
		return identifier == null ? "[unregistered]" : identifier.toString();
	}

	public static <T> Predicate<T> and() {
		return o -> true;
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> and(Predicate<? super T> a) {
		return (Predicate<T>) a;
	}

	public static <T> Predicate<T> and(Predicate<? super T> a, Predicate<? super T> b) {
		return o -> a.test(o) && b.test(o);
	}

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

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> predicates) {
		return switch (predicates.size()) {
			case 0 -> and();
			case 1 -> and((Predicate<? super T>) predicates.get(0));
			case 2 -> and(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1)
			);
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
				@SuppressWarnings("unchecked")
				Predicate<? super T>[] array = predicates.toArray(Predicate[]::new);
				yield and(array);
			}
		};
	}

	public static <T> Predicate<T> or() {
		return o -> false;
	}

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> or(Predicate<? super T> a) {
		return (Predicate<T>) a;
	}

	public static <T> Predicate<T> or(Predicate<? super T> a, Predicate<? super T> b) {
		return o -> a.test(o) || b.test(o);
	}

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

	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> anyOf(List<? extends Predicate<? super T>> predicates) {
		return switch (predicates.size()) {
			case 0 -> or();
			case 1 -> or((Predicate<? super T>) predicates.get(0));
			case 2 -> or(
					(Predicate<? super T>) predicates.get(0),
					(Predicate<? super T>) predicates.get(1)
			);
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
				@SuppressWarnings("unchecked")
				Predicate<? super T>[] array = predicates.toArray(Predicate[]::new);
				yield or(array);
			}
		};
	}

	public static <T> boolean isSymmetrical(int width, int height, List<T> list) {
		if (width == 1) {
			return true;
		}

		int halfWidth = width / 2;

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < halfWidth; col++) {
				int mirrorCol = width - 1 - col;
				T left = list.get(col + row * width);
				T right = list.get(mirrorCol + row * width);

				if (!left.equals(right)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Вычисляет следующую ёмкость буфера: увеличивает на 50%, но не менее {@code min}.
	 * Ограничен значением {@code Integer.MAX_VALUE - 8} для совместимости с массивами JVM.
	 */
	public static int nextCapacity(int current, int min) {
		return (int) Math.max(Math.min((long) current + (current >> 1), 2_147_483_639L), (long) min);
	}

	@SuppressLinter(reason = "Intentional use of default locale for user-visible date")
	public static DateTimeFormatter getDefaultLocaleFormatter(FormatStyle style) {
		return DateTimeFormatter.ofLocalizedDateTime(style);
	}

	public static OperatingSystem getOperatingSystem() {
		String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

		if (osName.contains("win")) {
			return OperatingSystem.WINDOWS;
		}

		if (osName.contains("mac")) {
			return OperatingSystem.OSX;
		}

		if (osName.contains("solaris") || osName.contains("sunos")) {
			return OperatingSystem.SOLARIS;
		}

		if (osName.contains("linux") || osName.contains("unix")) {
			return OperatingSystem.LINUX;
		}

		return OperatingSystem.UNKNOWN;
	}

	public static boolean isOnAarch64() {
		return System.getProperty("os.arch").toLowerCase(Locale.ROOT).equals("aarch64");
	}

	/**
	 * Валидирует URI: проверяет наличие схемы и её принадлежность к разрешённым протоколам
	 * ({@code http}, {@code https}).
	 *
	 * @param uri строка URI
	 * @return разобранный {@link URI}
	 * @throws URISyntaxException если URI некорректен или использует неподдерживаемый протокол
	 */
	public static URI validateUri(String uri) throws URISyntaxException {
		URI parsed = new URI(uri);
		String scheme = parsed.getScheme();

		if (scheme == null) {
			throw new URISyntaxException(uri, "Missing protocol in URI: " + uri);
		}

		if (!SUPPORTED_URI_PROTOCOLS.contains(scheme.toLowerCase(Locale.ROOT))) {
			throw new URISyntaxException(uri, "Unsupported protocol in URI: " + uri);
		}

		return parsed;
	}

	/**
	 * Возвращает элемент, следующий за {@code object} в итерируемой последовательности.
	 * Если {@code object} — последний элемент или {@code null}, возвращает первый элемент.
	 */
	public static <T> T next(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T first = iterator.next();

		if (object == null) {
			return first;
		}

		T current = first;

		while (current != object) {
			if (!iterator.hasNext()) {
				return first;
			}

			current = iterator.next();
		}

		return iterator.hasNext() ? iterator.next() : first;
	}

	/**
	 * Возвращает элемент, предшествующий {@code object} в итерируемой последовательности.
	 * Если {@code object} — первый элемент, возвращает последний.
	 */
	public static <T> T previous(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T previous = null;

		while (iterator.hasNext()) {
			T current = iterator.next();

			if (current == object) {
				if (previous == null) {
					@SuppressWarnings("unchecked")
					T last = (T) (iterator.hasNext() ? Iterators.getLast(iterator) : object);
					previous = last;
				}

				break;
			}

			previous = current;
		}

		return previous;
	}

	public static <T> T make(Supplier<T> factory) {
		return factory.get();
	}

	public static <T> T make(T object, Consumer<? super T> initializer) {
		initializer.accept(object);
		return object;
	}

	@SuppressWarnings("unchecked")
	public static <K extends Enum<K>, V> Map<K, V> mapEnum(Class<K> enumClass, Function<K, V> mapper) {
		EnumMap<K, V> enumMap = new EnumMap<>(enumClass);

		for (K constant : (K[]) enumClass.getEnumConstants()) {
			enumMap.put(constant, mapper.apply(constant));
		}

		return enumMap;
	}

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

	public static <V> CompletableFuture<List<V>> combineSafe(List<? extends CompletableFuture<V>> futures) {
		if (futures.isEmpty()) {
			return CompletableFuture.completedFuture(List.of());
		}

		if (futures.size() == 1) {
			return futures.getFirst().thenApply(ObjectLists::singleton);
		}

		CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		return allDone.thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
	}

	public static <V> CompletableFuture<List<V>> combine(List<? extends CompletableFuture<? extends V>> futures) {
		CompletableFuture<List<V>> result = new CompletableFuture<>();
		return combine(futures, result::completeExceptionally).applyToEither(result, Function.identity());
	}

	public static <V> CompletableFuture<List<V>> combineCancellable(
			List<? extends CompletableFuture<? extends V>> futures
	) {
		CompletableFuture<List<V>> result = new CompletableFuture<>();
		return combine(
				futures,
				throwable -> {
					if (result.completeExceptionally(throwable)) {
						for (CompletableFuture<? extends V> future : futures) {
							future.cancel(true);
						}
					}
				}
		).applyToEither(result, Function.identity());
	}

	private static <V> CompletableFuture<List<V>> combine(
			List<? extends CompletableFuture<? extends V>> futures,
			Consumer<Throwable> exceptionHandler
	) {
		ObjectArrayList<V> results = new ObjectArrayList<>();
		results.size(futures.size());
		CompletableFuture<?>[] completableFutures = new CompletableFuture[futures.size()];

		for (int index = 0; index < futures.size(); index++) {
			int finalIndex = index;
			completableFutures[index] = futures.get(index).whenComplete((value, throwable) -> {
				if (throwable != null) {
					exceptionHandler.accept(throwable);
				}
				else {
					results.set(finalIndex, value);
				}
			});
		}

		return CompletableFuture.allOf(completableFutures).thenApply(ignored -> results);
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

	public static <T> Supplier<T> debugSupplier(Supplier<T> supplier, Supplier<String> messageSupplier) {
		if (!SharedConstants.NAMED_RUNNABLES) {
			return supplier;
		}

		String message = messageSupplier.get();
		return new Supplier<>() {
			@Override
			public T get() {
				return supplier.get();
			}

			@Override
			public String toString() {
				return message;
			}
		};
	}

	public static Runnable debugRunnable(Runnable runnable, Supplier<String> messageSupplier) {
		if (!SharedConstants.NAMED_RUNNABLES) {
			return runnable;
		}

		String message = messageSupplier.get();
		return new Runnable() {
			@Override
			public void run() {
				runnable.run();
			}

			@Override
			public String toString() {
				return message;
			}
		};
	}

	public static void logErrorOrPause(String message) {
		LOGGER.error(message);

		if (SharedConstants.isDevelopment) {
			pause(message);
		}
	}

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

	public static void setMissingBreakpointHandler(Consumer<String> handler) {
		missingBreakpointHandler = handler;
	}

	private static void pause(String message) {
		Instant before = Instant.now();
		LOGGER.warn("Did you remember to set a breakpoint here?");
		boolean debuggerAttached = Duration.between(before, Instant.now()).toMillis() > BREAKPOINT_DETECTION_THRESHOLD_MS;

		if (!debuggerAttached) {
			missingBreakpointHandler.accept(message);
		}
	}

	public static String getInnermostMessage(Throwable t) {
		if (t.getCause() != null) {
			return getInnermostMessage(t.getCause());
		}

		return t.getMessage() != null ? t.getMessage() : t.toString();
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
				catch (IOException exception) {
					LOGGER.error("Failed to rename", exception);
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
				catch (IOException exception) {
					LOGGER.warn("Failed to delete", exception);
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
		for (BooleanSupplier task : tasks) {
			if (!task.getAsBoolean()) {
				LOGGER.warn("Failed to execute {}", task);
				return false;
			}
		}

		return true;
	}

	private static boolean attemptTasks(int retries, String taskName, BooleanSupplier... tasks) {
		for (int attempt = 0; attempt < retries; attempt++) {
			if (attemptTasks(tasks)) {
				return true;
			}

			LOGGER.error("Failed to {}, retrying {}/{}", new Object[]{taskName, attempt, retries});
		}

		LOGGER.error("Failed to {}, aborting, progress might be lost", taskName);
		return false;
	}

	public static void backupAndReplace(Path current, Path newPath, Path backup) {
		backupAndReplace(current, newPath, backup, false);
	}

	/**
	 * Атомарно заменяет файл {@code current} файлом {@code newPath}, предварительно создавая резервную копию.
	 * При неудаче восстанавливает оригинал из резервной копии (если {@code noRestoreOnFail} равен {@code false}).
	 *
	 * @param current        текущий файл, который нужно заменить
	 * @param newPath        новый файл, которым заменяется текущий
	 * @param backup         путь для резервной копии текущего файла
	 * @param noRestoreOnFail если {@code true}, не восстанавливать оригинал при неудаче замены
	 * @return {@code true} при успешной замене
	 */
	public static boolean backupAndReplace(Path current, Path newPath, Path backup, boolean noRestoreOnFail) {
		if (Files.exists(current) && !attemptTasks(
				BACKUP_ATTEMPTS,
				"create backup " + backup,
				deleteTask(backup),
				renameTask(current, backup),
				existenceCheckTask(backup)
		)) {
			return false;
		}

		if (!attemptTasks(BACKUP_ATTEMPTS, "remove old " + current, deleteTask(current), deletionVerifyTask(current))) {
			return false;
		}

		if (!attemptTasks(
				BACKUP_ATTEMPTS,
				"replace " + current + " with " + newPath,
				renameTask(newPath, current),
				existenceCheckTask(current)
		)) {
			if (!noRestoreOnFail) {
				attemptTasks(
						BACKUP_ATTEMPTS,
						"restore " + current + " from " + backup,
						renameTask(backup, current),
						existenceCheckTask(current)
				);
			}

			return false;
		}

		return true;
	}

	/**
	 * Перемещает курсор в строке на {@code delta} кодовых точек Unicode,
	 * корректно обрабатывая суррогатные пары.
	 *
	 * @param string строка
	 * @param cursor текущая позиция курсора (индекс char)
	 * @param delta  количество кодовых точек для перемещения (положительное — вперёд, отрицательное — назад)
	 * @return новая позиция курсора
	 */
	public static int moveCursor(String string, int cursor, int delta) {
		int length = string.length();

		if (delta >= 0) {
			for (int step = 0; cursor < length && step < delta; step++) {
				if (Character.isHighSurrogate(string.charAt(cursor++)) && cursor < length
						&& Character.isLowSurrogate(string.charAt(cursor))) {
					cursor++;
				}
			}
		}
		else {
			for (int step = delta; cursor > 0 && step < 0; step++) {
				cursor--;

				if (Character.isLowSurrogate(string.charAt(cursor)) && cursor > 0
						&& Character.isHighSurrogate(string.charAt(cursor - 1))) {
					cursor--;
				}
			}
		}

		return cursor;
	}

	public static Consumer<String> addPrefix(String prefix, Consumer<String> consumer) {
		return string -> consumer.accept(prefix + string);
	}

	/**
	 * Декодирует поток целых чисел в массив фиксированной длины.
	 * Возвращает ошибку, если длина потока не совпадает с {@code length}.
	 */
	public static DataResult<int[]> decodeFixedLengthIntArray(IntStream stream, int length) {
		int[] array = stream.limit(length + 1L).toArray();

		if (array.length != length) {
			Supplier<String> errorMessage = () -> "Input is not a list of " + length + " ints";
			return array.length >= length
					? DataResult.error(errorMessage, Arrays.copyOf(array, length))
					: DataResult.error(errorMessage);
		}

		return DataResult.success(array);
	}

	/**
	 * Декодирует поток длинных целых чисел в массив фиксированной длины.
	 * Возвращает ошибку, если длина потока не совпадает с {@code length}.
	 */
	public static DataResult<long[]> decodeFixedLengthArray(LongStream stream, int length) {
		long[] array = stream.limit(length + 1L).toArray();

		if (array.length != length) {
			Supplier<String> errorMessage = () -> "Input is not a list of " + length + " longs";
			return array.length >= length
					? DataResult.error(errorMessage, Arrays.copyOf(array, length))
					: DataResult.error(errorMessage);
		}

		return DataResult.success(array);
	}

	/**
	 * Проверяет, что список содержит ровно {@code length} элементов.
	 * Возвращает ошибку с усечённым или исходным списком при несоответствии.
	 */
	public static <T> DataResult<List<T>> decodeFixedLengthList(List<T> list, int length) {
		if (list.size() != length) {
			Supplier<String> errorMessage = () -> "Input is not a list of " + length + " elements";
			return list.size() >= length
					? DataResult.error(errorMessage, list.subList(0, length))
					: DataResult.error(errorMessage);
		}

		return DataResult.success(list);
	}

	/**
	 * Запускает фоновый поток-демон, предотвращающий деградацию точности системного таймера на Windows.
	 * Поток спит максимально долго, не давая JVM перейти в режим пониженной точности таймера.
	 */
	public static void startTimerHack() {
		Thread thread = new Thread("Timer hack thread") {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					}
					catch (InterruptedException exception) {
						LOGGER.warn("Timer hack thread interrupted, that really should not happen");
						return;
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
		thread.start();
	}

	public static void relativeCopy(Path src, Path dest, Path toCopy) throws IOException {
		Path relative = src.relativize(toCopy);
		Path target = dest.resolve(relative);
		Files.copy(toCopy, target);
	}

	public static String replaceInvalidChars(String string, CharPredicate predicate) {
		return string
				.toLowerCase(Locale.ROOT)
				.chars()
				.mapToObj(charCode -> predicate.test((char) charCode) ? Character.toString((char) charCode) : "_")
				.collect(Collectors.joining());
	}

	public static <K, V> CachedMapper<K, V> cachedMapper(Function<K, V> mapper) {
		return new CachedMapper<>(mapper);
	}

	/**
	 * Создаёт мемоизированную функцию с потокобезопасным кэшем на основе {@link java.util.concurrent.ConcurrentHashMap}.
	 *
	 * @param function исходная функция
	 * @return мемоизированная функция
	 */
	public static <T, R> Function<T, R> memoize(Function<T, R> function) {
		return new Function<>() {
			private final Map<T, R> cache = new java.util.concurrent.ConcurrentHashMap<>();

			@Override
			public R apply(T object) {
				return cache.computeIfAbsent(object, function);
			}

			@Override
			public String toString() {
				return "memoize/1[function=" + function + ", size=" + cache.size() + "]";
			}
		};
	}

	/**
	 * Создаёт мемоизированную двухаргументную функцию с потокобезопасным кэшем.
	 *
	 * @param biFunction исходная функция
	 * @return мемоизированная функция
	 */
	public static <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> biFunction) {
		return new BiFunction<>() {
			private final Map<Pair<T, U>, R> cache = new java.util.concurrent.ConcurrentHashMap<>();

			@Override
			@SuppressWarnings("unchecked")
			public R apply(T a, U b) {
				return cache.computeIfAbsent(
						Pair.of(a, b),
						pair -> biFunction.apply((T) pair.getFirst(), (U) pair.getSecond())
				);
			}

			@Override
			public String toString() {
				return "memoize/2[function=" + biFunction + ", size=" + cache.size() + "]";
			}
		};
	}

	public static <T> List<T> copyShuffled(Stream<T> stream, Random random) {
		ObjectArrayList<T> list = stream.collect(ObjectArrayList.toList());
		shuffle(list, random);
		return list;
	}

	public static IntArrayList shuffle(IntStream stream, Random random) {
		IntArrayList list = IntArrayList.wrap(stream.toArray());
		int size = list.size();

		for (int remaining = size; remaining > 1; remaining--) {
			int swapIndex = random.nextInt(remaining);
			list.set(remaining - 1, list.set(swapIndex, list.getInt(remaining - 1)));
		}

		return list;
	}

	public static <T> List<T> copyShuffled(T[] array, Random random) {
		ObjectArrayList<T> list = new ObjectArrayList<>(array);
		shuffle(list, random);
		return list;
	}

	public static <T> List<T> copyShuffled(ObjectArrayList<T> source, Random random) {
		ObjectArrayList<T> list = new ObjectArrayList<>(source);
		shuffle(list, random);
		return list;
	}

	public static <T> void shuffle(List<T> list, Random random) {
		int size = list.size();

		for (int remaining = size; remaining > 1; remaining--) {
			int swapIndex = random.nextInt(remaining);
			list.set(remaining - 1, list.set(swapIndex, list.get(remaining - 1)));
		}
	}

	public static <T> CompletableFuture<T> waitAndApply(Function<Executor, CompletableFuture<T>> resultFactory) {
		return waitAndApply(resultFactory, CompletableFuture::isDone);
	}

	/**
	 * Синхронно ожидает завершения задачи, выполняя задачи из очереди в текущем потоке.
	 * Используется для интеграции асинхронного кода в синхронный контекст.
	 *
	 * @param resultFactory фабрика, принимающая executor и возвращающая результат
	 * @param donePredicate предикат завершения
	 * @return результат выполнения
	 */
	public static <T> T waitAndApply(Function<Executor, T> resultFactory, Predicate<T> donePredicate) {
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		T result = resultFactory.apply(queue::add);

		while (!donePredicate.test(result)) {
			try {
				Runnable task = queue.poll(QUEUE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);

				if (task != null) {
					task.run();
				}
			}
			catch (InterruptedException exception) {
				LOGGER.warn("Interrupted wait");
				break;
			}
		}

		int remaining = queue.size();

		if (remaining > 0) {
			LOGGER.warn("Tasks left in queue: {}", remaining);
		}

		return result;
	}

	/**
	 * Создаёт функцию поиска последнего индекса элемента в списке.
	 * При размере списка менее {@value #LINEAR_SEARCH_THRESHOLD} использует линейный поиск,
	 * иначе строит хэш-карту для O(1) доступа.
	 */
	public static <T> ToIntFunction<T> lastIndexGetter(List<T> values) {
		int size = values.size();

		if (size < LINEAR_SEARCH_THRESHOLD) {
			return values::indexOf;
		}

		Object2IntMap<T> indexMap = new Object2IntOpenHashMap<>(size);
		indexMap.defaultReturnValue(-1);

		for (int index = 0; index < size; index++) {
			indexMap.put(values.get(index), index);
		}

		return indexMap;
	}

	/**
	 * Аналог {@link #lastIndexGetter}, но использует сравнение по идентичности объектов (==),
	 * а не по равенству ({@link Object#equals}).
	 */
	public static <T> ToIntFunction<T> lastIdentityIndexGetter(List<T> values) {
		int size = values.size();

		if (size < LINEAR_SEARCH_THRESHOLD) {
			ReferenceList<T> referenceList = new ReferenceImmutableList<>(values);
			return referenceList::indexOf;
		}

		Reference2IntMap<T> indexMap = new Reference2IntOpenHashMap<>(size);
		indexMap.defaultReturnValue(-1);

		for (int index = 0; index < size; index++) {
			indexMap.put(values.get(index), index);
		}

		return indexMap;
	}

	@SuppressWarnings("unchecked")
	public static <A, B> Typed<B> apply(Typed<A> typed, Type<B> type, UnaryOperator<Dynamic<?>> modifier) {
		Dynamic<?> dynamic = (Dynamic<?>) typed.write().getOrThrow();
		return readTyped(type, modifier.apply(dynamic), true);
	}

	public static <T> Typed<T> readTyped(Type<T> type, Dynamic<?> value) {
		return readTyped(type, value, false);
	}

	/**
	 * Читает типизированное значение из {@link Dynamic}, оборачивая ошибки в {@link CrashException}.
	 *
	 * @param type         целевой тип DataFixer
	 * @param value        динамическое значение для чтения
	 * @param allowPartial если {@code true}, допускает частичное чтение при ошибках
	 * @return типизированное значение
	 */
	@SuppressWarnings("unchecked")
	public static <T> Typed<T> readTyped(Type<T> type, Dynamic<?> value, boolean allowPartial) {
		DataResult<Typed<T>> dataResult = type.readTyped(value).map(Pair::getFirst);

		try {
			return allowPartial
					? (Typed<T>) dataResult.getPartialOrThrow(IllegalStateException::new)
					: (Typed<T>) dataResult.getOrThrow(IllegalStateException::new);
		}
		catch (IllegalStateException exception) {
			CrashReport crashReport = CrashReport.create(exception, "Reading type");
			CrashReportSection section = crashReport.addElement("Info");
			section.add("Data", value);
			section.add("Type", type);
			throw new CrashException(crashReport);
		}
	}

	public static <T> List<T> withAppended(List<T> list, T valueToAppend) {
		return ImmutableList.<T>builderWithExpectedSize(list.size() + 1).addAll(list).add(valueToAppend).build();
	}

	public static <T> List<T> withPrepended(T valueToPrepend, List<T> list) {
		return ImmutableList.<T>builderWithExpectedSize(list.size() + 1).add(valueToPrepend).addAll(list).build();
	}

	public static <K, V> Map<K, V> mapWith(Map<K, V> map, K keyToAppend, V valueToAppend) {
		return ImmutableMap
				.<K, V>builderWithExpectedSize(map.size() + 1)
				.putAll(map)
				.put(keyToAppend, valueToAppend)
				.buildKeepingLast();
	}

	public enum OperatingSystem {
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

		OperatingSystem(String name) {
			this.name = name;
		}

		public void open(URI uri) {
			try {
				Process process = Runtime.getRuntime().exec(getURIOpenCommand(uri));
				process.getInputStream().close();
				process.getErrorStream().close();
				process.getOutputStream().close();
			}
			catch (IOException exception) {
				LOGGER.error("Couldn't open location '{}'", uri, exception);
			}
		}

		public void open(File file) {
			open(file.toURI());
		}

		public void open(Path path) {
			open(path.toUri());
		}

		protected String[] getURIOpenCommand(URI uri) {
			String uriString = uri.toString();

			if ("file".equals(uri.getScheme())) {
				uriString = uriString.replace("file:", "file://");
			}

			return new String[]{"xdg-open", uriString};
		}

		public void open(String uri) {
			try {
				open(new URI(uri));
			}
			catch (IllegalArgumentException | URISyntaxException exception) {
				LOGGER.error("Couldn't open uri '{}'", uri, exception);
			}
		}

		public String getName() {
			return name;
		}
	}
}
