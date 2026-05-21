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
 * Класс filter mask.
 */
public class FilterMask {

	public static final Codec<FilterMask> CODEC = StringIdentifiable.createCodec(FilterMask.FilterStatus::values)
	                                                                .dispatch(
			                                                                FilterMask::getStatus,
			                                                                FilterMask.FilterStatus::getCodec
	                                                                );
	public static final FilterMask
			FULLY_FILTERED =
			new FilterMask(new BitSet(0), FilterMask.FilterStatus.FULLY_FILTERED);
	public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.FilterStatus.PASS_THROUGH);
	public static final Style FILTERED_STYLE = Style.EMPTY
			.withColor(Formatting.DARK_GRAY)
			.withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.filtered")));
	static final MapCodec<FilterMask> PASS_THROUGH_CODEC = MapCodec.unit(PASS_THROUGH);
	static final MapCodec<FilterMask> FULLY_FILTERED_CODEC = MapCodec.unit(FULLY_FILTERED);
	static final MapCodec<FilterMask>
			PARTIALLY_FILTERED_CODEC =
			Codecs.BIT_SET.xmap(FilterMask::new, FilterMask::getMask).fieldOf("value");
	private static final char FILTERED = '#';
	private final BitSet mask;
	private final FilterMask.FilterStatus status;

	private FilterMask(BitSet mask, FilterMask.FilterStatus status) {
		this.mask = mask;
		this.status = status;
	}

	private FilterMask(BitSet mask) {
		this.mask = mask;
		this.status = FilterMask.FilterStatus.PARTIALLY_FILTERED;
	}

	public FilterMask(int length) {
		this(new BitSet(length), FilterMask.FilterStatus.PARTIALLY_FILTERED);
	}

	private FilterMask.FilterStatus getStatus() {
		return this.status;
	}

	private BitSet getMask() {
		return this.mask;
	}

	/**
	 * Читает mask.
	 *
	 * @param buf buf
	 *
	 * @return FilterMask — результат операции
	 */
	public static FilterMask readMask(PacketByteBuf buf) {
		FilterMask.FilterStatus filterStatus = buf.readEnumConstant(FilterMask.FilterStatus.class);

		return switch (filterStatus) {
			case PASS_THROUGH -> PASS_THROUGH;
			case FULLY_FILTERED -> FULLY_FILTERED;
			case PARTIALLY_FILTERED -> new FilterMask(buf.readBitSet(), FilterMask.FilterStatus.PARTIALLY_FILTERED);
		};
	}

	/**
	 * Записывает mask.
	 *
	 * @param buf buf
	 * @param mask mask
	 */
	public static void writeMask(PacketByteBuf buf, FilterMask mask) {
		buf.writeEnumConstant(mask.status);
		if (mask.status == FilterMask.FilterStatus.PARTIALLY_FILTERED) {
			buf.writeBitSet(mask.mask);
		}
	}

	/**
	 * Mark filtered.
	 *
	 * @param index index
	 */
	public void markFiltered(int index) {
		this.mask.set(index);
	}

	/**
	 * Filter.
	 *
	 * @param raw raw
	 *
	 * @return @Nullable String — результат операции
	 */
	public @Nullable String filter(String raw) {
		return switch (this.status) {
			case PASS_THROUGH -> raw;
			case FULLY_FILTERED -> null;
			case PARTIALLY_FILTERED -> {
				char[] cs = raw.toCharArray();

				for (int i = 0; i < cs.length && i < this.mask.length(); i++) {
					if (this.mask.get(i)) {
						cs[i] = '#';
					}
				}

				yield new String(cs);
			}
		};
	}

	public @Nullable Text getFilteredText(String message) {
		return switch (this.status) {
			case PASS_THROUGH -> Text.literal(message);
			case FULLY_FILTERED -> null;
			case PARTIALLY_FILTERED -> {
				MutableText mutableText = Text.empty();
				int i = 0;
				boolean bl = this.mask.get(0);

				while (true) {
					int j = bl ? this.mask.nextClearBit(i) : this.mask.nextSetBit(i);
					j = j < 0 ? message.length() : j;
					if (j == i) {
						yield mutableText;
					}

					if (bl) {
						mutableText.append(Text.literal(StringUtils.repeat('#', j - i)).fillStyle(FILTERED_STYLE));
					}
					else {
						mutableText.append(message.substring(i, j));
					}

					bl = !bl;
					i = j;
				}
			}
		};
	}

	public boolean isPassThrough() {
		return this.status == FilterMask.FilterStatus.PASS_THROUGH;
	}

	public boolean isFullyFiltered() {
		return this.status == FilterMask.FilterStatus.FULLY_FILTERED;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o != null && this.getClass() == o.getClass()) {
			FilterMask filterMask = (FilterMask) o;
			return this.mask.equals(filterMask.mask) && this.status == filterMask.status;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int i = this.mask.hashCode();
		return 31 * i + this.status.hashCode();
	}

	static enum FilterStatus implements StringIdentifiable {
		PASS_THROUGH("pass_through", () -> FilterMask.PASS_THROUGH_CODEC),
		FULLY_FILTERED("fully_filtered", () -> FilterMask.FULLY_FILTERED_CODEC),
		PARTIALLY_FILTERED("partially_filtered", () -> FilterMask.PARTIALLY_FILTERED_CODEC);

		private final String id;
		private final Supplier<MapCodec<FilterMask>> codecSupplier;

		private FilterStatus(final String id, final Supplier<MapCodec<FilterMask>> codecSupplier) {
			this.id = id;
			this.codecSupplier = codecSupplier;
		}

		@Override
		public String asString() {
			return this.id;
		}

		private MapCodec<FilterMask> getCodec() {
			return this.codecSupplier.get();
		}
	}
}
