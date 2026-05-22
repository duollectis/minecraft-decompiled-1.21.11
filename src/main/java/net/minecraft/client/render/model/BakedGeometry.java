package net.minecraft.client.render.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Запечённая геометрия блочной модели: набор {@link BakedQuad} сгруппированных по стороне.
 * Используется рендерером блоков для эффективной выборки квадов по направлению.
 */
@Environment(EnvType.CLIENT)
public class BakedGeometry {

	public static final BakedGeometry EMPTY = new BakedGeometry(
			List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
	);

	private final List<BakedQuad> allQuads;
	private final List<BakedQuad> sidelessQuads;
	private final List<BakedQuad> northQuads;
	private final List<BakedQuad> southQuads;
	private final List<BakedQuad> eastQuads;
	private final List<BakedQuad> westQuads;
	private final List<BakedQuad> upQuads;
	private final List<BakedQuad> downQuads;

	BakedGeometry(
			List<BakedQuad> allQuads,
			List<BakedQuad> sidelessQuads,
			List<BakedQuad> northQuads,
			List<BakedQuad> southQuads,
			List<BakedQuad> eastQuads,
			List<BakedQuad> westQuads,
			List<BakedQuad> upQuads,
			List<BakedQuad> downQuads
	) {
		this.allQuads = allQuads;
		this.sidelessQuads = sidelessQuads;
		this.northQuads = northQuads;
		this.southQuads = southQuads;
		this.eastQuads = eastQuads;
		this.westQuads = westQuads;
		this.upQuads = upQuads;
		this.downQuads = downQuads;
	}

	public List<BakedQuad> getQuads(@Nullable Direction side) {
		return switch (side) {
			case null -> sidelessQuads;
			case NORTH -> northQuads;
			case SOUTH -> southQuads;
			case EAST -> eastQuads;
			case WEST -> westQuads;
			case UP -> upQuads;
			case DOWN -> downQuads;
		};
	}

	public List<BakedQuad> getAllQuads() {
		return allQuads;
	}

	/**
	 * Строитель для пошагового накопления квадов по направлениям с последующей сборкой в {@link BakedGeometry}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final ImmutableList.Builder<BakedQuad> sidelessQuads = ImmutableList.builder();
		private final Multimap<Direction, BakedQuad> sidedQuads = ArrayListMultimap.create();

		public BakedGeometry.Builder add(Direction side, BakedQuad quad) {
			sidedQuads.put(side, quad);
			return this;
		}

		public BakedGeometry.Builder add(BakedQuad quad) {
			sidelessQuads.add(quad);
			return this;
		}

		/**
		 * Нарезает единый плоский список квадов на подсписки по направлениям через смещение.
		 * Порядок секций: sideless → north → south → east → west → up → down.
		 */
		private static BakedGeometry buildFromList(
				List<BakedQuad> quads,
				int sidelessCount,
				int northCount,
				int southCount,
				int eastCount,
				int westCount,
				int upCount,
				int downCount
		) {
			int offset = 0;
			List<BakedQuad> sideless = quads.subList(offset, offset += sidelessCount);
			List<BakedQuad> north = quads.subList(offset, offset += northCount);
			List<BakedQuad> south = quads.subList(offset, offset += southCount);
			List<BakedQuad> east = quads.subList(offset, offset += eastCount);
			List<BakedQuad> west = quads.subList(offset, offset += westCount);
			List<BakedQuad> up = quads.subList(offset, offset += upCount);
			List<BakedQuad> down = quads.subList(offset, offset + downCount);

			return new BakedGeometry(quads, sideless, north, south, east, west, up, down);
		}

		public BakedGeometry build() {
			ImmutableList<BakedQuad> builtSideless = sidelessQuads.build();

			if (sidedQuads.isEmpty()) {
				return builtSideless.isEmpty()
						? BakedGeometry.EMPTY
						: new BakedGeometry(
								builtSideless,
								builtSideless,
								List.of(),
								List.of(),
								List.of(),
								List.of(),
								List.of(),
								List.of()
						);
			}

			Collection<BakedQuad> north = sidedQuads.get(Direction.NORTH);
			Collection<BakedQuad> south = sidedQuads.get(Direction.SOUTH);
			Collection<BakedQuad> east = sidedQuads.get(Direction.EAST);
			Collection<BakedQuad> west = sidedQuads.get(Direction.WEST);
			Collection<BakedQuad> up = sidedQuads.get(Direction.UP);
			Collection<BakedQuad> down = sidedQuads.get(Direction.DOWN);

			ImmutableList<BakedQuad> all = ImmutableList.<BakedQuad>builder()
					.addAll(builtSideless)
					.addAll(north)
					.addAll(south)
					.addAll(east)
					.addAll(west)
					.addAll(up)
					.addAll(down)
					.build();

			return buildFromList(
					all,
					builtSideless.size(),
					north.size(),
					south.size(),
					east.size(),
					west.size(),
					up.size(),
					down.size()
			);
		}
	}
}
