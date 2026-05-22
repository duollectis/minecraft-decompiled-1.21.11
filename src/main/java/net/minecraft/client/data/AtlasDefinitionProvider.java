package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.BellBlockEntityRenderer;
import net.minecraft.client.render.block.entity.ConduitBlockEntityRenderer;
import net.minecraft.client.render.block.entity.EnchantingTableBlockEntityRenderer;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.atlas.*;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.SpriteMapper;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.equipment.trim.ArmorTrimAssets;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.item.equipment.trim.ArmorTrimPatterns;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Провайдер данных для генерации определений атласов текстур.
 * Создаёт JSON-файлы атласов для всех категорий спрайтов: блоки, предметы,
 * баннеры, щиты, броня с трим-паттернами и прочие.
 */
@Environment(EnvType.CLIENT)
public class AtlasDefinitionProvider implements DataProvider {

	private static final Identifier TRIM_PALETTES_ID = Identifier.ofVanilla("trims/color_palettes/trim_palette");
	private static final Map<String, Identifier> TRIM_ASSET_SUFFIX_TO_COLOR_PALETTE = streamTrimAssets()
			.collect(Collectors.toMap(
					ArmorTrimAssets.AssetId::suffix,
					assetId -> Identifier.ofVanilla("trims/color_palettes/" + assetId.suffix())
			));
	private static final List<RegistryKey<ArmorTrimPattern>> ARMOR_TRIM_PATTERN_KEYS = List.of(
			ArmorTrimPatterns.SENTRY,
			ArmorTrimPatterns.DUNE,
			ArmorTrimPatterns.COAST,
			ArmorTrimPatterns.WILD,
			ArmorTrimPatterns.WARD,
			ArmorTrimPatterns.EYE,
			ArmorTrimPatterns.VEX,
			ArmorTrimPatterns.TIDE,
			ArmorTrimPatterns.SNOUT,
			ArmorTrimPatterns.RIB,
			ArmorTrimPatterns.SPIRE,
			ArmorTrimPatterns.WAYFINDER,
			ArmorTrimPatterns.SHAPER,
			ArmorTrimPatterns.SILENCE,
			ArmorTrimPatterns.RAISER,
			ArmorTrimPatterns.HOST,
			ArmorTrimPatterns.FLOW,
			ArmorTrimPatterns.BOLT
	);
	private static final List<EquipmentModel.LayerType> EQUIPMENT_MODEL_LAYER_TYPES = List.of(
			EquipmentModel.LayerType.HUMANOID, EquipmentModel.LayerType.HUMANOID_LEGGINGS
	);
	private final DataOutput.PathResolver pathResolver;

	public AtlasDefinitionProvider(DataOutput output) {
		this.pathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "atlases");
	}

	private static List<Identifier> getArmorTrimTextures() {
		List<Identifier> list = new ArrayList<>(ARMOR_TRIM_PATTERN_KEYS.size() * EQUIPMENT_MODEL_LAYER_TYPES.size());

		for (RegistryKey<ArmorTrimPattern> registryKey : ARMOR_TRIM_PATTERN_KEYS) {
			Identifier identifier = ArmorTrimPatterns.getId(registryKey);

			for (EquipmentModel.LayerType layerType : EQUIPMENT_MODEL_LAYER_TYPES) {
				list.add(identifier.withPath(trimPatternPath -> layerType.getTrimsDirectory() + "/" + trimPatternPath));
			}
		}

		return list;
	}

	private static AtlasSource createSingleAtlasSource(SpriteIdentifier spriteId) {
		return new SingleAtlasSource(spriteId.getTextureId());
	}

	private static AtlasSource createDirectoryAtlasSource(SpriteMapper spriteMapper) {
		return new DirectoryAtlasSource(spriteMapper.prefix(), spriteMapper.prefix() + "/");
	}

	private static List<AtlasSource> createAtlasSources(SpriteMapper spriteMapper) {
		return List.of(createDirectoryAtlasSource(spriteMapper));
	}

	private static List<AtlasSource> createAtlasSources(String directorySource) {
		return List.of(new DirectoryAtlasSource(directorySource, ""));
	}

	private static Stream<ArmorTrimAssets.AssetId> streamTrimAssets() {
		return ItemModelGenerator.TRIM_MATERIALS
				.stream()
				.map(ItemModelGenerator.TrimMaterial::assets)
				.flatMap(assets -> Stream.concat(Stream.of(assets.base()), assets.overrides().values().stream()))
				.sorted(Comparator.comparing(ArmorTrimAssets.AssetId::suffix));
	}

	private static List<AtlasSource> createArmorTrimsAtlasSources() {
		return List.of(new PalettedPermutationsAtlasSource(
				getArmorTrimTextures(),
				TRIM_PALETTES_ID,
				TRIM_ASSET_SUFFIX_TO_COLOR_PALETTE
		));
	}

	private static List<AtlasSource> createBlocksAtlasSources() {
		return List.of(
				createDirectoryAtlasSource(TexturedRenderLayers.BLOCK_SPRITE_MAPPER),
				createDirectoryAtlasSource(ConduitBlockEntityRenderer.SPRITE_MAPPER),
				createSingleAtlasSource(BellBlockEntityRenderer.BELL_BODY_TEXTURE),
				createSingleAtlasSource(EnchantingTableBlockEntityRenderer.BOOK_TEXTURE)
		);
	}

	private static List<AtlasSource> createItemsAtlasSources() {
		return List.of(
				createDirectoryAtlasSource(TexturedRenderLayers.ITEM_SPRITE_MAPPER),
				new PalettedPermutationsAtlasSource(
						List.of(
								ItemModelGenerator.HELMET_TRIM_ID_PREFIX,
								ItemModelGenerator.CHESTPLATE_TRIM_ID_PREFIX,
								ItemModelGenerator.LEGGINGS_TRIM_ID_PREFIX,
								ItemModelGenerator.BOOTS_TRIM_ID_PREFIX
						),
						TRIM_PALETTES_ID,
						TRIM_ASSET_SUFFIX_TO_COLOR_PALETTE
				)
		);
	}

	private static List<AtlasSource> createBannerPatternsAtlasSources() {
		return List.of(
				createSingleAtlasSource(ModelBaker.BANNER_BASE),
				createDirectoryAtlasSource(TexturedRenderLayers.BANNER_PATTERN_SPRITE_MAPPER)
		);
	}

	private static List<AtlasSource> createShieldAtlasSources() {
		return List.of(
				createSingleAtlasSource(ModelBaker.SHIELD_BASE),
				createSingleAtlasSource(ModelBaker.SHIELD_BASE_NO_PATTERN),
				createDirectoryAtlasSource(TexturedRenderLayers.SHIELD_PATTERN_SPRITE_MAPPER)
		);
	}

	private static List<AtlasSource> createGuiAtlasSources() {
		return List.of(
				new DirectoryAtlasSource("gui/sprites", ""),
				new DirectoryAtlasSource("mob_effect", "mob_effect/")
		);
	}

	/**
	 * Запускает генерацию всех атласов текстур ресурс-пака.
	 *
	 * @param writer писатель данных для сохранения результатов
	 * @return будущее, завершающееся после записи всех атласов
	 */
	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return CompletableFuture.allOf(
				runForAtlas(writer, Atlases.ARMOR_TRIMS, createArmorTrimsAtlasSources()),
				runForAtlas(writer, Atlases.BANNER_PATTERNS, createBannerPatternsAtlasSources()),
				runForAtlas(writer, Atlases.BEDS, createAtlasSources(TexturedRenderLayers.BED_SPRITE_MAPPER)),
				runForAtlas(writer, Atlases.BLOCKS, createBlocksAtlasSources()),
				runForAtlas(writer, Atlases.ITEMS, createItemsAtlasSources()),
				runForAtlas(writer, Atlases.CHESTS, createAtlasSources(TexturedRenderLayers.CHEST_SPRITE_MAPPER)),
				runForAtlas(
						writer,
						Atlases.DECORATED_POT,
						createAtlasSources(TexturedRenderLayers.DECORATED_POT_SPRITE_MAPPER)
				),
				runForAtlas(writer, Atlases.GUI, createGuiAtlasSources()),
				runForAtlas(writer, Atlases.MAP_DECORATIONS, createAtlasSources("map/decorations")),
				runForAtlas(writer, Atlases.PAINTINGS, createAtlasSources("painting")),
				runForAtlas(writer, Atlases.PARTICLES, createAtlasSources("particle")),
				runForAtlas(writer, Atlases.SHIELD_PATTERNS, createShieldAtlasSources()),
				runForAtlas(
						writer,
						Atlases.SHULKER_BOXES,
						createAtlasSources(TexturedRenderLayers.SHULKER_SPRITE_MAPPER)
				),
				runForAtlas(writer, Atlases.SIGNS, createAtlasSources(TexturedRenderLayers.SIGN_SPRITE_MAPPER)),
				runForAtlas(writer, Atlases.CELESTIALS, createAtlasSources("environment/celestial"))
		);
	}

	private CompletableFuture<?> runForAtlas(DataWriter writer, Identifier atlasId, List<AtlasSource> atlasSources) {
		return DataProvider.writeCodecToPath(
				writer,
				AtlasSourceManager.LIST_CODEC,
				atlasSources,
				pathResolver.resolveJson(atlasId)
		);
	}

	@Override
	public String getName() {
		return "Atlas Definitions";
	}
}
