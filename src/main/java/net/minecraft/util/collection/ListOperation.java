package net.minecraft.util.collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Операция над списком, описывающая способ применения новых значений к существующему списку.
 * Поддерживает режимы: замена всего списка, замена секции, вставка и добавление в конец.
 */
public interface ListOperation {

	MapCodec<ListOperation> UNLIMITED_SIZE_CODEC = createCodec(Integer.MAX_VALUE);

	/**
	 * Создаёт кодек для операций над списком с ограничением максимального размера.
	 *
	 * @param maxSize максимально допустимый размер результирующего списка
	 * @return кодек для сериализации/десериализации операций
	 */
	@SuppressWarnings("unchecked")
	static MapCodec<ListOperation> createCodec(int maxSize) {
		MapCodec<ListOperation> baseCodec = (MapCodec<ListOperation>) (MapCodec<?>) Mode.CODEC.dispatchMap(
			"mode", ListOperation::getMode, mode -> mode.codec
		);

		return baseCodec.validate(operation -> {
			if (operation instanceof ReplaceSection replaceSection && replaceSection.size().isPresent()) {
				int sectionSize = replaceSection.size().get();
				if (sectionSize > maxSize) {
					return DataResult.error(() -> "Size value too large: " + sectionSize + ", max size is " + maxSize);
				}
			}

			return DataResult.success(operation);
		});
	}

	Mode getMode();

	default <T> List<T> apply(List<T> current, List<T> values) {
		return apply(current, values, Integer.MAX_VALUE);
	}

	<T> List<T> apply(List<T> current, List<T> values, int maxSize);

	class Append implements ListOperation {

		private static final Logger LOGGER = LogUtils.getLogger();
		public static final Append INSTANCE = new Append();
		public static final MapCodec<Append> CODEC = MapCodec.unit(() -> INSTANCE);

		private Append() {
		}

		@Override
		public Mode getMode() {
			return Mode.APPEND;
		}

		@Override
		public <T> List<T> apply(List<T> current, List<T> values, int maxSize) {
			if (current.size() + values.size() > maxSize) {
				LOGGER.error("Contents overflow in section append");
				return current;
			}

			return Stream.concat(current.stream(), values.stream()).toList();
		}
	}

	record Insert(int offset) implements ListOperation {

		private static final Logger LOGGER = LogUtils.getLogger();
		public static final MapCodec<Insert> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codecs.NON_NEGATIVE_INT
					.optionalFieldOf("offset", 0)
					.forGetter(Insert::offset))
				.apply(instance, Insert::new)
		);

		@Override
		public Mode getMode() {
			return Mode.INSERT;
		}

		@Override
		public <T> List<T> apply(List<T> current, List<T> values, int maxSize) {
			int currentSize = current.size();

			if (offset > currentSize) {
				LOGGER.error("Cannot insert when offset is out of bounds");
				return current;
			}

			if (currentSize + values.size() > maxSize) {
				LOGGER.error("Contents overflow in section insertion");
				return current;
			}

			Builder<T> builder = ImmutableList.builder();
			builder.addAll(current.subList(0, offset));
			builder.addAll(values);
			builder.addAll(current.subList(offset, currentSize));
			return builder.build();
		}
	}

	enum Mode implements StringIdentifiable {
		REPLACE_ALL("replace_all", ReplaceAll.CODEC),
		REPLACE_SECTION("replace_section", ReplaceSection.CODEC),
		INSERT("insert", Insert.CODEC),
		APPEND("append", Append.CODEC);

		public static final Codec<Mode> CODEC = StringIdentifiable.createCodec(Mode::values);

		private final String id;
		final MapCodec<? extends ListOperation> codec;

		Mode(String id, MapCodec<? extends ListOperation> codec) {
			this.id = id;
			this.codec = codec;
		}

		public MapCodec<? extends ListOperation> getCodec() {
			return codec;
		}

		@Override
		public String asString() {
			return id;
		}
	}

	class ReplaceAll implements ListOperation {

		public static final ReplaceAll INSTANCE = new ReplaceAll();
		public static final MapCodec<ReplaceAll> CODEC = MapCodec.unit(() -> INSTANCE);

		private ReplaceAll() {
		}

		@Override
		public Mode getMode() {
			return Mode.REPLACE_ALL;
		}

		@Override
		public <T> List<T> apply(List<T> current, List<T> values, int maxSize) {
			return values;
		}
	}

	record ReplaceSection(int offset, Optional<Integer> size) implements ListOperation {

		private static final Logger LOGGER = LogUtils.getLogger();
		public static final MapCodec<ReplaceSection> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codecs.NON_NEGATIVE_INT
					.optionalFieldOf("offset", 0)
					.forGetter(ReplaceSection::offset),
				Codecs.NON_NEGATIVE_INT
					.optionalFieldOf("size")
					.forGetter(ReplaceSection::size)
			).apply(instance, ReplaceSection::new)
		);

		public ReplaceSection(int offset) {
			this(offset, Optional.empty());
		}

		@Override
		public Mode getMode() {
			return Mode.REPLACE_SECTION;
		}

		@Override
		public <T> List<T> apply(List<T> current, List<T> values, int maxSize) {
			int currentSize = current.size();

			if (offset > currentSize) {
				LOGGER.error("Cannot replace when offset is out of bounds");
				return current;
			}

			Builder<T> builder = ImmutableList.builder();
			builder.addAll(current.subList(0, offset));
			builder.addAll(values);

			int endOffset = offset + size.orElse(values.size());

			if (endOffset < currentSize) {
				builder.addAll(current.subList(endOffset, currentSize));
			}

			List<T> result = builder.build();

			if (result.size() > maxSize) {
				LOGGER.error("Contents overflow in section replacement");
				return current;
			}

			return result;
		}
	}

	record Values<T>(List<T> value, ListOperation operation) {

		public static <T> Codec<Values<T>> createCodec(Codec<T> codec, int maxSize) {
			return RecordCodecBuilder.create(
				instance -> instance.group(
					codec.sizeLimitedListOf(maxSize).fieldOf("values").forGetter(values -> values.value),
					ListOperation.createCodec(maxSize).forGetter(values -> values.operation)
				).apply(instance, Values::new)
			);
		}

		public List<T> apply(List<T> current) {
			return operation.apply(current, value);
		}
	}
}
