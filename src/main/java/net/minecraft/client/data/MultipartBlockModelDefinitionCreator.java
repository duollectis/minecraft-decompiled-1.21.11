package net.minecraft.client.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.json.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Создатель определения модели блока в формате multipart.
 * Позволяет добавлять части модели с опциональными условиями на свойства блока,
 * что соответствует секции {@code multipart} в JSON-файле blockstate.
 */
@Environment(EnvType.CLIENT)
public class MultipartBlockModelDefinitionCreator implements BlockModelDefinitionCreator {

	private final Block block;
	private final List<Part> multiparts = new ArrayList<>();

	private MultipartBlockModelDefinitionCreator(Block block) {
		this.block = block;
	}

	@Override
	public Block getBlock() {
		return block;
	}

	/**
	 * Создаёт новый экземпляр для заданного блока.
	 *
	 * @param block блок, для которого создаётся определение
	 * @return новый создатель multipart-определения
	 */
	public static MultipartBlockModelDefinitionCreator create(Block block) {
		return new MultipartBlockModelDefinitionCreator(block);
	}

	/**
	 * Добавляет безусловную часть модели (применяется всегда).
	 *
	 * @param part вариант модели
	 * @return {@code this} для цепочки вызовов
	 */
	public MultipartBlockModelDefinitionCreator with(WeightedVariant part) {
		multiparts.add(new Part(Optional.empty(), part));
		return this;
	}

	private void validate(MultipartModelCondition selector) {
		selector.instantiate(block.getStateManager());
	}

	/**
	 * Добавляет условную часть модели.
	 *
	 * @param condition условие на свойства блока
	 * @param part      вариант модели, применяемый при выполнении условия
	 * @return {@code this} для цепочки вызовов
	 */
	public MultipartBlockModelDefinitionCreator with(MultipartModelCondition condition, WeightedVariant part) {
		validate(condition);
		multiparts.add(new Part(Optional.of(condition), part));
		return this;
	}

	public MultipartBlockModelDefinitionCreator with(
			MultipartModelConditionBuilder conditionBuilder,
			WeightedVariant part
	) {
		return with(conditionBuilder.build(), part);
	}

	@Override
	public BlockModelDefinition createBlockModelDefinition() {
		return new BlockModelDefinition(
				Optional.empty(),
				Optional.of(new BlockModelDefinition.Multipart(
						multiparts.stream()
								.map(Part::toComponent)
								.toList()
				))
		);
	}

	/**
	 * Одна часть multipart-определения: опциональное условие и вариант модели.
	 */
	@Environment(EnvType.CLIENT)
	record Part(Optional<MultipartModelCondition> condition, WeightedVariant variants) {

		public MultipartModelComponent toComponent() {
			return new MultipartModelComponent(condition, variants.toModel());
		}
	}
}
