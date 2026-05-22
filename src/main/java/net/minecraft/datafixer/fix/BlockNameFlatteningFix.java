package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Objects;

/**
 * Конвертирует имена блоков из устаревшего формата (числовой ID или строка без namespace)
 * в новый формат с полным namespace-идентификатором через {@link BlockStateFlattening}.
 */
public class BlockNameFlatteningFix extends DataFix {

	public BlockNameFlatteningFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	public TypeRewriteRule makeRule() {
		Type<?> inputType = getInputSchema().getType(TypeReferences.BLOCK_NAME);
		Type<?> outputType = getOutputSchema().getType(TypeReferences.BLOCK_NAME);
		Type<Pair<String, Either<Integer, String>>> legacyType = DSL.named(
				TypeReferences.BLOCK_NAME.typeName(),
				DSL.or(DSL.intType(), IdentifierNormalizingSchema.getIdentifierType())
		);
		Type<Pair<String, String>> modernType =
				DSL.named(TypeReferences.BLOCK_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType());

		if (Objects.equals(inputType, legacyType) && Objects.equals(outputType, modernType)) {
			return fixTypeEverywhere(
					"BlockNameFlatteningFix",
					legacyType,
					modernType,
					dynamicOps -> pair -> pair.mapSecond(
							either -> (String) either.map(
									BlockStateFlattening::lookupStateBlock,
									string -> BlockStateFlattening.lookupBlock(
											IdentifierNormalizingSchema.normalize(string)
									)
							)
					)
			);
		}

		throw new IllegalStateException("Expected and actual types don't match.");
	}
}
