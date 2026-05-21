package net.minecraft.client.data;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.json.BlockModelDefinition;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Провайдер данных для генерации определений моделей блоков и предметов.
 * Координирует работу генераторов моделей и записывает результаты в ресурс-пак.
 */
@Environment(EnvType.CLIENT)
public class ModelProvider implements DataProvider {

	private final DataOutput.PathResolver blockstatesPathResolver;
	private final DataOutput.PathResolver itemsPathResolver;
	private final DataOutput.PathResolver modelsPathResolver;

	/**
	 * @param output целевой вывод данных для разрешения путей ресурс-пака
	 */
	public ModelProvider(DataOutput output) {
		this.blockstatesPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "blockstates");
		this.itemsPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "items");
		this.modelsPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "models");
	}

	/**
	 * Запускает генерацию всех определений моделей блоков и предметов.
	 *
	 * @param writer писатель данных для сохранения результатов
	 * @return будущее, завершающееся после записи всех файлов
	 */
	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		ItemAssets itemAssetsCollector = new ItemAssets();
		BlockStateSuppliers blockStateSuppliersCollector = new BlockStateSuppliers();
		ModelSuppliers modelSuppliersCollector = new ModelSuppliers();

		new BlockStateModelGenerator(
				blockStateSuppliersCollector,
				itemAssetsCollector,
				modelSuppliersCollector
		).register();
		new ItemModelGenerator(itemAssetsCollector, modelSuppliersCollector).register();

		blockStateSuppliersCollector.validate();
		itemAssetsCollector.resolveAndValidate();

		return CompletableFuture.allOf(
				blockStateSuppliersCollector.writeAllToPath(writer, this.blockstatesPathResolver),
				modelSuppliersCollector.writeAllToPath(writer, this.modelsPathResolver),
				itemAssetsCollector.writeAllToPath(writer, this.itemsPathResolver)
		);
	}

	/**
	 * @return человекочитаемое имя провайдера
	 */
	@Override
	public String getName() {
		return "Model Definitions";
	}

	/**
	 * Коллектор определений состояний блоков.
	 * Принимает {@link BlockModelDefinitionCreator} и обеспечивает уникальность по блоку.
	 */
	@Environment(EnvType.CLIENT)
	static class BlockStateSuppliers implements Consumer<BlockModelDefinitionCreator> {

		private final Map<Block, BlockModelDefinitionCreator> blockStateSuppliers = new HashMap<>();

		/**
		 * Регистрирует создатель определения состояния блока.
		 *
		 * @param blockModelDefinitionCreator создатель определения для конкретного блока
		 * @throws IllegalStateException если определение для блока уже зарегистрировано
		 */
		@Override
		public void accept(BlockModelDefinitionCreator blockModelDefinitionCreator) {
			Block block = blockModelDefinitionCreator.getBlock();
			BlockModelDefinitionCreator existing = this.blockStateSuppliers.put(block, blockModelDefinitionCreator);

			if (existing != null) {
				throw new IllegalStateException("Duplicate blockstate definition for " + block);
			}
		}

		/**
		 * Проверяет, что все зарегистрированные блоки имеют определения состояний.
		 *
		 * @throws IllegalStateException если для каких-либо блоков отсутствуют определения
		 */
		public void validate() {
			Stream<RegistryEntry.Reference<Block>> stream = Registries.BLOCK.streamEntries().filter(entry -> true);
			List<Identifier> missing = stream.filter(entry -> !this.blockStateSuppliers.containsKey(entry.value()))
			                                 .map(entryx -> entryx.registryKey().getValue())
			                                 .toList();

			if (!missing.isEmpty()) {
				throw new IllegalStateException("Missing blockstate definitions for: " + missing);
			}
		}

		/**
		 * Записывает все определения состояний блоков по указанным путям.
		 *
		 * @param writer       писатель данных
		 * @param pathResolver резолвер путей для блокстейтов
		 * @return будущее записи всех файлов
		 */
		public CompletableFuture<?> writeAllToPath(DataWriter writer, DataOutput.PathResolver pathResolver) {
			Map<Block, BlockModelDefinition> definitions = Maps.transformValues(
					this.blockStateSuppliers,
					BlockModelDefinitionCreator::createBlockModelDefinition
			);
			Function<Block, Path>
					pathFunction =
					block -> pathResolver.resolveJson(block.getRegistryEntry().registryKey().getValue());

			return DataProvider.writeAllToPath(writer, BlockModelDefinition.CODEC, pathFunction, definitions);
		}
	}

	/**
	 * Коллектор ассетов моделей предметов.
	 * Реализует {@link ItemModelOutput} и обеспечивает уникальность по предмету.
	 */
	@Environment(EnvType.CLIENT)
	static class ItemAssets implements ItemModelOutput {

		private final Map<Item, ItemAsset> itemAssets = new HashMap<>();
		private final Map<Item, Item> aliasedAssets = new HashMap<>();

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void accept(Item item, ItemModel.Unbaked model, ItemAsset.Properties properties) {
			this.accept(item, new ItemAsset(model, properties));
		}

		/**
		 * Регистрирует ассет предмета.
		 *
		 * @param item  предмет
		 * @param asset ассет модели
		 * @throws IllegalStateException если ассет для предмета уже зарегистрирован
		 */
		private void accept(Item item, ItemAsset asset) {
			ItemAsset previous = this.itemAssets.put(item, asset);

			if (previous != null) {
				throw new IllegalStateException("Duplicate item model definition for " + item);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void acceptAlias(Item base, Item alias) {
			this.aliasedAssets.put(alias, base);
		}

		/**
		 * Разрешает псевдонимы и проверяет полноту регистрации всех предметов.
		 *
		 * @throws IllegalStateException если донор псевдонима не найден или предметы без ассетов
		 */
		public void resolveAndValidate() {
			Registries.ITEM.forEach(item -> {
				if (!this.aliasedAssets.containsKey(item)) {
					if (item instanceof BlockItem blockItem && !this.itemAssets.containsKey(blockItem)) {
						Identifier identifier = ModelIds.getBlockModelId(blockItem.getBlock());
						this.accept(blockItem, ItemModels.basic(identifier));
					}
				}
			});

			this.aliasedAssets.forEach((base, alias) -> {
				ItemAsset donorAsset = this.itemAssets.get(alias);

				if (donorAsset == null) {
					throw new IllegalStateException("Missing donor: " + alias + " -> " + base);
				}
				else {
					this.accept(base, donorAsset);
				}
			});

			List<Identifier> missing = Registries.ITEM
					.streamEntries()
					.filter(entry -> !this.itemAssets.containsKey(entry.value()))
					.map(entryx -> entryx.registryKey().getValue())
					.toList();

			if (!missing.isEmpty()) {
				throw new IllegalStateException("Missing item model definitions for: " + missing);
			}
		}

		/**
		 * Записывает все ассеты предметов по указанным путям.
		 *
		 * @param writer       писатель данных
		 * @param pathResolver резолвер путей для предметов
		 * @return будущее записи всех файлов
		 */
		public CompletableFuture<?> writeAllToPath(DataWriter writer, DataOutput.PathResolver pathResolver) {
			return DataProvider.writeAllToPath(
					writer,
					ItemAsset.CODEC,
					item -> pathResolver.resolveJson(item.getRegistryEntry().registryKey().getValue()),
					this.itemAssets
			);
		}
	}

	/**
	 * Коллектор поставщиков моделей, идентифицированных по {@link Identifier}.
	 * Обеспечивает уникальность регистрации моделей.
	 */
	@Environment(EnvType.CLIENT)
	static class ModelSuppliers implements BiConsumer<Identifier, ModelSupplier> {

		private final Map<Identifier, ModelSupplier> modelSuppliers = new HashMap<>();

		/**
		 * Регистрирует поставщика модели по идентификатору.
		 *
		 * @param identifier    идентификатор модели
		 * @param modelSupplier поставщик JSON-элемента модели
		 * @throws IllegalStateException если модель с таким идентификатором уже зарегистрирована
		 */
		@Override
		public void accept(Identifier identifier, ModelSupplier modelSupplier) {
			Supplier<JsonElement> existing = this.modelSuppliers.put(identifier, modelSupplier);

			if (existing != null) {
				throw new IllegalStateException("Duplicate model definition for " + identifier);
			}
		}

		/**
		 * Записывает все модели по указанным путям.
		 *
		 * @param writer       писатель данных
		 * @param pathResolver резолвер путей для моделей
		 * @return будущее записи всех файлов
		 */
		public CompletableFuture<?> writeAllToPath(DataWriter writer, DataOutput.PathResolver pathResolver) {
			return DataProvider.writeAllToPath(writer, Supplier::get, pathResolver::resolveJson, this.modelSuppliers);
		}
	}
}
