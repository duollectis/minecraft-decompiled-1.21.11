package net.minecraft.client.render.model.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AxisRotation;

/**
 * Вариант модели блока: связывает идентификатор JSON-модели с её трансформацией
 * (вращение по осям X/Y/Z и UV-lock). Реализует {@link BlockModelPart.Unbaked},
 * запекаясь в {@link BlockModelPart} через {@link GeometryBakedModel}.
 */
@Environment(EnvType.CLIENT)
public record ModelVariant(Identifier modelId, ModelVariant.ModelState modelState) implements BlockModelPart.Unbaked {

	public static final MapCodec<ModelVariant> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Identifier.CODEC.fieldOf("model").forGetter(ModelVariant::modelId),
					ModelVariant.ModelState.CODEC.forGetter(ModelVariant::modelState)
			)
			.apply(instance, ModelVariant::new)
	);
	public static final Codec<ModelVariant> CODEC = MAP_CODEC.codec();

	public ModelVariant(Identifier model) {
		this(model, ModelVariant.ModelState.DEFAULT);
	}

	public ModelVariant withRotationX(AxisRotation amount) {
		return setState(modelState.setRotationX(amount));
	}

	public ModelVariant withRotationY(AxisRotation amount) {
		return setState(modelState.setRotationY(amount));
	}

	public ModelVariant withRotationZ(AxisRotation axisRotation) {
		return setState(modelState.setRotationZ(axisRotation));
	}

	public ModelVariant withUVLock(boolean uvLock) {
		return setState(modelState.setUVLock(uvLock));
	}

	public ModelVariant withModel(Identifier newModelId) {
		return new ModelVariant(newModelId, modelState);
	}

	public ModelVariant setState(ModelVariant.ModelState newState) {
		return new ModelVariant(modelId, newState);
	}

	public ModelVariant with(ModelVariantOperator variantOperator) {
		return variantOperator.apply(this);
	}

	@Override
	public BlockModelPart bake(Baker baker) {
		return GeometryBakedModel.create(baker, modelId, modelState.asModelBakeSettings());
	}

	@Override
	public void resolve(ResolvableModel.Resolver resolver) {
		resolver.markDependency(modelId);
	}

	/**
	 * Состояние трансформации варианта модели: вращение по трём осям и флаг UV-lock.
	 * Метод {@link #asModelBakeSettings()} комбинирует вращения через {@link AxisRotation#combineXYZ}
	 * и возвращает либо {@link ModelRotation}, либо его UV-модель в зависимости от флага uvLock.
	 */
	@Environment(EnvType.CLIENT)
	public record ModelState(AxisRotation x, AxisRotation y, AxisRotation z, boolean uvLock) {

		public static final MapCodec<ModelVariant.ModelState> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						AxisRotation.CODEC.optionalFieldOf("x", AxisRotation.R0).forGetter(ModelVariant.ModelState::x),
						AxisRotation.CODEC.optionalFieldOf("y", AxisRotation.R0).forGetter(ModelVariant.ModelState::y),
						AxisRotation.CODEC.optionalFieldOf("z", AxisRotation.R0).forGetter(ModelVariant.ModelState::z),
						Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(ModelVariant.ModelState::uvLock)
				)
				.apply(instance, ModelVariant.ModelState::new)
		);
		public static final ModelVariant.ModelState DEFAULT =
				new ModelVariant.ModelState(AxisRotation.R0, AxisRotation.R0, AxisRotation.R0, false);

		public ModelBakeSettings asModelBakeSettings() {
			ModelRotation modelRotation = ModelRotation.fromDirectionTransformation(
					AxisRotation.combineXYZ(x, y, z)
			);
			return (ModelBakeSettings) (uvLock ? modelRotation.getUVModel() : modelRotation);
		}

		public ModelVariant.ModelState setRotationX(AxisRotation amount) {
			return new ModelVariant.ModelState(amount, y, z, uvLock);
		}

		public ModelVariant.ModelState setRotationY(AxisRotation amount) {
			return new ModelVariant.ModelState(x, amount, z, uvLock);
		}

		public ModelVariant.ModelState setRotationZ(AxisRotation axisRotation) {
			return new ModelVariant.ModelState(x, y, axisRotation, uvLock);
		}

		public ModelVariant.ModelState setUVLock(boolean newUvLock) {
			return new ModelVariant.ModelState(x, y, z, newUvLock);
		}
	}
}
