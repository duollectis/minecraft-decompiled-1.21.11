package net.minecraft.data.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockTypes;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Провайдер данных для генерации отчёта {@code reports/blocks.json}.
 * Содержит список всех блоков с их свойствами, состояниями и определениями типов.
 */
public class BlockListProvider implements DataProvider {

	private final DataOutput output;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public BlockListProvider(DataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		this.output = output;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Path path = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("blocks.json");
		return registriesFuture.thenCompose(registries -> {
			JsonObject blocksJson = new JsonObject();
			RegistryOps<JsonElement> registryOps = registries.getOps(JsonOps.INSTANCE);

			registries.getOrThrow(RegistryKeys.BLOCK).streamEntries().forEach(entry -> {
				JsonObject blockJson = new JsonObject();
				StateManager<Block, BlockState> stateManager = entry.value().getStateManager();

				if (!stateManager.getProperties().isEmpty()) {
					JsonObject propertiesJson = new JsonObject();

					for (Property<?> property : stateManager.getProperties()) {
						JsonArray valuesArray = new JsonArray();

						for (Comparable<?> value : property.getValues()) {
							valuesArray.add(Util.getValueAsString(property, value));
						}

						propertiesJson.add(property.getName(), valuesArray);
					}

					blockJson.add("properties", propertiesJson);
				}

				JsonArray statesArray = new JsonArray();

				for (BlockState blockState : stateManager.getStates()) {
					JsonObject stateJson = new JsonObject();
					JsonObject statePropertiesJson = new JsonObject();

					for (Property<?> property : stateManager.getProperties()) {
						statePropertiesJson.addProperty(
								property.getName(),
								Util.getValueAsString(property, blockState.get(property))
						);
					}

					if (!statePropertiesJson.isEmpty()) {
						stateJson.add("properties", statePropertiesJson);
					}

					stateJson.addProperty("id", Block.getRawIdFromState(blockState));

					if (blockState == entry.value().getDefaultState()) {
						stateJson.addProperty("default", true);
					}

					statesArray.add(stateJson);
				}

				blockJson.add("states", statesArray);

				String blockId = entry.getIdAsString();
				JsonElement definitionJson = BlockTypes.CODEC
						.codec()
						.encodeStart(registryOps, entry.value())
						.getOrThrow(error -> new AssertionError(
								"Failed to serialize block " + blockId
										+ " (is type registered in BlockTypes?): " + error
						));

				blockJson.add("definition", definitionJson);
				blocksJson.add(blockId, blockJson);
			});

			return DataProvider.writeToPath(writer, blocksJson, path);
		});
	}

	@Override
	public String getName() {
		return "Block List";
	}
}
