package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Optional;

/**
 * Абстрактный фикс для переименования свойств блоков в {@code BLOCK_STATE}.
 * Подклассы определяют, какие блоки затрагиваются ({@link #shouldFix(String)})
 * и как именно переименовываются свойства ({@link #fix(String, Dynamic)}).
 */
public abstract class BlockPropertyRenameFix extends DataFix {

	private final String name;

	public BlockPropertyRenameFix(Schema outputSchema, String name) {
		super(outputSchema, false);
		this.name = name;
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				name,
				getInputSchema().getType(TypeReferences.BLOCK_STATE),
				typed -> typed.update(DSL.remainderFinder(), this::fix)
		);
	}

	private Dynamic<?> fix(Dynamic<?> blockState) {
		Optional<String> blockName =
				blockState.get("Name").asString().result().map(IdentifierNormalizingSchema::normalize);
		return blockName.isPresent() && shouldFix(blockName.get())
		       ? blockState.update("Properties", properties -> fix(blockName.get(), properties))
		       : blockState;
	}

	protected abstract boolean shouldFix(String id);

	protected abstract <T> Dynamic<T> fix(String id, Dynamic<T> properties);
}
