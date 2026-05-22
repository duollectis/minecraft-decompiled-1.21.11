package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

/**
 * Удаляет пустой второй слот покупки ({@code buyB}) из торговли жителя,
 * если предмет является воздухом или имеет нулевое количество.
 */
public class EmptyItemInVillagerTradeFix extends DataFix {

	public EmptyItemInVillagerTradeFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	public TypeRewriteRule makeRule() {
		Type<?> tradeType = getInputSchema().getType(TypeReferences.VILLAGER_TRADE);

		return writeFixAndRead(
				"EmptyItemInVillagerTradeFix",
				tradeType,
				tradeType,
				trade -> {
					Dynamic<?> buyB = trade.get("buyB").orElseEmptyMap();
					String itemId = IdentifierNormalizingSchema.normalize(buyB.get("id").asString("minecraft:air"));
					int count = buyB.get("count").asInt(0);

					return "minecraft:air".equals(itemId) || count == 0
							? trade.remove("buyB")
							: trade;
				}
		);
	}
}
