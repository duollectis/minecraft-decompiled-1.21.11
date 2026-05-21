package net.minecraft.client.render.model.json;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.SimpleBlockStateModel;
import net.minecraft.client.render.model.WeightedBlockStateModel;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code WeightedVariant}.
 */
public record WeightedVariant(Pool<ModelVariant> variants) {

	public WeightedVariant(Pool<ModelVariant> variants) {
		if (variants.isEmpty()) {
			throw new IllegalArgumentException("Variant list must contain at least one element");
		}
		else {
			this.variants = variants;
		}
	}

	/**
	 * Apply.
	 *
	 * @param operator operator
	 *
	 * @return WeightedVariant — результат операции
	 */
	public WeightedVariant apply(ModelVariantOperator operator) {
		return new WeightedVariant(this.variants.transform(operator));
	}

	public BlockStateModel.Unbaked toModel() {
		List<Weighted<ModelVariant>> list = this.variants.getEntries();
		return (BlockStateModel.Unbaked) (list.size() == 1
		                                  ? new SimpleBlockStateModel.Unbaked(list.getFirst().value())
		                                  : new WeightedBlockStateModel.Unbaked(this.variants.transform(
				                                  SimpleBlockStateModel.Unbaked::new))
		);
	}
}
