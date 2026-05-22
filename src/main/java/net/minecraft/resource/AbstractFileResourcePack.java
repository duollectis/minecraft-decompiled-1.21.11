package net.minecraft.resource;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Базовая реализация {@link ResourcePack} для паков, хранящихся в файловой системе.
 * Предоставляет общую логику чтения и разбора метаданных из файла {@code pack.mcmeta}.
 */
public abstract class AbstractFileResourcePack implements ResourcePack {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final ResourcePackInfo info;

	protected AbstractFileResourcePack(ResourcePackInfo info) {
		this.info = info;
	}

	@Override
	public <T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException {
		InputSupplier<InputStream> inputSupplier = openRoot("pack.mcmeta");
		if (inputSupplier == null) {
			return null;
		}

		try (InputStream inputStream = inputSupplier.get()) {
			return parseMetadata(metadataSerializer, inputStream, info);
		}
	}

	/**
	 * Разбирает метаданные пака из входного потока.
	 * Читает JSON из потока и декодирует секцию, соответствующую имени сериализатора.
	 *
	 * @param metadataSerializer сериализатор нужной секции метаданных
	 * @param inputStream        поток с содержимым файла {@code pack.mcmeta}
	 * @param packInfo           информация о паке (используется для логирования ошибок)
	 * @return декодированный объект метаданных или {@code null} при ошибке / отсутствии секции
	 */
	public static <T> @Nullable T parseMetadata(
		ResourceMetadataSerializer<T> metadataSerializer,
		InputStream inputStream,
		ResourcePackInfo packInfo
	) {
		JsonObject jsonObject;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			jsonObject = JsonHelper.deserialize(reader);
		} catch (Exception exception) {
			LOGGER.error(
				"Couldn't load {} {} metadata: {}",
				packInfo.id(), metadataSerializer.name(), exception.getMessage()
			);
			return null;
		}

		if (!jsonObject.has(metadataSerializer.name())) {
			return null;
		}

		return metadataSerializer.codec()
			.parse(JsonOps.INSTANCE, jsonObject.get(metadataSerializer.name()))
			.ifError(error -> LOGGER.error(
				"Couldn't load {} {} metadata: {}",
				packInfo.id(), metadataSerializer.name(), error.message()
			))
			.result()
			.orElse(null);
	}

	@Override
	public ResourcePackInfo getInfo() {
		return info;
	}
}
