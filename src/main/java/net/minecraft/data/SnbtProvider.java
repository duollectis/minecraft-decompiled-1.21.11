package net.minecraft.data;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Провайдер данных для конвертации SNBT-файлов в сжатые NBT-файлы.
 * Рекурсивно обходит входные директории, находит файлы {@code *.snbt},
 * применяет зарегистрированные {@link Tweaker}-ы и записывает результат как {@code *.nbt}.
 */
public class SnbtProvider implements DataProvider {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final DataOutput output;
	private final Iterable<Path> paths;
	private final List<Tweaker> tweakers = Lists.newArrayList();

	public SnbtProvider(DataOutput output, Iterable<Path> paths) {
		this.output = output;
		this.paths = paths;
	}

	public SnbtProvider addWriter(Tweaker tweaker) {
		tweakers.add(tweaker);
		return this;
	}

	private NbtCompound applyTweakers(String key, NbtCompound compound) {
		NbtCompound result = compound;

		for (Tweaker tweaker : tweakers) {
			result = tweaker.write(key, result);
		}

		return result;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Path outputPath = output.getPath();
		List<CompletableFuture<?>> futures = Lists.newArrayList();

		for (Path inputPath : paths) {
			futures.add(
					CompletableFuture.<CompletableFuture<?>>supplyAsync(
							() -> {
								try (Stream<Path> stream = Files.walk(inputPath)) {
									return CompletableFuture.allOf(
											stream
													.filter(file -> file.toString().endsWith(".snbt"))
													.map(file -> CompletableFuture.runAsync(
															() -> {
																CompressedData compressedData = toCompressedNbt(
																		file,
																		getFileName(inputPath, file)
																);
																writeNbt(writer, compressedData, outputPath);
															},
															Util.getMainWorkerExecutor().named("SnbtToNbt")
													))
													.toArray(CompletableFuture[]::new)
									);
								} catch (Exception exception) {
									throw new RuntimeException(
											"Failed to read structure input directory, aborting", exception
									);
								}
							},
							Util.getMainWorkerExecutor().named("SnbtToNbt")
					).thenCompose(future -> future)
			);
		}

		return Util.combine(futures);
	}

	@Override
	public String getName() {
		return "SNBT -> NBT";
	}

	private String getFileName(Path root, Path file) {
		String relativePath = root.relativize(file).toString().replaceAll("\\\\", "/");
		return relativePath.substring(0, relativePath.length() - ".snbt".length());
	}

	private CompressedData toCompressedNbt(Path path, String name) {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String snbtContent = IOUtils.toString(reader);
			NbtCompound nbtCompound = applyTweakers(name, NbtHelper.fromNbtProviderString(snbtContent));
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			HashingOutputStream hashingOutput = new HashingOutputStream(Hashing.sha1(), byteOutput);
			NbtIo.writeCompressed(nbtCompound, hashingOutput);
			byte[] bytes = byteOutput.toByteArray();
			HashCode hashCode = hashingOutput.hash();
			return new CompressedData(name, bytes, hashCode);
		} catch (Throwable throwable) {
			throw new CompressionException(path, throwable);
		}
	}

	private void writeNbt(DataWriter writer, CompressedData data, Path root) {
		Path outputPath = root.resolve(data.name + ".nbt");

		try {
			writer.write(outputPath, data.bytes, data.sha1);
		} catch (IOException exception) {
			LOGGER.error("Couldn't write structure {} at {}", data.name, outputPath, exception);
		}
	}

	record CompressedData(String name, byte[] bytes, HashCode sha1) {
	}

	static class CompressionException extends RuntimeException {

		public CompressionException(Path path, Throwable cause) {
			super(path.toAbsolutePath().toString(), cause);
		}
	}

	/**
	 * Функциональный интерфейс для постобработки NBT-данных перед записью.
	 * Позволяет модифицировать или валидировать структуры (например, обновлять DataVersion).
	 */
	@FunctionalInterface
	public interface Tweaker {

		NbtCompound write(String name, NbtCompound nbt);
	}
}
