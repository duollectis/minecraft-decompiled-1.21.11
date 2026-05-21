package net.minecraft.client.render.model;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockStateModel}.
 */
public interface BlockStateModel extends FabricBlockStateModel {

	void addParts(Random random, List<BlockModelPart> parts);

	default List<BlockModelPart> getParts(Random random) {
		List<BlockModelPart> list = new ObjectArrayList();
		this.addParts(random, list);
		return list;
	}

	Sprite particleSprite();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CachedUnbaked}.
	 */
	public static class CachedUnbaked implements BlockStateModel.UnbakedGrouped {

		final BlockStateModel.Unbaked delegate;
		private final Baker.ResolvableCacheKey<BlockStateModel>
				cacheKey =
				new Baker.ResolvableCacheKey<BlockStateModel>() {
					public BlockStateModel compute(Baker baker) {
						return CachedUnbaked.this.delegate.bake(baker);
					}
				};

		public CachedUnbaked(BlockStateModel.Unbaked delegate) {
			this.delegate = delegate;
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			this.delegate.resolve(resolver);
		}

		@Override
		public BlockStateModel bake(BlockState state, Baker baker) {
			return baker.compute(this.cacheKey);
		}

		@Override
		public Object getEqualityGroup(BlockState state) {
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public interface Unbaked extends ResolvableModel {

		Codec<Weighted<ModelVariant>> WEIGHTED_VARIANT_CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    ModelVariant.MAP_CODEC.forGetter(Weighted::value),
						                    Codecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Weighted::weight)
				                    )
				                    .apply(instance, Weighted::new)
		);

		Codec<WeightedBlockStateModel.Unbaked> WEIGHTED_CODEC = Codecs.nonEmptyList(WEIGHTED_VARIANT_CODEC.listOf())
		                                                              .flatComapMap(
				                                                              list -> new WeightedBlockStateModel.Unbaked(
						                                                              Pool.of(Lists.transform(
								                                                              list,
								                                                              weighted -> weighted.transform(
										                                                              SimpleBlockStateModel.Unbaked::new)
						                                                              ))),
				                                                              unbaked -> {
					                                                              List<Weighted<BlockStateModel.Unbaked>>
							                                                              list =
							                                                              unbaked
									                                                              .entries()
									                                                              .getEntries();
					                                                              List<Weighted<ModelVariant>>
							                                                              list2 =
							                                                              new ArrayList<>(list.size());

					                                                              for (Weighted<BlockStateModel.Unbaked> weighted : list) {
						                                                              if (!(weighted.value() instanceof SimpleBlockStateModel.Unbaked unbaked2)) {
							                                                              return DataResult.error(() -> "Only single variants are supported");
						                                                              }

						                                                              list2.add(new Weighted<>(
								                                                              unbaked2.variant(),
								                                                              weighted.weight()
						                                                              ));
					                                                              }

					                                                              return DataResult.success(list2);
				                                                              }
		                                                              );

		Codec<BlockStateModel.Unbaked> CODEC = Codec.either(WEIGHTED_CODEC, SimpleBlockStateModel.Unbaked.CODEC)
		                                            .flatComapMap(
				                                            either -> (BlockStateModel.Unbaked) either.map(
						                                            left -> left,
						                                            right -> right
				                                            ), variant -> {
					                                            return switch (variant) {
						                                            case SimpleBlockStateModel.Unbaked unbaked2 ->
								                                            DataResult.success(Either.right(unbaked2));
						                                            case WeightedBlockStateModel.Unbaked unbaked3 ->
								                                            DataResult.success(Either.left(unbaked3));
						                                            default ->
								                                            DataResult.error(() -> "Only a single variant or a list of variants are supported");
					                                            };
				                                            }
		                                            );

		BlockStateModel bake(Baker baker);

		default BlockStateModel.UnbakedGrouped cached() {
			return new BlockStateModel.CachedUnbaked(this);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code UnbakedGrouped}.
	 */
	public interface UnbakedGrouped extends ResolvableModel {

		BlockStateModel bake(BlockState state, Baker baker);

		Object getEqualityGroup(BlockState state);
	}
}
