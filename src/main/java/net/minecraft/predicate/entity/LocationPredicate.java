package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.CampfireBlock;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.FluidPredicate;
import net.minecraft.predicate.LightPredicate;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

import java.util.Optional;

/**
 * Предикат местоположения. Проверяет позицию в мире по биому, структуре, измерению,
 * дыму, освещению, блоку, жидкости и видимости неба.
 */
public record LocationPredicate(
		Optional<LocationPredicate.PositionRange> position,
		Optional<RegistryEntryList<Biome>> biomes,
		Optional<RegistryEntryList<Structure>> structures,
		Optional<RegistryKey<World>> dimension,
		Optional<Boolean> smokey,
		Optional<LightPredicate> light,
		Optional<BlockPredicate> block,
		Optional<FluidPredicate> fluid,
		Optional<Boolean> canSeeSky
) {

	public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					LocationPredicate.PositionRange.CODEC
							.optionalFieldOf("position")
							.forGetter(LocationPredicate::position),
					RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("biomes").forGetter(LocationPredicate::biomes),
					RegistryCodecs
							.entryList(RegistryKeys.STRUCTURE)
							.optionalFieldOf("structures")
							.forGetter(LocationPredicate::structures),
					RegistryKey.createCodec(RegistryKeys.WORLD)
							.optionalFieldOf("dimension")
							.forGetter(LocationPredicate::dimension),
					Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationPredicate::smokey),
					LightPredicate.CODEC.optionalFieldOf("light").forGetter(LocationPredicate::light),
					BlockPredicate.CODEC.optionalFieldOf("block").forGetter(LocationPredicate::block),
					FluidPredicate.CODEC.optionalFieldOf("fluid").forGetter(LocationPredicate::fluid),
					Codec.BOOL.optionalFieldOf("can_see_sky").forGetter(LocationPredicate::canSeeSky)
			).apply(instance, LocationPredicate::new)
	);

	public boolean test(ServerWorld world, double x, double y, double z) {
		if (position.isPresent() && !position.get().test(x, y, z)) {
			return false;
		}

		if (dimension.isPresent() && dimension.get() != world.getRegistryKey()) {
			return false;
		}

		BlockPos blockPos = BlockPos.ofFloored(x, y, z);
		boolean loaded = world.isPosLoaded(blockPos);

		if (biomes.isPresent() && !(loaded && biomes.get().contains(world.getBiome(blockPos)))) {
			return false;
		}

		if (structures.isPresent()
				&& !(loaded && world.getStructureAccessor()
						.getStructureContaining(blockPos, structures.get())
						.hasChildren())
		) {
			return false;
		}

		if (smokey.isPresent()
				&& !(loaded && smokey.get() == CampfireBlock.isLitCampfireInRange(world, blockPos))
		) {
			return false;
		}

		if (light.isPresent() && !light.get().test(world, blockPos)) {
			return false;
		}

		if (block.isPresent() && !block.get().test(world, blockPos)) {
			return false;
		}

		if (fluid.isPresent() && !fluid.get().test(world, blockPos)) {
			return false;
		}

		return canSeeSky.isEmpty() || canSeeSky.get() == world.isSkyVisible(blockPos);
	}

	/**
	 * Строитель для составления {@link LocationPredicate} с фильтрами по координатам, биому, измерению и видимости неба.
	 */
	public static class Builder {

		private NumberRange.DoubleRange x = NumberRange.DoubleRange.ANY;
		private NumberRange.DoubleRange y = NumberRange.DoubleRange.ANY;
		private NumberRange.DoubleRange z = NumberRange.DoubleRange.ANY;
		private Optional<RegistryEntryList<Biome>> biome = Optional.empty();
		private Optional<RegistryEntryList<Structure>> feature = Optional.empty();
		private Optional<RegistryKey<World>> dimension = Optional.empty();
		private Optional<Boolean> smokey = Optional.empty();
		private Optional<LightPredicate> light = Optional.empty();
		private Optional<BlockPredicate> block = Optional.empty();
		private Optional<FluidPredicate> fluid = Optional.empty();
		private Optional<Boolean> canSeeSky = Optional.empty();

		public static LocationPredicate.Builder create() {
			return new LocationPredicate.Builder();
		}

		public static LocationPredicate.Builder createBiome(RegistryEntry<Biome> biome) {
			return create().biome(RegistryEntryList.of(biome));
		}

		public static LocationPredicate.Builder createDimension(RegistryKey<World> dimension) {
			return create().dimension(dimension);
		}

		public static LocationPredicate.Builder createStructure(RegistryEntry<Structure> structure) {
			return create().structure(RegistryEntryList.of(structure));
		}

		public static LocationPredicate.Builder createY(NumberRange.DoubleRange y) {
			return create().y(y);
		}

		public LocationPredicate.Builder x(NumberRange.DoubleRange x) {
			this.x = x;
			return this;
		}

		public LocationPredicate.Builder y(NumberRange.DoubleRange y) {
			this.y = y;
			return this;
		}

		public LocationPredicate.Builder z(NumberRange.DoubleRange z) {
			this.z = z;
			return this;
		}

		public LocationPredicate.Builder biome(RegistryEntryList<Biome> biome) {
			this.biome = Optional.of(biome);
			return this;
		}

		public LocationPredicate.Builder structure(RegistryEntryList<Structure> structure) {
			feature = Optional.of(structure);
			return this;
		}

		public LocationPredicate.Builder dimension(RegistryKey<World> dimension) {
			this.dimension = Optional.of(dimension);
			return this;
		}

		public LocationPredicate.Builder light(LightPredicate.Builder light) {
			this.light = Optional.of(light.build());
			return this;
		}

		public LocationPredicate.Builder block(BlockPredicate.Builder block) {
			this.block = Optional.of(block.build());
			return this;
		}

		public LocationPredicate.Builder fluid(FluidPredicate.Builder fluid) {
			this.fluid = Optional.of(fluid.build());
			return this;
		}

		public LocationPredicate.Builder smokey(boolean smokey) {
			this.smokey = Optional.of(smokey);
			return this;
		}

		public LocationPredicate.Builder canSeeSky(boolean canSeeSky) {
			this.canSeeSky = Optional.of(canSeeSky);
			return this;
		}

		public LocationPredicate build() {
			Optional<LocationPredicate.PositionRange> positionRange =
					LocationPredicate.PositionRange.create(x, y, z);
			return new LocationPredicate(positionRange, biome, feature, dimension, smokey, light, block, fluid, canSeeSky);
		}
	}

	/**
	 * Диапазон координат позиции по трём осям.
	 */
	record PositionRange(NumberRange.DoubleRange x, NumberRange.DoubleRange y, NumberRange.DoubleRange z) {

		public static final Codec<LocationPredicate.PositionRange> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						NumberRange.DoubleRange.CODEC
								.optionalFieldOf("x", NumberRange.DoubleRange.ANY)
								.forGetter(LocationPredicate.PositionRange::x),
						NumberRange.DoubleRange.CODEC
								.optionalFieldOf("y", NumberRange.DoubleRange.ANY)
								.forGetter(LocationPredicate.PositionRange::y),
						NumberRange.DoubleRange.CODEC
								.optionalFieldOf("z", NumberRange.DoubleRange.ANY)
								.forGetter(LocationPredicate.PositionRange::z)
				).apply(instance, LocationPredicate.PositionRange::new)
		);

		static Optional<LocationPredicate.PositionRange> create(
				NumberRange.DoubleRange x,
				NumberRange.DoubleRange y,
				NumberRange.DoubleRange z
		) {
			return x.isDummy() && y.isDummy() && z.isDummy()
					? Optional.empty()
					: Optional.of(new LocationPredicate.PositionRange(x, y, z));
		}

		public boolean test(double x, double y, double z) {
			return this.x.test(x) && this.y.test(y) && this.z.test(z);
		}
	}
}
