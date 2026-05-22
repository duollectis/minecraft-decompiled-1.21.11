package net.minecraft.data.dev;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.FixedBufferInputStream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Провайдер данных для конвертации NBT-файлов в SNBT (текстовый формат).
 * Используется в режиме разработки для инспекции структур и других NBT-данных.
 */
public class NbtProvider implements DataProvider {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final byte NEWLINE_BYTE = '\n';

	private final Iterable<Path> paths;
	private final DataOutput output;

	public NbtProvider(DataOutput output, Collection<Path> paths) {
		this.paths = paths;
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Path outputPath = output.getPath();
		List<CompletableFuture<?>> futures = new ArrayList<>();

		for (Path inputPath : paths) {
			futures.add(
					CompletableFuture.<CompletableFuture<?>>supplyAsync(
							() -> {
								try (Stream<Path> stream = Files.walk(inputPath)) {
									return CompletableFuture.allOf(
											stream
													.filter(file -> file.toString().endsWith(".nbt"))
													.map(file -> CompletableFuture.runAsync(
															() -> convertNbtToSnbt(
																	writer,
																	file,
																	getLocation(inputPath, file),
																	outputPath
															),
															Util.getIoWorkerExecutor()
													))
													.toArray(CompletableFuture[]::new)
									);
								} catch (IOException exception) {
									LOGGER.error("Failed to read structure input directory", exception);
									return CompletableFuture.completedFuture(null);
								}
							},
							Util.getMainWorkerExecutor().named("NbtToSnbt")
					).thenCompose(future -> future)
			);
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	@Override
	public String getName() {
		return "NBT -> SNBT";
	}

	private static String getLocation(Path inputPath, Path filePath) {
		String relativePath = inputPath.relativize(filePath).toString().replaceAll("\\\\", "/");
		return relativePath.substring(0, relativePath.length() - ".nbt".length());
	}

	/**
	 * Конвертирует один NBT-файл в SNBT и записывает результат через {@link DataWriter}.
	 *
	 * @param writer     целевой писатель данных
	 * @param inputPath  путь к исходному {@code .nbt} файлу
	 * @param filename   относительное имя файла без расширения
	 * @param outputPath корневая директория вывода
	 * @return путь к записанному {@code .snbt} файлу, или {@code null} при ошибке
	 */
	public static @Nullable Path convertNbtToSnbt(
			DataWriter writer,
			Path inputPath,
			String filename,
			Path outputPath
	) {
		try (
				InputStream inputStream = Files.newInputStream(inputPath);
				InputStream bufferedStream = new FixedBufferInputStream(inputStream)
		) {
			Path snbtPath = outputPath.resolve(filename + ".snbt");
			writeTo(
					writer,
					snbtPath,
					NbtHelper.toNbtProviderString(NbtIo.readCompressed(bufferedStream, NbtSizeTracker.ofUnlimitedBytes()))
			);
			LOGGER.info("Converted {} from NBT to SNBT", filename);
			return snbtPath;
		} catch (IOException exception) {
			LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", filename, inputPath, exception);
			return null;
		}
	}

	public static void writeTo(DataWriter writer, Path path, String content) throws IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		HashingOutputStream hashingOutput = new HashingOutputStream(Hashing.sha1(), byteOutput);
		hashingOutput.write(content.getBytes(StandardCharsets.UTF_8));
		hashingOutput.write(NEWLINE_BYTE);
		writer.write(path, byteOutput.toByteArray(), hashingOutput.hash());
	}
}
