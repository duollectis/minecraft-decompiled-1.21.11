package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Мигрирует UUID владельца снарядов из различных старых форматов (плоские поля Most/Least,
 * вложенный объект Owner, поле owner с M/L) в единый формат массива int[4] {@code OwnerUUID}.
 */
public class EntityProjectileOwnerFix extends DataFix {

	public EntityProjectileOwnerFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"EntityProjectileOwner",
				getInputSchema().getType(TypeReferences.ENTITY),
				this::fixEntities
		);
	}

	private Typed<?> fixEntities(Typed<?> entityTyped) {
		entityTyped = applyFix(entityTyped, "minecraft:egg", this::moveOwnerToArray);
		entityTyped = applyFix(entityTyped, "minecraft:ender_pearl", this::moveOwnerToArray);
		entityTyped = applyFix(entityTyped, "minecraft:experience_bottle", this::moveOwnerToArray);
		entityTyped = applyFix(entityTyped, "minecraft:snowball", this::moveOwnerToArray);
		entityTyped = applyFix(entityTyped, "minecraft:potion", this::moveOwnerToArray);
		entityTyped = applyFix(entityTyped, "minecraft:llama_spit", this::moveNestedOwnerMostLeastToArray);
		entityTyped = applyFix(entityTyped, "minecraft:arrow", this::moveFlatOwnerMostLeastToArray);
		entityTyped = applyFix(entityTyped, "minecraft:spectral_arrow", this::moveFlatOwnerMostLeastToArray);

		return applyFix(entityTyped, "minecraft:trident", this::moveFlatOwnerMostLeastToArray);
	}

	private Dynamic<?> moveFlatOwnerMostLeastToArray(Dynamic<?> entity) {
		long mostBits = entity.get("OwnerUUIDMost").asLong(0L);
		long leastBits = entity.get("OwnerUUIDLeast").asLong(0L);

		return insertOwnerUuidArray(entity, mostBits, leastBits)
				.remove("OwnerUUIDMost")
				.remove("OwnerUUIDLeast");
	}

	private Dynamic<?> moveNestedOwnerMostLeastToArray(Dynamic<?> entity) {
		OptionalDynamic<?> ownerData = entity.get("Owner");
		long mostBits = ownerData.get("OwnerUUIDMost").asLong(0L);
		long leastBits = ownerData.get("OwnerUUIDLeast").asLong(0L);

		return insertOwnerUuidArray(entity, mostBits, leastBits).remove("Owner");
	}

	private Dynamic<?> moveOwnerToArray(Dynamic<?> entity) {
		OptionalDynamic<?> ownerData = entity.get("owner");
		long mostBits = ownerData.get("M").asLong(0L);
		long leastBits = ownerData.get("L").asLong(0L);

		return insertOwnerUuidArray(entity, mostBits, leastBits).remove("owner");
	}

	private Dynamic<?> insertOwnerUuidArray(Dynamic<?> entity, long mostBits, long leastBits) {
		if (mostBits == 0L || leastBits == 0L) {
			return entity;
		}

		return entity.set(
				"OwnerUUID",
				entity.createIntList(Arrays.stream(makeUuidArray(mostBits, leastBits)))
		);
	}

	private static int[] makeUuidArray(long mostBits, long leastBits) {
		return new int[]{(int) (mostBits >> 32), (int) mostBits, (int) (leastBits >> 32), (int) leastBits};
	}

	private Typed<?> applyFix(Typed<?> entityTyped, String entityId, Function<Dynamic<?>, Dynamic<?>> fixer) {
		Type<?> inputType = getInputSchema().getChoiceType(TypeReferences.ENTITY, entityId);
		Type<?> outputType = getOutputSchema().getChoiceType(TypeReferences.ENTITY, entityId);

		return entityTyped.updateTyped(
				DSL.namedChoice(entityId, inputType),
				outputType,
				typed -> typed.update(DSL.remainderFinder(), fixer)
		);
	}
}
