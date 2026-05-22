package net.minecraft.block.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.SharedConstants;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Менеджер распространения скалька. Управляет курсорами, каждый из которых несёт заряд опыта
 * и постепенно конвертирует соседние блоки в скальк, двигаясь по миру.
 */
public class SculkSpreadManager {

	public static final int MAX_SPREAD_RADIUS = 24;
	public static final int MAX_CHARGE = 1000;
	public static final float EXTRA_BLOCK_CHANCE = 0.5F;
	private static final int MAX_CURSORS = 32;
	public static final int MIN_SPREAD_CHARGE = 11;
	public static final int MAX_CURSOR_DISTANCE = 1024;
	final boolean worldGen;
	private final TagKey<Block> replaceableTag;
	private final int extraBlockChance;
	private final int maxDistance;
	private final int spreadChance;
	private final int decayChance;
	private List<SculkSpreadManager.Cursor> cursors = new ArrayList<>();

	public SculkSpreadManager(
			boolean worldGen,
			TagKey<Block> replaceableTag,
			int extraBlockChance,
			int maxDistance,
			int spreadChance,
			int decayChance
	) {
		this.worldGen = worldGen;
		this.replaceableTag = replaceableTag;
		this.extraBlockChance = extraBlockChance;
		this.maxDistance = maxDistance;
		this.spreadChance = spreadChance;
		this.decayChance = decayChance;
	}

	/**
	 * Create.
	 *
	 * @return SculkSpreadManager — результат операции
	 */
	public static SculkSpreadManager create() {
		return new SculkSpreadManager(false, BlockTags.SCULK_REPLACEABLE, 10, 4, 10, 5);
	}

	/**
	 * Создаёт world gen.
	 *
	 * @return SculkSpreadManager — результат операции
	 */
	public static SculkSpreadManager createWorldGen() {
		return new SculkSpreadManager(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10);
	}

	public TagKey<Block> getReplaceableTag() {
		return replaceableTag;
	}

	public int getExtraBlockChance() {
		return extraBlockChance;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public int getSpreadChance() {
		return spreadChance;
	}

	public int getDecayChance() {
		return decayChance;
	}

	public boolean isWorldGen() {
		return worldGen;
	}

	@VisibleForTesting
	public List<SculkSpreadManager.Cursor> getCursors() {
		return cursors;
	}

	public void clearCursors() {
		cursors.clear();
	}

	public void readData(ReadView view) {
		cursors.clear();
		view
			.<List<SculkSpreadManager.Cursor>>read("cursors", SculkSpreadManager.Cursor.CODEC.sizeLimitedListOf(MAX_CURSORS))
			.orElse(List.of())
			.forEach(this::addCursor);
	}

	public void writeData(WriteView view) {
		view.put("cursors", SculkSpreadManager.Cursor.CODEC.listOf(), cursors);

		if (SharedConstants.SCULK_CATALYST) {
			int totalCharge = cursors.stream().map(SculkSpreadManager.Cursor::getCharge).reduce(0, Integer::sum);
			int cursorCount = cursors.size();
			int maxCharge = cursors.stream().map(SculkSpreadManager.Cursor::getCharge).reduce(0, Math::max);

			view.putInt("stats.total", totalCharge);
			view.putInt("stats.count", cursorCount);
			view.putInt("stats.max", maxCharge);
			view.putInt("stats.avg", totalCharge / (cursorCount + 1));
		}
	}

	public void spread(BlockPos pos, int charge) {
		while (charge > 0) {
			int batch = Math.min(charge, MAX_CHARGE);
			addCursor(new SculkSpreadManager.Cursor(pos, batch));
			charge -= batch;
		}
	}

	private void addCursor(SculkSpreadManager.Cursor cursor) {
		if (cursors.size() < MAX_CURSORS) {
			cursors.add(cursor);
		}
	}

	public void tick(WorldAccess world, BlockPos pos, Random random, boolean shouldConvertToBlock) {
		if (cursors.isEmpty()) {
			return;
		}

		List<SculkSpreadManager.Cursor> activeCursors = new ArrayList<>();
		Map<BlockPos, SculkSpreadManager.Cursor> posToLeadCursor = new HashMap<>();
		Object2IntMap<BlockPos> posToTotalCharge = new Object2IntOpenHashMap();

		for (SculkSpreadManager.Cursor cursor : cursors) {
			if (cursor.isTooFarFrom(pos)) {
				continue;
			}

			cursor.spread(world, pos, random, this, shouldConvertToBlock);

			if (cursor.charge <= 0) {
				world.syncWorldEvent(3006, cursor.getPos(), 0);
				continue;
			}

			BlockPos cursorPos = cursor.getPos();
			posToTotalCharge.computeInt(cursorPos, (p, charge) -> (charge == null ? 0 : charge) + cursor.charge);

			SculkSpreadManager.Cursor existing = posToLeadCursor.get(cursorPos);

			if (existing == null) {
				posToLeadCursor.put(cursorPos, cursor);
				activeCursors.add(cursor);
			} else if (!isWorldGen() && cursor.charge + existing.charge <= MAX_CHARGE) {
				existing.merge(cursor);
			} else {
				activeCursors.add(cursor);

				if (cursor.charge < existing.charge) {
					posToLeadCursor.put(cursorPos, cursor);
				}
			}
		}

		for (Entry<BlockPos> entry : posToTotalCharge.object2IntEntrySet()) {
			BlockPos entryPos = entry.getKey();
			int totalCharge = entry.getIntValue();
			SculkSpreadManager.Cursor leadCursor = posToLeadCursor.get(entryPos);
			Collection<Direction> faces = leadCursor == null ? null : leadCursor.getFaces();

			if (totalCharge > 0 && faces != null) {
				int particleLevel = (int) (Math.log1p(totalCharge) / 2.3F) + 1;
				int eventData = (particleLevel << 6) + MultifaceBlock.directionsToFlag(faces);
				world.syncWorldEvent(3006, entryPos, eventData);
			}
		}

		cursors = activeCursors;
	}

	/**
	 * Курсор распространения скалька. Хранит позицию, заряд опыта и набор граней,
	 * по которым скальк уже распространился на текущей позиции.
	 */
	public static class Cursor {

		private static final ObjectArrayList<Vec3i> OFFSETS = Util.make(
			new ObjectArrayList(18),
			list -> BlockPos.stream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
				.filter(pos -> (pos.getX() == 0 || pos.getY() == 0 || pos.getZ() == 0) && !pos.equals(BlockPos.ORIGIN))
				.map(BlockPos::toImmutable)
				.forEach(list::add)
		);

		public static final int MAX_DECAY_DELAY = 1;

		private static final Codec<Set<Direction>> DIRECTION_SET_CODEC = Direction.CODEC
			.listOf()
			.xmap(directions -> Sets.newEnumSet(directions, Direction.class), Lists::newArrayList);

		public static final Codec<SculkSpreadManager.Cursor> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(SculkSpreadManager.Cursor::getPos),
				Codec.intRange(0, MAX_CHARGE).fieldOf("charge").orElse(0).forGetter(SculkSpreadManager.Cursor::getCharge),
				Codec.intRange(0, 1).fieldOf("decay_delay").orElse(1).forGetter(SculkSpreadManager.Cursor::getDecay),
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("update_delay").orElse(0).forGetter(cursor -> cursor.update),
				DIRECTION_SET_CODEC.lenientOptionalFieldOf("facings").forGetter(cursor -> Optional.ofNullable(cursor.getFaces()))
			).apply(instance, SculkSpreadManager.Cursor::new)
		);

		private BlockPos pos;
		int charge;
		private int update;
		private int decay;
		private @Nullable Set<Direction> faces;

		private Cursor(BlockPos pos, int charge, int decay, int update, Optional<Set<Direction>> faces) {
			this.pos = pos;
			this.charge = charge;
			this.decay = decay;
			this.update = update;
			this.faces = faces.orElse(null);
		}

		public Cursor(BlockPos pos, int charge) {
			this(pos, charge, 1, 0, Optional.empty());
		}

		public BlockPos getPos() {
			return pos;
		}

		boolean isTooFarFrom(BlockPos pos) {
			return this.pos.getChebyshevDistance(pos) > MAX_CURSOR_DISTANCE;
		}

		public int getCharge() {
			return charge;
		}

		public int getDecay() {
			return decay;
		}

		public @Nullable Set<Direction> getFaces() {
			return faces;
		}

		private boolean canSpread(WorldAccess world, BlockPos pos, boolean isWorldGen) {
			if (charge <= 0) {
				return false;
			}

			if (isWorldGen) {
				return true;
			}

			return world instanceof ServerWorld serverWorld && serverWorld.shouldTickBlockPos(pos);
		}

		public void spread(
			WorldAccess world,
			BlockPos pos,
			Random random,
			SculkSpreadManager spreadManager,
			boolean shouldConvertToBlock
		) {
			if (!canSpread(world, pos, spreadManager.worldGen)) {
				return;
			}

			if (update > 0) {
				update--;
				return;
			}

			BlockState blockState = world.getBlockState(this.pos);
			SculkSpreadable sculkSpreadable = getSpreadable(blockState);

			if (shouldConvertToBlock && sculkSpreadable.spread(world, this.pos, blockState, faces, spreadManager.isWorldGen())) {
				if (sculkSpreadable.shouldConvertToSpreadable()) {
					blockState = world.getBlockState(this.pos);
					sculkSpreadable = getSpreadable(blockState);
				}

				world.playSound(null, this.pos, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.BLOCKS, 1.0F, 1.0F);
			}

			charge = sculkSpreadable.spread(this, world, pos, random, spreadManager, shouldConvertToBlock);

			if (charge <= 0) {
				sculkSpreadable.spreadAtSamePosition(world, blockState, this.pos, random);
				return;
			}

			BlockPos nextPos = getSpreadPos(world, this.pos, random);

			if (nextPos != null) {
				sculkSpreadable.spreadAtSamePosition(world, blockState, this.pos, random);
				this.pos = nextPos.toImmutable();

				if (spreadManager.isWorldGen() && !this.pos.isWithinDistance(new Vec3i(pos.getX(), this.pos.getY(), pos.getZ()), 15.0)) {
					charge = 0;
					return;
				}

				blockState = world.getBlockState(nextPos);
			}

			if (blockState.getBlock() instanceof SculkSpreadable) {
				faces = MultifaceBlock.collectDirections(blockState);
			}

			decay = sculkSpreadable.getDecay(decay);
			update = sculkSpreadable.getUpdate();
		}

		void merge(SculkSpreadManager.Cursor other) {
			charge = charge + other.charge;
			other.charge = 0;
			update = Math.min(update, other.update);
		}

		private static SculkSpreadable getSpreadable(BlockState state) {
			return state.getBlock() instanceof SculkSpreadable sculkSpreadable
				? sculkSpreadable
				: SculkSpreadable.VEIN_ONLY_SPREADER;
		}

		private static List<Vec3i> shuffleOffsets(Random random) {
			return Util.copyShuffled(OFFSETS, random);
		}

		private static @Nullable BlockPos getSpreadPos(WorldAccess world, BlockPos pos, Random random) {
			BlockPos.Mutable best = pos.mutableCopy();
			BlockPos.Mutable candidate = pos.mutableCopy();

			for (Vec3i offset : shuffleOffsets(random)) {
				candidate.set(pos, offset);
				BlockState candidateState = world.getBlockState(candidate);

				if (candidateState.getBlock() instanceof SculkSpreadable && canSpread(world, pos, candidate)) {
					best.set(candidate);

					if (SculkVeinBlock.veinCoversSculkReplaceable(world, candidateState, candidate)) {
						break;
					}
				}
			}

			return best.equals(pos) ? null : best;
		}

		private static boolean canSpread(WorldAccess world, BlockPos sourcePos, BlockPos targetPos) {
			if (sourcePos.getManhattanDistance(targetPos) == 1) {
				return true;
			}

			BlockPos delta = targetPos.subtract(sourcePos);
			Direction dirX = Direction.from(Direction.Axis.X, delta.getX() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
			Direction dirY = Direction.from(Direction.Axis.Y, delta.getY() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
			Direction dirZ = Direction.from(Direction.Axis.Z, delta.getZ() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);

			if (delta.getX() == 0) {
				return canSpread(world, sourcePos, dirY) || canSpread(world, sourcePos, dirZ);
			}

			return delta.getY() == 0
				? canSpread(world, sourcePos, dirX) || canSpread(world, sourcePos, dirZ)
				: canSpread(world, sourcePos, dirX) || canSpread(world, sourcePos, dirY);
		}

		private static boolean canSpread(WorldAccess world, BlockPos pos, Direction direction) {
			BlockPos neighbor = pos.offset(direction);
			return !world.getBlockState(neighbor).isSideSolidFullSquare(world, neighbor, direction.getOpposite());
		}
	}
}
