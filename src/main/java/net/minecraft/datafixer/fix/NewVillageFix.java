package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList.CompoundListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code NewVillageFix}.
 */
public class NewVillageFix extends DataFix {

	public NewVillageFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	protected TypeRewriteRule makeRule() {
		CompoundListType<String, ?>
				compoundListType =
				DSL.compoundList(DSL.string(), this.getInputSchema().getType(TypeReferences.STRUCTURE_FEATURE));
		OpticFinder<? extends List<? extends Pair<String, ?>>> opticFinder = compoundListType.finder();
		return this.fix(compoundListType);
	}

	private <SF> TypeRewriteRule fix(CompoundListType<String, SF> compoundListType) {
		Type<?> type = this.getInputSchema().getType(TypeReferences.CHUNK);
		Type<?> type2 = this.getInputSchema().getType(TypeReferences.STRUCTURE_FEATURE);
		OpticFinder<?> opticFinder = type.findField("Level");
		OpticFinder<?> opticFinder2 = opticFinder.type().findField("Structures");
		OpticFinder<?> opticFinder3 = opticFinder2.type().findField("Starts");
		OpticFinder<List<Pair<String, SF>>> opticFinder4 = compoundListType.finder();
		return TypeRewriteRule.seq(
				this.fixTypeEverywhereTyped(
						"NewVillageFix",
						type,
						typed -> typed.updateTyped(
								opticFinder,
								typedx -> typedx.updateTyped(
										opticFinder2,
										typedxx -> typedxx.updateTyped(
												                  opticFinder3,
												                  typedxxx -> typedxxx.update(
														                  opticFinder4,
														                  list -> list.stream()
														                              .filter(pair -> !Objects.equals(
																                              pair.getFirst(),
																                              "Village"
														                              ))
														                              .map(pair -> pair.mapFirst(string ->
																                              string.equals("New_Village") ? "Village"
																                                                           : string))
														                              .collect(Collectors.toList())
												                  )
										                  )
										                  .update(
												                  DSL.remainderFinder(),
												                  dynamic -> dynamic.update(
														                  "References",
														                  dynamicx -> {
															                  Optional<? extends Dynamic<?>>
																	                  optional =
																	                  dynamicx
																			                  .get("New_Village")
																			                  .result();
															                  return ((Dynamic) DataFixUtils.orElse(
																	                  optional.map(dynamic2 -> dynamicx
																			                  .remove("New_Village")
																			                  .set(
																					                  "Village",
																					                  dynamic2
																			                  )),
																	                  dynamicx
															                  )
															                  )
																	                  .remove("Village");
														                  }
												                  )
										                  )
								)
						)
				),
				this.fixTypeEverywhereTyped(
						"NewVillageStartFix",
						type2,
						typed -> typed.update(
								DSL.remainderFinder(),
								dynamic -> dynamic.update(
										"id",
										dynamicx ->
												Objects.equals(
														IdentifierNormalizingSchema.normalize(dynamicx.asString("")),
														"minecraft:new_village"
												)
												? dynamicx.createString("minecraft:village")
												: dynamicx
								)
						)
				)
		);
	}
}
