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

/**
 * Запечённая модель состояния блока: предоставляет части для рендеринга
 * и спрайт частиц при разрушении.
 */
@Environment(EnvType.CLIENT)
public interface BlockStateModel extends FabricBlockStateModel {

	void addParts(Random random, List<BlockModelPart> parts);

	default List<BlockModelPart> getParts(Random random) {
		List<BlockModelPart> parts = new ObjectArrayList<>();
		addParts(random, parts);
		return parts;
	}

	Sprite particleSprite();

	/**
	 * Незапечённая модель состояния блока с кэшированием результата запекания.
	 * Гарантирует, что один и тот же {@link BlockStateModel.Unbaked} запекается
	 * не более одного раза в рамках одного прохода через {@link Baker.ResolvableCacheKey}.
	 */
	@Environment(EnvType.CLIENT)
	class CachedUnbaked implements BlockStateModel.UnbakedGrouped {

		final BlockStateModel.Unbaked delegate;
		private final Baker.ResolvableCacheKey<BlockStateModel> cacheKey;

		public CachedUnbaked(BlockStateModel.Unbaked delegate) {
			this.delegate = delegate;
			this.cacheKey = baker -> delegate.bake(baker);
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
			delegate.resolve(resolver);
		}

		@Override
		public BlockStateModel bake(BlockState state, Baker baker) {
			return baker.compute(cacheKey);
		}

		@Override
		public Object getEqualityGroup(BlockState state) {
			return this;
		}
	}

	/**
	 * Незапечённая модель состояния блока.
	 * Содержит кодеки для десериализации как одиночного варианта,
	 * так и взвешенного списка вариантов из JSON blockstate-файлов.
	 */
	@Environment(EnvType.CLIENT)
	interface Unbaked extends ResolvableModel {

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
							                                                              entries =
							                                                              unbaked.entries().getEntries();
					                                                              List<Weighted<ModelVariant>>
							                                                              variants =
							                                                              new ArrayList<>(entries.size());

					                                                              for (Weighted<BlockStateModel.Unbaked> weighted : entries) {
						                                                              if (!(weighted.value() instanceof SimpleBlockStateModel.Unbaked simple)) {
							                                                              return DataResult.error(() -> "Only single variants are supported");
						                                                              }

						                                                              variants.add(new Weighted<>(
								                                                              simple.variant(),
								                                                              weighted.weight()
						                                                              ));
					                                                              }

					                                                              return DataResult.success(variants);
				                                                              }
		                                                              );

		Codec<BlockStateModel.Unbaked> CODEC = Codec.either(WEIGHTED_CODEC, SimpleBlockStateModel.Unbaked.CODEC)
		                                            .flatComapMap(
				                                            either -> (BlockStateModel.Unbaked) either.map(
						                                            left -> left,
						                                            right -> right
				                                            ), variant -> switch (variant) {
					                                            case SimpleBlockStateModel.Unbaked simple ->
							                                            DataResult.success(Either.right(simple));
					                                            case WeightedBlockStateModel.Unbaked weighted ->
							                                            DataResult.success(Either.left(weighted));
					                                            default ->
							                                            DataResult.error(() -> "Only a single variant or a list of variants are supported");
				                                            }
		                                            );

		BlockStateModel bake(Baker baker);

		default BlockStateModel.UnbakedGrouped cached() {
			return new BlockStateModel.CachedUnbaked(this);
		}
	}

	/**
	 * Незапечённая модель состояния блока с группировкой по равенству.
	 * Позволяет системе группировки определять, какие состояния блока
	 * используют одинаковую модель для оптимизации перерисовки.
	 */
	@Environment(EnvType.CLIENT)
	interface UnbakedGrouped extends ResolvableModel {

		BlockStateModel bake(BlockState state, Baker baker);

		Object getEqualityGroup(BlockState state);
	}
}
