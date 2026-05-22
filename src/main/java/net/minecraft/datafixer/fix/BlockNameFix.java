package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Абстрактный фикс для переименования блоков. Применяет {@link #rename(String)} к трём
 * типам данных: {@code BLOCK_NAME}, {@code BLOCK_STATE} и {@code FLAT_BLOCK_STATE}.
 * Используй {@link #create(Schema, String, Function)} для создания анонимных экземпляров.
 */
public abstract class BlockNameFix extends DataFix {

	private final String name;

	public BlockNameFix(Schema outputSchema, String name) {
		super(outputSchema, false);
		this.name = name;
	}

	public TypeRewriteRule makeRule() {
		Type<?> inputType = getInputSchema().getType(TypeReferences.BLOCK_NAME);
		Type<Pair<String, String>> namedType =
				DSL.named(TypeReferences.BLOCK_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType());

		if (!Objects.equals(inputType, namedType)) {
			throw new IllegalStateException("block type is not what was expected.");
		}

		TypeRewriteRule blockNameRule = fixTypeEverywhere(
				name + " for block",
				namedType,
				dynamicOps -> pair -> pair.mapSecond(this::rename)
		);
		TypeRewriteRule blockStateRule = fixTypeEverywhereTyped(
				name + " for block_state",
				getInputSchema().getType(TypeReferences.BLOCK_STATE),
				typed -> typed.update(DSL.remainderFinder(), this::fixBlockState)
		);
		TypeRewriteRule flatBlockStateRule = fixTypeEverywhereTyped(
				name + " for flat_block_state",
				getInputSchema().getType(TypeReferences.FLAT_BLOCK_STATE),
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> (Dynamic) DataFixUtils.orElse(
								dynamic
										.asString()
										.result()
										.map(this::fixFlatBlockState)
										.map(dynamic::createString),
								dynamic
						)
				)
		);

		return TypeRewriteRule.seq(blockNameRule, new TypeRewriteRule[]{blockStateRule, flatBlockStateRule});
	}

	private Dynamic<?> fixBlockState(Dynamic<?> blockStateDynamic) {
		Optional<String> blockName = blockStateDynamic.get("Name").asString().result();
		return blockName.isPresent()
		       ? blockStateDynamic.set("Name", blockStateDynamic.createString(rename(blockName.get())))
		       : blockStateDynamic;
	}

	/**
	 * Извлекает имя блока из строки flat block state (до символов {@code [} или {@code {}),
	 * применяет переименование и возвращает строку с восстановленным суффиксом.
	 */
	private String fixFlatBlockState(String flatBlockState) {
		int bracketPos = flatBlockState.indexOf('[');
		int bracePos = flatBlockState.indexOf('{');
		int nameEnd = flatBlockState.length();

		if (bracketPos > 0) {
			nameEnd = bracketPos;
		}

		if (bracePos > 0) {
			nameEnd = Math.min(nameEnd, bracePos);
		}

		String blockName = flatBlockState.substring(0, nameEnd);
		return rename(blockName) + flatBlockState.substring(nameEnd);
	}

	protected abstract String rename(String oldName);

	/**
	 * Фабричный метод для создания экземпляра {@code BlockNameFix} с заданной функцией переименования.
	 *
	 * @param outputSchema целевая схема
	 * @param name         название фикса для логирования
	 * @param rename       функция переименования блока
	 */
	public static DataFix create(Schema outputSchema, String name, Function<String, String> rename) {
		return new BlockNameFix(outputSchema, name) {
			@Override
			protected String rename(String oldName) {
				return rename.apply(oldName);
			}
		};
	}
}
