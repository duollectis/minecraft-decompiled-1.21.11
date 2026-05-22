package net.minecraft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.MinecraftVersion;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.metadata.PackFeatureSetMetadata;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Провайдер данных для генерации файла {@code pack.mcmeta}.
 * Позволяет добавлять произвольные секции метаданных через {@link #add}.
 */
public class MetadataProvider implements DataProvider {

	private final DataOutput output;
	private final Map<String, Supplier<JsonElement>> metadata = new HashMap<>();

	public MetadataProvider(DataOutput output) {
		this.output = output;
	}

	public <T> MetadataProvider add(ResourceMetadataSerializer<T> serializer, T value) {
		metadata.put(
				serializer.name(),
				() -> serializer
						.codec()
						.encodeStart(JsonOps.INSTANCE, value)
						.getOrThrow(IllegalArgumentException::new)
						.getAsJsonObject()
		);
		return this;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		JsonObject jsonObject = new JsonObject();
		metadata.forEach((key, jsonSupplier) -> jsonObject.add(key, jsonSupplier.get()));
		return DataProvider.writeToPath(writer, jsonObject, output.getPath().resolve("pack.mcmeta"));
	}

	@Override
	public String getName() {
		return "Pack Metadata";
	}

	public static MetadataProvider create(DataOutput output, Text description) {
		return new MetadataProvider(output).add(
				PackResourceMetadata.SERVER_DATA_SERIALIZER,
				new PackResourceMetadata(
						description,
						MinecraftVersion.DEVELOPMENT.packVersion(ResourceType.SERVER_DATA).majorRange()
				)
		);
	}

	public static MetadataProvider create(DataOutput output, Text description, FeatureSet requiredFeatures) {
		return create(output, description).add(
				PackFeatureSetMetadata.SERIALIZER,
				new PackFeatureSetMetadata(requiredFeatures)
		);
	}
}
