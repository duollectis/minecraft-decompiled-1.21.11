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
 * {@code NbtHelper}.
 */
public final class NbtHelper {

	private static final Comparator<NbtList>
			BLOCK_POS_COMPARATOR =
			Comparator.<NbtList>comparingInt(nbt -> nbt.getInt(1, 0))
			          .thenComparingInt(nbt -> nbt.getInt(0, 0))
			          .thenComparingInt(nbt -> nbt.getInt(2, 0));
	private static final Comparator<NbtList>
			ENTITY_POS_COMPARATOR =
			Comparator.<NbtList>comparingDouble(nbt -> nbt.getDouble(1, 0.0))
			          .thenComparingDouble(nbt -> nbt.getDouble(0, 0.0))
			          .thenComparingDouble(nbt -> nbt.getDouble(2, 0.0));
	private static final Codec<RegistryKey<Block>> BLOCK_KEY_CODEC = RegistryKey.createCodec(RegistryKeys.BLOCK);
	public static final String DATA_KEY = "data";
	private static final char LEFT_CURLY_BRACKET = '{';
	private static final char RIGHT_CURLY_BRACKET = '}';
	private static final String COMMA = ",";
	private static final char COLON = ':';
	private static final Splitter COMMA_SPLITTER = Splitter.on(",");
	private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int PALETTE_VERSION = 2;
	private static final int INVALID_PALETTE_VERSION = -1;

	private NbtHelper() {
	}

	@VisibleForTesting
	public static boolean matches(
			@Nullable NbtElement standard,
			@Nullable NbtElement subject,
			boolean ignoreListOrder
	) {
		if (standard == subject) {
			return true;
		}
		else if (standard == null) {
			return true;
		}
		else if (subject == null) {
			return false;
		}
		else if (!standard.getClass().equals(subject.getClass())) {
			return false;
		}
		else if (standard instanceof NbtCompound nbtCompound) {
			NbtCompound nbtCompound2 = (NbtCompound) subject;
			if (nbtCompound2.getSize() < nbtCompound.getSize()) {
				return false;
			}
			else {
				for (Entry<String, NbtElement> entry : nbtCompound.entrySet()) {
					NbtElement nbtElement = entry.getValue();
					if (!matches(nbtElement, nbtCompound2.get(entry.getKey()), ignoreListOrder)) {
						return false;
					}
				}

				return true;
			}
		}
		else if (standard instanceof NbtList nbtList && ignoreListOrder) {
			NbtList nbtList2 = (NbtList) subject;
			if (nbtList.isEmpty()) {
				return nbtList2.isEmpty();
			}
			else if (nbtList2.size() < nbtList.size()) {
				return false;
			}
			else {
				for (NbtElement nbtElement2 : nbtList) {
					boolean bl = false;

					for (NbtElement nbtElement3 : nbtList2) {
						if (matches(nbtElement2, nbtElement3, ignoreListOrder)) {
							bl = true;
							break;
						}
					}

					if (!bl) {
						return false;
					}
				}

				return true;
			}
		}
		else {
			return standard.equals(subject);
		}
	}

	public static BlockState toBlockState(RegistryEntryLookup<Block> blockLookup, NbtCompound nbt) {
		Optional<? extends RegistryEntry<Block>>
				optional =
				nbt.<RegistryKey<Block>>get("Name", BLOCK_KEY_CODEC).flatMap(blockLookup::getOptional);
		if (optional.isEmpty()) {
			return Blocks.AIR.getDefaultState();
		}
		else {
			Block block = optional.get().value();
			BlockState blockState = block.getDefaultState();
			Optional<NbtCompound> optional2 = nbt.getCompound("Properties");
			if (optional2.isPresent()) {
				StateManager<Block, BlockState> stateManager = block.getStateManager();

				for (String string : optional2.get().getKeys()) {
					Property<?> property = stateManager.getProperty(string);
					if (property != null) {
						blockState = withProperty(blockState, property, string, optional2.get(), nbt);
					}
				}
			}

			return blockState;
		}
	}

	private static <S extends State<?, S>, T extends Comparable<T>> S withProperty(
			S state, Property<T> property, String key, NbtCompound properties, NbtCompound root
	) {
		Optional<T> optional = properties.getString(key).flatMap(property::parse);
		if (optional.isPresent()) {
			return state.with(property, optional.get());
		}
		else {
			LOGGER.warn(
					"Unable to read property: {} with value: {} for blockstate: {}",
					new Object[]{key, properties.get(key), root}
			);
			return state;
		}
	}

	public static NbtCompound fromBlockState(BlockState state) {
		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.putString("Name", Registries.BLOCK.getId(state.getBlock()).toString());
		Map<Property<?>, Comparable<?>> map = state.getEntries();
		if (!map.isEmpty()) {
			NbtCompound nbtCompound2 = new NbtCompound();

			for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
				Property<?> property = entry.getKey();
				nbtCompound2.putString(property.getName(), nameValue(property, entry.getValue()));
			}

			nbtCompound.put("Properties", nbtCompound2);
		}

		return nbtCompound;
	}

	public static NbtCompound fromFluidState(FluidState state) {
		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.putString("Name", Registries.FLUID.getId(state.getFluid()).toString());
		Map<Property<?>, Comparable<?>> map = state.getEntries();
		if (!map.isEmpty()) {
			NbtCompound nbtCompound2 = new NbtCompound();

			for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
				Property<?> property = entry.getKey();
				nbtCompound2.putString(property.getName(), nameValue(property, entry.getValue()));
			}

			nbtCompound.put("Properties", nbtCompound2);
		}

		return nbtCompound;
	}

	private static <T extends Comparable<T>> String nameValue(Property<T> property, Comparable<?> value) {
		return property.name((T) value);
	}

	public static String toFormattedString(NbtElement nbt) {
		return toFormattedString(nbt, false);
	}

	public static String toFormattedString(NbtElement nbt, boolean withArrayContents) {
		return appendFormattedString(new StringBuilder(), nbt, 0, withArrayContents).toString();
	}

	public static StringBuilder appendFormattedString(
			StringBuilder stringBuilder,
			NbtElement nbt,
			int depth,
			boolean withArrayContents
	) {
		return switch (nbt) {
			case NbtPrimitive nbtPrimitive -> stringBuilder.append(nbtPrimitive);
			case NbtEnd nbtEnd -> stringBuilder;
			case NbtByteArray nbtByteArray -> {
				byte[] bs = nbtByteArray.getByteArray();
				int i = bs.length;
				appendIndent(depth, stringBuilder).append("byte[").append(i).append("] {\n");
				if (withArrayContents) {
					appendIndent(depth + 1, stringBuilder);

					for (int j = 0; j < bs.length; j++) {
						if (j != 0) {
							stringBuilder.append(',');
						}

						if (j % 16 == 0 && j / 16 > 0) {
							stringBuilder.append('\n');
							if (j < bs.length) {
								appendIndent(depth + 1, stringBuilder);
							}
						}
						else if (j != 0) {
							stringBuilder.append(' ');
						}

						stringBuilder.append(String.format(Locale.ROOT, "0x%02X", bs[j] & 255));
					}
				}
				else {
					appendIndent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
				}

				stringBuilder.append('\n');
				appendIndent(depth, stringBuilder).append('}');
				yield stringBuilder;
			}
			case NbtList nbtList -> {
				int i = nbtList.size();
				appendIndent(depth, stringBuilder).append("list").append("[").append(i).append("] [");
				if (i != 0) {
					stringBuilder.append('\n');
				}

				for (int j = 0; j < i; j++) {
					if (j != 0) {
						stringBuilder.append(",\n");
					}

					appendIndent(depth + 1, stringBuilder);
					appendFormattedString(stringBuilder, nbtList.get(j), depth + 1, withArrayContents);
				}

				if (i != 0) {
					stringBuilder.append('\n');
				}

				appendIndent(depth, stringBuilder).append(']');
				yield stringBuilder;
			}
			case NbtIntArray nbtIntArray -> {
				int[] is = nbtIntArray.getIntArray();
				int k = 0;

				for (int l : is) {
					k = Math.max(k, String.format(Locale.ROOT, "%X", l).length());
				}

				int m = is.length;
				appendIndent(depth, stringBuilder).append("int[").append(m).append("] {\n");
				if (withArrayContents) {
					appendIndent(depth + 1, stringBuilder);

					for (int n = 0; n < is.length; n++) {
						if (n != 0) {
							stringBuilder.append(',');
						}

						if (n % 16 == 0 && n / 16 > 0) {
							stringBuilder.append('\n');
							if (n < is.length) {
								appendIndent(depth + 1, stringBuilder);
							}
						}
						else if (n != 0) {
							stringBuilder.append(' ');
						}

						stringBuilder.append(String.format(Locale.ROOT, "0x%0" + k + "X", is[n]));
					}
				}
				else {
					appendIndent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
				}

				stringBuilder.append('\n');
				appendIndent(depth, stringBuilder).append('}');
				yield stringBuilder;
			}
			case NbtCompound nbtCompound -> {
				List<String> list = Lists.newArrayList(nbtCompound.getKeys());
				Collections.sort(list);
				appendIndent(depth, stringBuilder).append('{');
				if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (depth + 1)) {
					stringBuilder.append('\n');
					appendIndent(depth + 1, stringBuilder);
				}

				int m = list.stream().mapToInt(String::length).max().orElse(0);
				String string = Strings.repeat(" ", m);

				for (int o = 0; o < list.size(); o++) {
					if (o != 0) {
						stringBuilder.append(",\n");
					}

					String string2 = list.get(o);
					appendIndent(depth + 1, stringBuilder)
							.append('"')
							.append(string2)
							.append('"')
							.append(string, 0, string.length() - string2.length())
							.append(": ");
					appendFormattedString(stringBuilder, nbtCompound.get(string2), depth + 1, withArrayContents);
				}

				if (!list.isEmpty()) {
					stringBuilder.append('\n');
				}

				appendIndent(depth, stringBuilder).append('}');
				yield stringBuilder;
			}
			case NbtLongArray nbtLongArray -> {
				long[] ls = nbtLongArray.getLongArray();
				long p = 0L;

				for (long q : ls) {
					p = Math.max(p, (long) String.format(Locale.ROOT, "%X", q).length());
				}

				long r = ls.length;
				appendIndent(depth, stringBuilder).append("long[").append(r).append("] {\n");
				if (withArrayContents) {
					appendIndent(depth + 1, stringBuilder);

					for (int s = 0; s < ls.length; s++) {
						if (s != 0) {
							stringBuilder.append(',');
						}

						if (s % 16 == 0 && s / 16 > 0) {
							stringBuilder.append('\n');
							if (s < ls.length) {
								appendIndent(depth + 1, stringBuilder);
							}
						}
						else if (s != 0) {
							stringBuilder.append(' ');
						}

						stringBuilder.append(String.format(Locale.ROOT, "0x%0" + p + "X", ls[s]));
					}
				}
				else {
					appendIndent(depth + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
				}

				stringBuilder.append('\n');
				appendIndent(depth, stringBuilder).append('}');
				yield stringBuilder;
			}
			default -> throw new MatchException(null, null);
		};
	}

	private static StringBuilder appendIndent(int depth, StringBuilder stringBuilder) {
		int i = stringBuilder.lastIndexOf("\n") + 1;
		int j = stringBuilder.length() - i;

		for (int k = 0; k < 2 * depth - j; k++) {
			stringBuilder.append(' ');
		}

		return stringBuilder;
	}

	public static Text toPrettyPrintedText(NbtElement element) {
		return new NbtTextFormatter("").apply(element);
	}

	public static String toNbtProviderString(NbtCompound compound) {
		return new NbtOrderedStringFormatter().apply(toNbtProviderFormat(compound));
	}

	public static NbtCompound fromNbtProviderString(String string) throws CommandSyntaxException {
		return fromNbtProviderFormat(StringNbtReader.readCompound(string));
	}

	@VisibleForTesting
	static NbtCompound toNbtProviderFormat(NbtCompound compound) {
		Optional<NbtList> optional = compound.getList("palettes");
		NbtList nbtList;
		if (optional.isPresent()) {
			nbtList = optional.get().getListOrEmpty(0);
		}
		else {
			nbtList = compound.getListOrEmpty("palette");
		}

		NbtList nbtList2 = nbtList.streamCompounds()
		                          .map(NbtHelper::toNbtProviderFormattedPalette)
		                          .map(NbtString::of)
		                          .collect(Collectors.toCollection(NbtList::new));
		compound.put("palette", nbtList2);
		if (optional.isPresent()) {
			NbtList nbtList3 = new NbtList();
			optional.get().stream().flatMap(nbt -> nbt.asNbtList().stream()).forEach(nbt -> {
				NbtCompound nbtCompound = new NbtCompound();

				for (int i = 0; i < nbt.size(); i++) {
					nbtCompound.putString(
							nbtList2.getString(i).orElseThrow(),
							toNbtProviderFormattedPalette(nbt.getCompound(i).orElseThrow())
					);
				}

				nbtList3.add(nbtCompound);
			});
			compound.put("palettes", nbtList3);
		}

		Optional<NbtList> optional2 = compound.getList("entities");
		if (optional2.isPresent()) {
			NbtList nbtList4 = optional2.get()
			                            .streamCompounds()
			                            .sorted(Comparator.comparing(
					                            nbt -> nbt.getList("pos"),
					                            Comparators.emptiesLast(ENTITY_POS_COMPARATOR)
			                            ))
			                            .collect(Collectors.toCollection(NbtList::new));
			compound.put("entities", nbtList4);
		}

		NbtList nbtList4 = compound.getList("blocks")
		                           .stream()
		                           .flatMap(NbtList::streamCompounds)
		                           .sorted(Comparator.comparing(
				                           nbt -> nbt.getList("pos"),
				                           Comparators.emptiesLast(BLOCK_POS_COMPARATOR)
		                           ))
		                           .peek(nbt -> nbt.putString(
				                           "state",
				                           nbtList2.getString(nbt.getInt("state", 0)).orElseThrow()
		                           ))
		                           .collect(Collectors.toCollection(NbtList::new));
		compound.put("data", nbtList4);
		compound.remove("blocks");
		return compound;
	}

	@VisibleForTesting
	static NbtCompound fromNbtProviderFormat(NbtCompound compound) {
		NbtList nbtList = compound.getListOrEmpty("palette");
		Map<String, NbtElement> map = nbtList.stream()
		                                     .flatMap(nbt -> nbt.asString().stream())
		                                     .collect(ImmutableMap.toImmutableMap(
				                                     Function.identity(),
				                                     NbtHelper::fromNbtProviderFormattedPalette
		                                     ));
		Optional<NbtList> optional = compound.getList("palettes");
		if (optional.isPresent()) {
			compound.put(
					"palettes",
					optional.get()
					        .streamCompounds()
					        .map(
							        nbt -> map.keySet()
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
			compound.put("palette", map.values().stream().collect(Collectors.toCollection(NbtList::new)));
		}

		Optional<NbtList> optional2 = compound.getList("data");
		if (optional2.isPresent()) {
			Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();
			object2IntMap.defaultReturnValue(-1);

			for (int i = 0; i < nbtList.size(); i++) {
				object2IntMap.put(nbtList.getString(i).orElseThrow(), i);
			}

			NbtList nbtList2 = optional2.get();

			for (int j = 0; j < nbtList2.size(); j++) {
				NbtCompound nbtCompound = nbtList2.getCompound(j).orElseThrow();
				String string = nbtCompound.getString("state").orElseThrow();
				int k = object2IntMap.getInt(string);
				if (k == -1) {
					throw new IllegalStateException("Entry " + string + " missing from palette");
				}

				nbtCompound.putInt("state", k);
			}

			compound.put("blocks", nbtList2);
			compound.remove("data");
		}

		return compound;
	}

	@VisibleForTesting
	static String toNbtProviderFormattedPalette(NbtCompound compound) {
		StringBuilder stringBuilder = new StringBuilder(compound.getString("Name").orElseThrow());
		compound.getCompound("Properties")
		        .ifPresent(
				        properties -> {
					        String string = properties.entrySet()
					                                  .stream()
					                                  .sorted(Entry.comparingByKey())
					                                  .map(entry -> entry.getKey() + ":" + entry
							                                  .getValue()
							                                  .asString()
							                                  .orElseThrow())
					                                  .collect(Collectors.joining(","));
					        stringBuilder.append('{').append(string).append('}');
				        }
		        );
		return stringBuilder.toString();
	}

	@VisibleForTesting
	static NbtCompound fromNbtProviderFormattedPalette(String string) {
		NbtCompound nbtCompound = new NbtCompound();
		int i = string.indexOf(123);
		String string2;
		if (i >= 0) {
			string2 = string.substring(0, i);
			NbtCompound nbtCompound2 = new NbtCompound();
			if (i + 2 <= string.length()) {
				String string3 = string.substring(i + 1, string.indexOf(125, i));
				COMMA_SPLITTER.split(string3).forEach(property -> {
					List<String> list = COLON_SPLITTER.splitToList(property);
					if (list.size() == 2) {
						nbtCompound2.putString(list.get(0), list.get(1));
					}
					else {
						LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", string);
					}
				});
				nbtCompound.put("Properties", nbtCompound2);
			}
		}
		else {
			string2 = string;
		}

		nbtCompound.putString("Name", string2);
		return nbtCompound;
	}

	public static NbtCompound putDataVersion(NbtCompound nbt) {
		int i = SharedConstants.getGameVersion().dataVersion().id();
		return putDataVersion(nbt, i);
	}

	public static NbtCompound putDataVersion(NbtCompound nbt, int dataVersion) {
		nbt.putInt("DataVersion", dataVersion);
		return nbt;
	}

	public static Dynamic<NbtElement> putDataVersion(Dynamic<NbtElement> dynamic) {
		int i = SharedConstants.getGameVersion().dataVersion().id();
		return putDataVersion(dynamic, i);
	}

	public static Dynamic<NbtElement> putDataVersion(Dynamic<NbtElement> dynamic, int dataVersion) {
		return dynamic.set("DataVersion", dynamic.createInt(dataVersion));
	}

	public static void writeDataVersion(WriteView view) {
		int i = SharedConstants.getGameVersion().dataVersion().id();
		writeDataVersion(view, i);
	}

	public static void writeDataVersion(WriteView view, int dataVersion) {
		view.putInt("DataVersion", dataVersion);
	}

	public static int getDataVersion(NbtCompound nbt) {
		return getDataVersion(nbt, -1);
	}

	public static int getDataVersion(NbtCompound nbt, int fallback) {
		return nbt.getInt("DataVersion", fallback);
	}

	public static int getDataVersion(Dynamic<?> dynamic, int fallback) {
		return dynamic.get("DataVersion").asInt(fallback);
	}
}
