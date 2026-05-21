package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.json.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code MultipartBlockModelDefinitionCreator}.
 */
public class MultipartBlockModelDefinitionCreator implements BlockModelDefinitionCreator {

	private final Block block;
	private final List<MultipartBlockModelDefinitionCreator.Part> multiparts = new ArrayList<>();

	private MultipartBlockModelDefinitionCreator(Block block) {
		this.block = block;
	}

	@Override
	public Block getBlock() {
		return this.block;
	}

	/**
	 * Create.
	 *
	 * @param block block
	 *
	 * @return MultipartBlockModelDefinitionCreator — результат операции
	 */
	public static MultipartBlockModelDefinitionCreator create(Block block) {
		return new MultipartBlockModelDefinitionCreator(block);
	}

	/**
	 * With.
	 *
	 * @param part part
	 *
	 * @return MultipartBlockModelDefinitionCreator — результат операции
	 */
	public MultipartBlockModelDefinitionCreator with(WeightedVariant part) {
		this.multiparts.add(new MultipartBlockModelDefinitionCreator.Part(Optional.empty(), part));
		return this;
	}

	private void validate(MultipartModelCondition selector) {
		selector.instantiate(this.block.getStateManager());
	}

	/**
	 * With.
	 *
	 * @param condition condition
	 * @param part part
	 *
	 * @return MultipartBlockModelDefinitionCreator — результат операции
	 */
	public MultipartBlockModelDefinitionCreator with(MultipartModelCondition condition, WeightedVariant part) {
		this.validate(condition);
		this.multiparts.add(new MultipartBlockModelDefinitionCreator.Part(Optional.of(condition), part));
		return this;
	}

	public MultipartBlockModelDefinitionCreator with(
			MultipartModelConditionBuilder conditionBuilder,
			WeightedVariant part
	) {
		return this.with(conditionBuilder.build(), part);
	}

	@Override
	public BlockModelDefinition createBlockModelDefinition() {
		return new BlockModelDefinition(
				Optional.empty(),
				Optional.of(new BlockModelDefinition.Multipart(this.multiparts
						.stream()
						.map(MultipartBlockModelDefinitionCreator.Part::toComponent)
						.toList()))
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Part}.
	 */
	record Part(Optional<MultipartModelCondition> condition, WeightedVariant variants) {

		/**
		 * To component.
		 *
		 * @return MultipartModelComponent — результат операции
		 */
		public MultipartModelComponent toComponent() {
			return new MultipartModelComponent(this.condition, this.variants.toModel());
		}
	}
}
