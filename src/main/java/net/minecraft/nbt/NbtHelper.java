package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.visitor.NbtOrderedStringFormatter;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Утилитарный класс для работы с NBT-данными Minecraft.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *   <li>Конвертации {@link BlockState} и {@link FluidState} в NBT и обратно.</li>
 *   <li>Форматированного вывода NBT-деревьев в строку и {@link Text}.</li>
 *   <li>Работы с форматом NBT-провайдера (структуры, палитры блоков).</li>
 *   <li>Управления версией данных ({@code DataVersion}).</li>
 *   <li>Сравнения NBT-деревьев с поддержкой нечёткого совпадения списков.</li>
 * </ul>
 */
public final class NbtHelper {

	private static final Comparator<NbtList> BLOCK_POS_COMPARATOR =
		Comparator.<NbtList>comparingInt(nbt -> nbt.getInt(1, 0))
		          .thenComparingInt(nbt -> nbt.getInt(0, 0))
		          .thenComparingInt(nbt -> nbt.getInt(2, 0));
	private static final Comparator<NbtList> ENTITY_POS_COMPARATOR =
		Comparator.<NbtList>comparingDouble(nbt -> nbt.getDouble(1, 0.0))
		          .thenComparingDouble(nbt -> nbt.getDouble(0, 0.0))
		          .thenComparingDouble(nbt -> nbt.getDouble(2, 0.0));
	private static final Codec<RegistryKey<Block>> BLOCK_KEY_CODEC = RegistryKey.createCodec(RegistryKeys.BLOCK);
	private static final Splitter COMMA_SPLITTER = Splitter.on(",");
	private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int PALETTE_VERSION = 2;
	private static final int INVALID_PALETTE_VERSION = -1;
	private static final int UNKNOWN_DATA_VERSION = 0;
	private static final char OPEN_BRACE = '{';
	private static final char CLOSE_BRACE = '}';

	public static final String DATA_KEY = "data";
	public static final String COMMA = ",";
	public static final char COLON = ':';

	private NbtHelper() {
	}

	/**
	 * Проверяет, соответствует ли {@code subject} шаблону {@code standard}.
	 * <p>
	 * Логика совпадения:
	 * <ul>
	 *   <li>Если {@code standard == null}, считается совпадением (любой subject подходит).</li>
	 *   <li>Для {@link NbtCompound}: все ключи из {@code standard} должны присутствовать в {@code subject}.</li>
	 *   <li>Для {@link NbtList} при {@code ignoreListOrder = true}: каждый элемент из {@code standard}
	 *       должен иметь совпадение в {@code subject} (порядок не важен).</li>
	 * </ul>
	 *
	 * @param standard  шаблон для сравнения (может быть {@code null})
	 * @param subject   проверяемый элемент (может быть {@code null})
	 * @param ignoreListOrder если {@code true}, порядок элементов в списках игнорируется
	 * @return {@code true}, если {@code subject} соответствует шаблону
	 */
	@VisibleForTesting
	public static boolean matches(
		@Nullable NbtElement standard,
		@Nullable NbtElement subject,
		boolean ignoreListOrder
	) {
		if (standard == subject) {
			return true;
		}

		if (standard == null) {
			return true;
		}

		if (subject == null) {
			return false;
		}

		if (!standard.getClass().equals(subject.getClass())) {
			return false;
		}

		if (standard instanceof NbtCompound standardCompound) {
			NbtCompound subjectCompound = (NbtCompound) subject;
			if (subjectCompound.getSize() < standardCompound.getSize()) {
				return false;
			}

			for (Entry<String, NbtElement> entry : standardCompound.entrySet()) {
				if (!matches(entry.getValue(), subjectCompound.get(entry.getKey()), ignoreListOrder)) {
					return false;
				}
			}

			return true;
		}

		if (standard instanceof NbtList standardList && ignoreListOrder) {
			NbtList subjectList = (NbtList) subject;
			if (standardList.isEmpty()) {
				return subjectList.isEmpty();
			}

			if (subjectList.size() < standardList.size()) {
				return false;
			}

			for (NbtElement standardElement : standardList) {
				boolean found = false;

				for (NbtElement subjectElement : subjectList) {
					if (matches(standardElement, subjectElement, ignoreListOrder)) {
						found = true;
						break;
					}
				}

				if (!found) {
					return false;
				}
			}

			return true;
		}

		return standard.equals(subject);
	}

	/**
	 * Восстанавливает {@link BlockState} из NBT-тега.
	 * Если блок не найден в реестре, возвращает {@link Blocks#AIR}.
	 *
	 * @param blockLookup источник для поиска блоков в реестре
	 * @param nbt         тег с полями {@code Name} и опциональным {@code Properties}
	 * @return восстановленный {@link BlockState}
	 */
	public static BlockState toBlockState(RegistryEntryLookup<Block> blockLookup, NbtCompound nbt) {
		Optional<? extends RegistryEntry<Block>> blockEntry =
			nbt.<RegistryKey<Block>>get("Name", BLOCK_KEY_CODEC).flatMap(blockLookup::getOptional);

		if (blockEntry.isEmpty()) {
			return Blocks.AIR.getDefaultState();
		}

		Block block = blockEntry.get().value();
		BlockState blockState = block.getDefaultState();
		Optional<NbtCompound> propertiesNbt = nbt.getCompound("Properties");

		if (propertiesNbt.isPresent()) {
			StateManager<Block, BlockState> stateManager = block.getStateManager();

			for (String propertyName : propertiesNbt.get().getKeys()) {
				Property<?> property = stateManager.getProperty(propertyName);
				if (property != null) {
					blockState = withProperty(blockState, property, propertyName, propertiesNbt.get(), nbt);
				}
			}
		}

		return blockState;
	}

	private static <S extends State<?, S>, T extends Comparable<T>> S withProperty(
		S state, Property<T> property, String key, NbtCompound properties, NbtCompound root
	) {
		Optional<T> value = properties.getString(key).flatMap(property::parse);

		if (value.isPresent()) {
			return state.with(property, value.get());
		}

		LOGGER.warn(
			"Unable to read property: {} with value: {} for blockstate: {}",
			new Object[]{key, properties.get(key), root}
		);
		return state;
	}

	/**
	 * Сериализует {@link BlockState} в NBT-тег с полями {@code Name} и {@code Properties}.
	 *
	 * @param state состояние блока
	 * @return NBT-тег с данными состояния
	 */
	public static NbtCompound fromBlockState(BlockState state) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("Name", Registries.BLOCK.getId(state.getBlock()).toString());
		Map<Property<?>, Comparable<?>> entries = state.getEntries();

		if (!entries.isEmpty()) {
			NbtCompound propertiesNbt = new NbtCompound();

			for (Entry<Property<?>, Comparable<?>> entry : entries.entrySet()) {
				Property<?> property = entry.getKey();
				propertiesNbt.putString(property.getName(), nameValue(property, entry.getValue()));
			}

			nbt.put("Properties", propertiesNbt);
		}

		return nbt;
	}

	/**
	 * Сериализует {@link FluidState} в NBT-тег с полями {@code Name} и {@code Properties}.
	 *
	 * @param state состояние жидкости
	 * @return NBT-тег с данными состояния
	 */
	public static NbtCompound fromFluidState(FluidState state) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("Name", Registries.FLUID.getId(state.getFluid()).toString());
		Map<Property<?>, Comparable<?>> entries = state.getEntries();

		if (!entries.isEmpty()) {
			NbtCompound propertiesNbt = new NbtCompound();

			for (Entry<Property<?>, Comparable<?>> entry : entries.entrySet()) {
				Property<?> property = entry.getKey();
				propertiesNbt.putString(property.getName(), nameValue(property, entry.getValue()));
			}

			nbt.put("Properties", propertiesNbt);
		}

		return nbt;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> String nameValue(Property<T> property, Comparable<?> value) {
		return property.name((T) value);
	}

	public static String toFormattedString(NbtElement nbt) {
		return toFormattedString(nbt, false);
	}

	public static String toFormattedString(NbtElement nbt, boolean withArrayContents) {
		return appendFormattedString(new StringBuilder(), nbt, 0, withArrayContents).toString();
	}

	/**
	 * Рекурсивно форматирует NBT-элемент в читаемую строку с отступами.
	 * Массивы байт/int/long выводятся в шестнадцатеричном формате.
	 * При {@code withArrayContents = false} содержимое массивов скрывается.
	 *
	 * @param builder          целевой {@link StringBuilder}
	 * @param nbt              форматируемый элемент
	 * @param depth            текущая глубина вложенности (для отступов)
	 * @param withArrayContents если {@code true}, выводит содержимое бинарных массивов
	 * @return тот же {@link StringBuilder} для цепочки вызовов
	 */
	public static StringBuilder appendFormattedString(
		StringBuilder builder,
		NbtElement nbt,
		int depth,
		boolean withArrayContents
	) {
		return switch (nbt) {
			case NbtPrimitive primitive -> builder.append(primitive);
			case NbtEnd ignored -> builder;
			case NbtByteArray byteArray -> {
				byte[] bytes = byteArray.getByteArray();
				appendIndent(depth, builder).append("byte[").append(bytes.length).append("] {\n");

				if (withArrayContents) {
					appendIndent(depth + 1, builder);

					for (int index = 0; index < bytes.length; index++) {
						if (index != 0) {
							builder.append(',');
						}

						if (index % 16 == 0 && index / 16 > 0) {
							builder.append('\n');
							if (index < bytes.length) {
								appendIndent(depth + 1, builder);
							}
						}
						else if (index != 0) {
							builder.append(' ');
						}

						builder.append(String.format(Locale.ROOT, "0x%02X", bytes[index] & 255));
					}
				}
				else {
					appendIndent(depth + 1, builder).append(" // Skipped, supply withBinaryBlobs true");
				}

				builder.append('\n');
				appendIndent(depth, builder).append('}');
				yield builder;
			}
			case NbtList list -> {
				int size = list.size();
				appendIndent(depth, builder).append("list[").append(size).append("] [");

				if (size != 0) {
					builder.append('\n');
				}

				for (int index = 0; index < size; index++) {
					if (index != 0) {
						builder.append(",\n");
					}

					appendIndent(depth + 1, builder);
					appendFormattedString(builder, list.get(index), depth + 1, withArrayContents);
				}

				if (size != 0) {
					builder.append('\n');
				}

				appendIndent(depth, builder).append(']');
				yield builder;
			}
			case NbtIntArray intArray -> {
				int[] ints = intArray.getIntArray();
				int maxHexWidth = 0;

				for (int value : ints) {
					maxHexWidth = Math.max(maxHexWidth, String.format(Locale.ROOT, "%X", value).length());
				}

				appendIndent(depth, builder).append("int[").append(ints.length).append("] {\n");

				if (withArrayContents) {
					appendIndent(depth + 1, builder);

					for (int index = 0; index < ints.length; index++) {
						if (index != 0) {
							builder.append(',');
						}

						if (index % 16 == 0 && index / 16 > 0) {
							builder.append('\n');
							if (index < ints.length) {
								appendIndent(depth + 1, builder);
							}
						}
						else if (index != 0) {
							builder.append(' ');
						}

						builder.append(String.format(Locale.ROOT, "0x%0" + maxHexWidth + "X", ints[index]));
					}
				}
				else {
					appendIndent(depth + 1, builder).append(" // Skipped, supply withBinaryBlobs true");
				}

				builder.append('\n');
				appendIndent(depth, builder).append('}');
				yield builder;
			}
			case NbtCompound compound -> {
				List<String> keys = Lists.newArrayList(compound.getKeys());
				Collections.sort(keys);
				appendIndent(depth, builder).append('{');

				if (builder.length() - builder.lastIndexOf("\n") > 2 * (depth + 1)) {
					builder.append('\n');
					appendIndent(depth + 1, builder);
				}

				int maxKeyLength = keys.stream().mapToInt(String::length).max().orElse(0);
				String padding = Strings.repeat(" ", maxKeyLength);

				for (int index = 0; index < keys.size(); index++) {
					if (index != 0) {
						builder.append(",\n");
					}

					String key = keys.get(index);
					appendIndent(depth + 1, builder)
						.append('"')
						.append(key)
						.append('"')
						.append(padding, 0, padding.length() - key.length())
						.append(": ");
					appendFormattedString(builder, compound.get(key), depth + 1, withArrayContents);
				}

				if (!keys.isEmpty()) {
					builder.append('\n');
				}

				appendIndent(depth, builder).append('}');
				yield builder;
			}
			case NbtLongArray longArray -> {
				long[] longs = longArray.getLongArray();
				int maxHexWidth = 0;

				for (long value : longs) {
					maxHexWidth = Math.max(maxHexWidth, String.format(Locale.ROOT, "%X", value).length());
				}

				appendIndent(depth, builder).append("long[").append(longs.length).append("] {\n");

				if (withArrayContents) {
					appendIndent(depth + 1, builder);

					for (int index = 0; index < longs.length; index++) {
						if (index != 0) {
							builder.append(',');
						}

						if (index % 16 == 0 && index / 16 > 0) {
							builder.append('\n');
							if (index < longs.length) {
								appendIndent(depth + 1, builder);
							}
						}
						else if (index != 0) {
							builder.append(' ');
						}

						builder.append(String.format(Locale.ROOT, "0x%0" + maxHexWidth + "X", longs[index]));
					}
				}
				else {
					appendIndent(depth + 1, builder).append(" // Skipped, supply withBinaryBlobs true");
				}

				builder.append('\n');
				appendIndent(depth, builder).append('}');
				yield builder;
			}
			default -> throw new MatchException(null, null);
		};
	}

	private static StringBuilder appendIndent(int depth, StringBuilder builder) {
		int lastNewline = builder.lastIndexOf("\n") + 1;
		int currentLineLength = builder.length() - lastNewline;

		for (int spaces = 0; spaces < 2 * depth - currentLineLength; spaces++) {
			builder.append(' ');
		}

		return builder;
	}

	/**
	 * Форматирует NBT-элемент в цветной {@link Text} для отображения в интерфейсе.
	 *
	 * @param element форматируемый элемент
	 * @return цветной текст
	 */
	public static Text toPrettyPrintedText(NbtElement element) {
		return new NbtTextFormatter("").apply(element);
	}

	/**
	 * Сериализует {@link NbtCompound} в строку формата NBT-провайдера.
	 * Используется для хранения структур в текстовом виде.
	 *
	 * @param compound исходный compound-тег
	 * @return строка в формате NBT-провайдера
	 */
	public static String toNbtProviderString(NbtCompound compound) {
		return new NbtOrderedStringFormatter().apply(toNbtProviderFormat(compound));
	}

	/**
	 * Десериализует строку формата NBT-провайдера в {@link NbtCompound}.
	 *
	 * @param string строка в формате NBT-провайдера
	 * @return восстановленный compound-тег
	 * @throws CommandSyntaxException при синтаксической ошибке
	 */
	public static NbtCompound fromNbtProviderString(String string) throws CommandSyntaxException {
		return fromNbtProviderFormat(StringNbtReader.readCompound(string));
	}

	@VisibleForTesting
	static NbtCompound toNbtProviderFormat(NbtCompound compound) {
		Optional<NbtList> palettesOpt = compound.getList("palettes");
		NbtList palette;

		if (palettesOpt.isPresent()) {
			palette = palettesOpt.get().getListOrEmpty(0);
		}
		else {
			palette = compound.getListOrEmpty("palette");
		}

		NbtList formattedPalette = palette.streamCompounds()
		                                  .map(NbtHelper::toNbtProviderFormattedPalette)
		                                  .map(NbtString::of)
		                                  .collect(Collectors.toCollection(NbtList::new));
		compound.put("palette", formattedPalette);

		if (palettesOpt.isPresent()) {
			NbtList formattedPalettes = new NbtList();
			palettesOpt.get().stream().flatMap(nbt -> nbt.asNbtList().stream()).forEach(nbt -> {
				NbtCompound paletteEntry = new NbtCompound();

				for (int index = 0; index < nbt.size(); index++) {
					paletteEntry.putString(
						formattedPalette.getString(index).orElseThrow(),
						toNbtProviderFormattedPalette(nbt.getCompound(index).orElseThrow())
					);
				}

				formattedPalettes.add(paletteEntry);
			});
			compound.put("palettes", formattedPalettes);
		}

		Optional<NbtList> entitiesOpt = compound.getList("entities");
		if (entitiesOpt.isPresent()) {
			NbtList sortedEntities = entitiesOpt.get()
			                                    .streamCompounds()
			                                    .sorted(Comparator.comparing(
				                                    nbt -> nbt.getList("pos"),
				                                    Comparators.emptiesLast(ENTITY_POS_COMPARATOR)
			                                    ))
			                                    .collect(Collectors.toCollection(NbtList::new));
			compound.put("entities", sortedEntities);
		}

		NbtList sortedBlocks = compound.getList("blocks")
		                               .stream()
		                               .flatMap(NbtList::streamCompounds)
		                               .sorted(Comparator.comparing(
			                               nbt -> nbt.getList("pos"),
			                               Comparators.emptiesLast(BLOCK_POS_COMPARATOR)
		                               ))
		                               .peek(nbt -> nbt.putString(
			                               "state",
			                               formattedPalette.getString(nbt.getInt("state", 0)).orElseThrow()
		                               ))
		                               .collect(Collectors.toCollection(NbtList::new));
		compound.put("data", sortedBlocks);
		compound.remove("blocks");
		return compound;
	}

	@VisibleForTesting
	static NbtCompound fromNbtProviderFormat(NbtCompound compound) {
		NbtList palette = compound.getListOrEmpty("palette");
		Map<String, NbtElement> paletteMap = palette.stream()
		                                            .flatMap(nbt -> nbt.asString().stream())
		                                            .collect(ImmutableMap.toImmutableMap(
			                                            Function.identity(),
			                                            NbtHelper::fromNbtProviderFormattedPalette
		                                            ));
		Optional<NbtList> palettesOpt = compound.getList("palettes");

		if (palettesOpt.isPresent()) {
			compound.put(
				"palettes",
				palettesOpt.get()
				           .streamCompounds()
				           .map(
					           nbt -> paletteMap.keySet()
					                            .stream()
					                            .map(key -> nbt.getString(key).orElseThrow())
					                            .map(NbtHelper::fromNbtProviderFormattedPalette)
					                            .collect(Collectors.toCollection(NbtList::new))
				           )
				           .collect(Collectors.toCollection(NbtList::new))
			);
			compound.remove("palette");
		}
		else {
			compound.put("palette", paletteMap.values().stream().collect(Collectors.toCollection(NbtList::new)));
		}

		Optional<NbtList> dataOpt = compound.getList("data");
		if (dataOpt.isPresent()) {
			Object2IntMap<String> paletteIndex = new Object2IntOpenHashMap<>();
			paletteIndex.defaultReturnValue(INVALID_PALETTE_VERSION);

			for (int index = 0; index < palette.size(); index++) {
				paletteIndex.put(palette.getString(index).orElseThrow(), index);
			}

			NbtList dataList = dataOpt.get();

			for (int index = 0; index < dataList.size(); index++) {
				NbtCompound block = dataList.getCompound(index).orElseThrow();
				String stateName = block.getString("state").orElseThrow();
				int stateIndex = paletteIndex.getInt(stateName);

				if (stateIndex == INVALID_PALETTE_VERSION) {
					throw new IllegalStateException("Entry " + stateName + " missing from palette");
				}

				block.putInt("state", stateIndex);
			}

			compound.put("blocks", dataList);
			compound.remove("data");
		}

		return compound;
	}

	@VisibleForTesting
	static String toNbtProviderFormattedPalette(NbtCompound compound) {
		StringBuilder builder = new StringBuilder(compound.getString("Name").orElseThrow());
		compound.getCompound("Properties")
		        .ifPresent(properties -> {
			        String propsString = properties.entrySet()
			                                       .stream()
			                                       .sorted(Entry.comparingByKey())
			                                       .map(entry -> entry.getKey() + ":" + entry.getValue()
			                                                                                  .asString()
			                                                                                  .orElseThrow())
			                                       .collect(Collectors.joining(","));
			        builder.append(OPEN_BRACE).append(propsString).append(CLOSE_BRACE);
		        });
		return builder.toString();
	}

	@VisibleForTesting
	static NbtCompound fromNbtProviderFormattedPalette(String formatted) {
		NbtCompound nbt = new NbtCompound();
		int braceIndex = formatted.indexOf(OPEN_BRACE);
		String blockName;

		if (braceIndex >= 0) {
			blockName = formatted.substring(0, braceIndex);

			if (braceIndex + 2 <= formatted.length()) {
				String propsString = formatted.substring(braceIndex + 1, formatted.indexOf(CLOSE_BRACE, braceIndex));
				NbtCompound propertiesNbt = new NbtCompound();

				COMMA_SPLITTER.split(propsString).forEach(property -> {
					List<String> parts = COLON_SPLITTER.splitToList(property);
					if (parts.size() == 2) {
						propertiesNbt.putString(parts.get(0), parts.get(1));
					}
					else {
						LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", formatted);
					}
				});

				nbt.put("Properties", propertiesNbt);
			}
		}
		else {
			blockName = formatted;
		}

		nbt.putString("Name", blockName);
		return nbt;
	}

	/**
	 * Добавляет текущую версию данных игры в NBT-тег.
	 *
	 * @param nbt целевой тег
	 * @return тот же тег с добавленным полем {@code DataVersion}
	 */
	public static NbtCompound putDataVersion(NbtCompound nbt) {
		int version = SharedConstants.getGameVersion().dataVersion().id();
		return putDataVersion(nbt, version);
	}

	public static NbtCompound putDataVersion(NbtCompound nbt, int dataVersion) {
		nbt.putInt("DataVersion", dataVersion);
		return nbt;
	}

	/**
	 * Добавляет текущую версию данных игры в {@link Dynamic}-обёртку.
	 *
	 * @param dynamic целевой dynamic-объект
	 * @return новый dynamic с добавленным полем {@code DataVersion}
	 */
	public static Dynamic<NbtElement> putDataVersion(Dynamic<NbtElement> dynamic) {
		int version = SharedConstants.getGameVersion().dataVersion().id();
		return putDataVersion(dynamic, version);
	}

	public static Dynamic<NbtElement> putDataVersion(Dynamic<NbtElement> dynamic, int dataVersion) {
		return dynamic.set("DataVersion", dynamic.createInt(dataVersion));
	}

	/**
	 * Записывает текущую версию данных игры в {@link WriteView}.
	 *
	 * @param view целевое представление для записи
	 */
	public static void writeDataVersion(WriteView view) {
		int version = SharedConstants.getGameVersion().dataVersion().id();
		writeDataVersion(view, version);
	}

	public static void writeDataVersion(WriteView view, int dataVersion) {
		view.putInt("DataVersion", dataVersion);
	}

	public static int getDataVersion(NbtCompound nbt) {
		return getDataVersion(nbt, UNKNOWN_DATA_VERSION);
	}

	public static int getDataVersion(NbtCompound nbt, int fallback) {
		return nbt.getInt("DataVersion", fallback);
	}

	public static int getDataVersion(Dynamic<?> dynamic, int fallback) {
		return dynamic.get("DataVersion").asInt(fallback);
	}
}
