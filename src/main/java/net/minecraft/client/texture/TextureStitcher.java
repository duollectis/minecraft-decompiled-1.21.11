package net.minecraft.client.texture;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Упаковщик спрайтов в текстурный атлас методом bin-packing.
 *
 * <p>Принимает набор {@link Stitchable} через {@link #add}, сортирует их по убыванию
 * размера и жадно размещает в прямоугольные {@link Slot}. При нехватке места атлас
 * расширяется до следующей степени двойки через {@link #growAndFit}.
 *
 * @param <T> тип спрайта, реализующий {@link Stitchable}
 */
@Environment(EnvType.CLIENT)
public class TextureStitcher<T extends TextureStitcher.Stitchable> {

	private static final Comparator<Holder<?>> COMPARATOR =
		Comparator.<Holder<?>, Integer>comparing(holder -> -holder.height)
			.thenComparing(holder -> -holder.width)
			.thenComparing(holder -> holder.sprite.getId());

	private final int mipLevel;
	private final List<Holder<T>> holders = new ArrayList<>();
	private final List<Slot<T>> slots = new ArrayList<>();
	private int width;
	private int height;
	private final int maxWidth;
	private final int maxHeight;
	private final int padding;

	public TextureStitcher(int maxWidth, int maxHeight, int mipLevel, int anisotropy) {
		this.mipLevel = mipLevel;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		padding = 1 << mipLevel << MathHelper.clamp(anisotropy - 1, 0, 4);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void add(T info) {
		Holder<T> holder = new Holder<>(
			info,
			applyMipLevel(info.getWidth() + padding * 2, mipLevel),
			applyMipLevel(info.getHeight() + padding * 2, mipLevel)
		);
		holders.add(holder);
	}

	/**
	 * Выполняет упаковку всех добавленных спрайтов.
	 * Бросает {@link TextureStitcherCannotFitException}, если спрайт не помещается
	 * даже после максимального расширения атласа.
	 */
	public void stitch() {
		List<Holder<T>> sorted = new ArrayList<>(holders);
		sorted.sort(COMPARATOR);

		for (Holder<T> holder : sorted) {
			if (!fit(holder)) {
				throw new TextureStitcherCannotFitException(
					holder.sprite,
					sorted.stream().map(h -> h.sprite).collect(ImmutableList.toImmutableList())
				);
			}
		}
	}

	public void getStitchedSprites(SpriteConsumer<T> consumer) {
		for (Slot<T> slot : slots) {
			slot.addAllFilledSlots(consumer, padding);
		}
	}

	private static int applyMipLevel(int size, int mipLevel) {
		return (size >> mipLevel) + ((size & (1 << mipLevel) - 1) == 0 ? 0 : 1) << mipLevel;
	}

	private boolean fit(Holder<T> holder) {
		for (Slot<T> slot : slots) {
			if (slot.fit(holder)) {
				return true;
			}
		}

		return growAndFit(holder);
	}

	/**
	 * Расширяет атлас до следующей степени двойки и пытается разместить спрайт.
	 * Выбирает направление расширения (по ширине или высоте) так, чтобы
	 * атлас оставался как можно более квадратным.
	 */
	private boolean growAndFit(Holder<T> holder) {
		int currentWidthPow2 = MathHelper.smallestEncompassingPowerOfTwo(width);
		int currentHeightPow2 = MathHelper.smallestEncompassingPowerOfTwo(height);
		int newWidthPow2 = MathHelper.smallestEncompassingPowerOfTwo(width + holder.width);
		int newHeightPow2 = MathHelper.smallestEncompassingPowerOfTwo(height + holder.height);
		boolean canGrowWidth = newWidthPow2 <= maxWidth;
		boolean canGrowHeight = newHeightPow2 <= maxHeight;

		if (!canGrowWidth && !canGrowHeight) {
			return false;
		}

		boolean widthWouldGrow = canGrowWidth && currentWidthPow2 != newWidthPow2;
		boolean heightWouldGrow = canGrowHeight && currentHeightPow2 != newHeightPow2;
		boolean growWidth = widthWouldGrow ^ heightWouldGrow
			? widthWouldGrow
			: canGrowWidth && currentWidthPow2 <= currentHeightPow2;

		Slot<T> newSlot;

		if (growWidth) {
			if (height == 0) {
				height = newHeightPow2;
			}

			newSlot = new Slot<>(width, 0, newWidthPow2 - width, height);
			width = newWidthPow2;
		} else {
			newSlot = new Slot<>(0, height, width, newHeightPow2 - height);
			height = newHeightPow2;
		}

		newSlot.fit(holder);
		slots.add(newSlot);
		return true;
	}

	@Environment(EnvType.CLIENT)
	record Holder<T extends Stitchable>(T sprite, int width, int height) {
	}

	/**
	 * Прямоугольная ячейка в атласе. Может содержать один спрайт или
	 * рекурсивно делиться на подячейки при размещении меньшего спрайта.
	 */
	@Environment(EnvType.CLIENT)
	public static class Slot<T extends Stitchable> {

		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private @Nullable List<Slot<T>> subSlots;
		private @Nullable Holder<T> texture;

		public Slot(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		/**
		 * Пытается разместить {@code holder} в данной ячейке или в одной из подячеек.
		 * При первом размещении меньшего спрайта делит ячейку на подячейки,
		 * выбирая разбиение, минимизирующее потери площади.
		 */
		public boolean fit(Holder<T> holder) {
			if (texture != null) {
				return false;
			}

			int holderWidth = holder.width;
			int holderHeight = holder.height;

			if (holderWidth > width || holderHeight > height) {
				return false;
			}

			if (holderWidth == width && holderHeight == height) {
				texture = holder;
				return true;
			}

			if (subSlots == null) {
				subSlots = new ArrayList<>(1);
				subSlots.add(new Slot<>(x, y, holderWidth, holderHeight));
				int remainWidth = width - holderWidth;
				int remainHeight = height - holderHeight;

				if (remainHeight > 0 && remainWidth > 0) {
					int maxDim = Math.max(height, remainWidth);
					int altDim = Math.max(width, remainHeight);

					if (maxDim >= altDim) {
						subSlots.add(new Slot<>(x, y + holderHeight, holderWidth, remainHeight));
						subSlots.add(new Slot<>(x + holderWidth, y, remainWidth, height));
					} else {
						subSlots.add(new Slot<>(x + holderWidth, y, remainWidth, holderHeight));
						subSlots.add(new Slot<>(x, y + holderHeight, width, remainHeight));
					}
				} else if (remainWidth == 0) {
					subSlots.add(new Slot<>(x, y + holderHeight, holderWidth, remainHeight));
				} else if (remainHeight == 0) {
					subSlots.add(new Slot<>(x + holderWidth, y, remainWidth, holderHeight));
				}
			}

			for (Slot<T> subSlot : subSlots) {
				if (subSlot.fit(holder)) {
					return true;
				}
			}

			return false;
		}

		public void addAllFilledSlots(SpriteConsumer<T> consumer, int padding) {
			if (texture != null) {
				consumer.load(texture.sprite, getX(), getY(), padding);
			} else if (subSlots != null) {
				for (Slot<T> subSlot : subSlots) {
					subSlot.addAllFilledSlots(consumer, padding);
				}
			}
		}

		@Override
		public String toString() {
			return "Slot{originX=" + x + ", originY=" + y + ", width=" + width
				+ ", height=" + height + ", texture=" + texture + ", subSlots=" + subSlots + "}";
		}
	}

	@Environment(EnvType.CLIENT)
	public interface SpriteConsumer<T extends Stitchable> {

		void load(T info, int x, int y, int padding);
	}

	@Environment(EnvType.CLIENT)
	public interface Stitchable {

		int getWidth();

		int getHeight();

		Identifier getId();
	}
}
