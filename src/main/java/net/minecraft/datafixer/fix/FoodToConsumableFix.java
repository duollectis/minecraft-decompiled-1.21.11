package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@code FoodToConsumableFix}.
 */
public class FoodToConsumableFix extends DataFix {

	public FoodToConsumableFix(Schema schema) {
		super(schema, true);
	}

	protected TypeRewriteRule makeRule() {
		return this.writeFixAndRead(
				"Food to consumable fix",
				this.getInputSchema().getType(TypeReferences.DATA_COMPONENTS),
				this.getOutputSchema().getType(TypeReferences.DATA_COMPONENTS),
				dynamic -> {
					Optional<? extends Dynamic<?>> optional = dynamic.get("minecraft:food").result();
					if (optional.isPresent()) {
						float f = optional.get().get("eat_seconds").asFloat(1.6F);
						Stream<? extends Dynamic<?>> stream = optional.get().get("effects").asStream();
						Stream<? extends Dynamic<?>> stream2 = stream.map(
								dynamicx -> dynamicx.emptyMap()
								                    .set("type", dynamicx.createString("minecraft:apply_effects"))
								                    .set("effects",
										                    dynamicx.createList(dynamicx
												                    .get("effect")
												                    .result()
												                    .stream())
								                    )
								                    .set("probability",
										                    dynamicx.createFloat(dynamicx
												                    .get("probability")
												                    .asFloat(1.0F))
								                    )
						);
						dynamic =
								Dynamic.copyField(
										optional.get(),
										"using_converts_to",
										dynamic,
										"minecraft:use_remainder"
								);
						dynamic =
								dynamic.set(
										"minecraft:food",
										optional
												.get()
												.remove("eat_seconds")
												.remove("effects")
												.remove("using_converts_to")
								);
						return dynamic.set(
								"minecraft:consumable",
								dynamic
										.emptyMap()
										.set("consume_seconds", dynamic.createFloat(f))
										.set("on_consume_effects", dynamic.createList(stream2))
						);
					}
					else {
						return dynamic;
					}
				}
		);
	}
}
