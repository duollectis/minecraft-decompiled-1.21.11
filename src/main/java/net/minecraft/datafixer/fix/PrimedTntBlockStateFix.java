package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class PrimedTntBlockStateFix extends ChoiceWriteReadFix {

	public PrimedTntBlockStateFix(Schema outputSchema) {
		super(outputSchema, true, "PrimedTnt BlockState fixer", TypeReferences.ENTITY, "minecraft:tnt");
	}

	private static <T> Dynamic<T> fixFuse(Dynamic<T> data) {
		Optional<Dynamic<T>> optional = data.get("Fuse").get().result();
		return optional.isPresent() ? data.set("fuse", optional.get()) : data;
	}

	private static <T> Dynamic<T> fixBlockState(Dynamic<T> data) {
		return data.set(
				"block_state",
				data.createMap(Map.of(data.createString("Name"), data.createString("minecraft:tnt")))
		);
	}

	@Override
	protected <T> Dynamic<T> transform(Dynamic<T> data) {
		return fixFuse(fixBlockState(data));
	}
}
