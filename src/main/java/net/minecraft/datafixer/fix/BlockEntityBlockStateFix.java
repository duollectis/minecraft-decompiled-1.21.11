package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Конвертирует устаревший формат блок-стейта поршня (числовые {@code blockId} + {@code blockData})
 * в новый формат на основе именованных состояний блоков через {@link BlockStateFlattening}.
 */
public class BlockEntityBlockStateFix extends ChoiceFix {

	private static final int BLOCK_DATA_MASK = 15;

	public BlockEntityBlockStateFix(Schema schema, boolean changesType) {
		super(schema, changesType, "BlockEntityBlockStateFix", TypeReferences.BLOCK_ENTITY, "minecraft:piston");
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Typed<?> transform(Typed<?> inputTyped) {
		Type<?> pistonType = getOutputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:piston");
		Type<?> blockStateFieldType = pistonType.findFieldType("blockState");
		OpticFinder<?> blockStateFinder = DSL.fieldFinder("blockState", blockStateFieldType);

		Dynamic<?> remainder = (Dynamic<?>) inputTyped.get(DSL.remainderFinder());
		int blockId = remainder.get("blockId").asInt(0);
		remainder = remainder.remove("blockId");

		int blockData = remainder.get("blockData").asInt(0) & BLOCK_DATA_MASK;
		remainder = remainder.remove("blockData");

		Dynamic<?> newBlockState = BlockStateFlattening.lookupState(blockId << 4 | blockData);
		Typed<?> newTyped = pistonType
			.pointTyped(inputTyped.getOps())
			.orElseThrow(() -> new IllegalStateException("Could not create new piston block entity."));

		return newTyped
			.set(DSL.remainderFinder(), remainder)
			.set(
				(OpticFinder<Object>) blockStateFinder,
				(Object) ((Pair<?, ?>) blockStateFieldType
					.readTyped(newBlockState)
					.result()
					.orElseThrow(() -> new IllegalStateException("Could not parse newly created block state tag."))
				).getFirst()
			);
	}
}
