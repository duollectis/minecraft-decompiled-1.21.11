package net.minecraft.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.function.Supplier;

/**
 * Маска фильтрации чат-сообщения на стороне сервера.
 * Определяет, какие символы сообщения были отфильтрованы модерацией.
 * Три режима: пропустить всё ({@code PASS_THROUGH}), заблокировать всё ({@code FULLY_FILTERED}),
 * или частичная фильтрация по битовой маске ({@code PARTIALLY_FILTERED}).
 */
public class FilterMask {

	public static final Codec<FilterMask> CODEC = StringIdentifiable
			.createCodec(FilterMask.FilterStatus::values)
			.dispatch(FilterMask::getStatus, FilterMask.FilterStatus::getCodec);

	public static final FilterMask FULLY_FILTERED = new FilterMask(new BitSet(0), FilterStatus.FULLY_FILTERED);
	public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterStatus.PASS_THROUGH);
	public static final Style FILTERED_STYLE = Style.EMPTY
			.withColor(Formatting.DARK_GRAY)
			.withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.filtered")));

	static final MapCodec<FilterMask> PASS_THROUGH_CODEC = MapCodec.unit(PASS_THROUGH);
	static final MapCodec<FilterMask> FULLY_FILTERED_CODEC = MapCodec.unit(FULLY_FILTERED);
	static final MapCodec<FilterMask> PARTIALLY_FILTERED_CODEC = Codecs.BIT_SET
			.xmap(FilterMask::new, FilterMask::getMask)
			.fieldOf("value");

	private static final char FILTERED_CHAR = '#';

	private final BitSet mask;
	private final FilterStatus status;

	private FilterMask(BitSet mask, FilterStatus status) {
		this.mask = mask;
		this.status = status;
	}

	private FilterMask(BitSet mask) {
		this.mask = mask;
		this.status = FilterStatus.PARTIALLY_FILTERED;
	}

	public FilterMask(int length) {
		this(new BitSet(length), FilterStatus.PARTIALLY_FILTERED);
	}

	private FilterStatus getStatus() {
		return status;
	}

	private BitSet getMask() {
		return mask;
	}

	public static FilterMask readMask(PacketByteBuf buf) {
		FilterStatus filterStatus = buf.readEnumConstant(FilterStatus.class);

		return switch (filterStatus) {
			case PASS_THROUGH -> PASS_THROUGH;
			case FULLY_FILTERED -> FULLY_FILTERED;
			case PARTIALLY_FILTERED -> new FilterMask(buf.readBitSet(), FilterStatus.PARTIALLY_FILTERED);
		};
	}

	public static void writeMask(PacketByteBuf buf, FilterMask mask) {
		buf.writeEnumConstant(mask.status);

		if (mask.status == FilterStatus.PARTIALLY_FILTERED) {
			buf.writeBitSet(mask.mask);
		}
	}

	public void markFiltered(int index) {
		mask.set(index);
	}

	/**
	 * Применяет маску к строке: заменяет отфильтрованные символы на {@code '#'}.
	 *
	 * @param raw исходная строка сообщения
	 * @return отфильтрованная строка, или {@code null} если сообщение полностью заблокировано
	 */
	public @Nullable String filter(String raw) {
		return switch (status) {
			case PASS_THROUGH -> raw;
			case FULLY_FILTERED -> null;
			case PARTIALLY_FILTERED -> {
				char[] chars = raw.toCharArray();

				for (int index = 0; index < chars.length && index < mask.length(); index++) {
					if (mask.get(index)) {
						chars[index] = FILTERED_CHAR;
					}
				}

				yield new String(chars);
			}
		};
	}

	/**
	 * Строит форматированный {@link Text} с визуальным выделением отфильтрованных фрагментов.
	 * Отфильтрованные участки заменяются серыми символами {@code '#'} со стилем {@link #FILTERED_STYLE}.
	 *
	 * @param message исходная строка сообщения
	 * @return форматированный текст, или {@code null} если сообщение полностью заблокировано
	 */
	public @Nullable Text getFilteredText(String message) {
		return switch (status) {
			case PASS_THROUGH -> Text.literal(message);
			case FULLY_FILTERED -> null;
			case PARTIALLY_FILTERED -> {
				MutableText result = Text.empty();
				int pos = 0;
				boolean inFiltered = mask.get(0);

				while (true) {
					int nextBoundary = inFiltered ? mask.nextClearBit(pos) : mask.nextSetBit(pos);
					nextBoundary = nextBoundary < 0 ? message.length() : nextBoundary;

					if (nextBoundary == pos) {
						yield result;
					}

					if (inFiltered) {
						result.append(
								Text.literal(StringUtils.repeat(FILTERED_CHAR, nextBoundary - pos))
										.fillStyle(FILTERED_STYLE)
						);
					} else {
						result.append(message.substring(pos, nextBoundary));
					}

					inFiltered = !inFiltered;
					pos = nextBoundary;
				}
			}
		};
	}

	public boolean isPassThrough() {
		return status == FilterStatus.PASS_THROUGH;
	}

	public boolean isFullyFiltered() {
		return status == FilterStatus.FULLY_FILTERED;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FilterMask other = (FilterMask) o;
		return mask.equals(other.mask) && status == other.status;
	}

	@Override
	public int hashCode() {
		return 31 * mask.hashCode() + status.hashCode();
	}

	enum FilterStatus implements StringIdentifiable {
		PASS_THROUGH("pass_through", () -> FilterMask.PASS_THROUGH_CODEC),
		FULLY_FILTERED("fully_filtered", () -> FilterMask.FULLY_FILTERED_CODEC),
		PARTIALLY_FILTERED("partially_filtered", () -> FilterMask.PARTIALLY_FILTERED_CODEC);

		private final String id;
		private final Supplier<MapCodec<FilterMask>> codecSupplier;

		FilterStatus(final String id, final Supplier<MapCodec<FilterMask>> codecSupplier) {
			this.id = id;
			this.codecSupplier = codecSupplier;
		}

		@Override
		public String asString() {
			return id;
		}

		private MapCodec<FilterMask> getCodec() {
			return codecSupplier.get();
		}
	}
}
