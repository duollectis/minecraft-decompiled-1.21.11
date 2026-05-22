package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Конвертирует раздельные координатные поля сущностей и игрока в компактный
 * формат {@code BlockPos}: объединяет {@code SleepingX/Y/Z} → {@code sleeping_pos},
 * {@code SpawnX/Y/Z} → {@code respawn.pos}, {@code BoundX/Y/Z} → {@code bound_pos}
 * (Vex), {@code AX/AY/AZ} → {@code anchor_pos} (Phantom), {@code HomePosX/Y/Z} →
 * {@code home_pos} (Turtle), {@code TileX/Y/Z} → {@code block_pos} (декорации).
 */
public class InlineBlockPosFormatFix extends DataFix {

	public InlineBlockPosFormatFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	public TypeRewriteRule makeRule() {
		OpticFinder<?> vexFinder = getEntityFinder("minecraft:vex");
		OpticFinder<?> phantomFinder = getEntityFinder("minecraft:phantom");
		OpticFinder<?> turtleFinder = getEntityFinder("minecraft:turtle");

		List<OpticFinder<?>> decorationFinders = List.of(
			getEntityFinder("minecraft:item_frame"),
			getEntityFinder("minecraft:glow_item_frame"),
			getEntityFinder("minecraft:painting"),
			getEntityFinder("minecraft:leash_knot")
		);

		return TypeRewriteRule.seq(
			fixTypeEverywhereTyped(
				"InlineBlockPosFormatFix - player",
				getInputSchema().getType(TypeReferences.PLAYER),
				player -> player.update(DSL.remainderFinder(), this::fixPlayerFields)
			),
			fixTypeEverywhereTyped(
				"InlineBlockPosFormatFix - entity",
				getInputSchema().getType(TypeReferences.ENTITY),
				entity -> {
					entity = entity.update(DSL.remainderFinder(), this::fixSleeping)
						.updateTyped(vexFinder, vex -> vex.update(DSL.remainderFinder(), this::fixVexFields))
						.updateTyped(phantomFinder, phantom -> phantom.update(DSL.remainderFinder(), this::fixPhantomFields))
						.updateTyped(turtleFinder, turtle -> turtle.update(DSL.remainderFinder(), this::fixTurtleFields));

					for (OpticFinder<?> decorationFinder : decorationFinders) {
						entity = entity.updateTyped(
							decorationFinder,
							decoration -> decoration.update(DSL.remainderFinder(), this::fixDecorationFields)
						);
					}

					return entity;
				}
			)
		);
	}

	private OpticFinder<?> getEntityFinder(String entityId) {
		return DSL.namedChoice(entityId, getInputSchema().getChoiceType(TypeReferences.ENTITY, entityId));
	}

	private Dynamic<?> fixPlayerFields(Dynamic<?> player) {
		player = fixSleeping(player);

		Optional<Number> spawnX = player.get("SpawnX").asNumber().result();
		Optional<Number> spawnY = player.get("SpawnY").asNumber().result();
		Optional<Number> spawnZ = player.get("SpawnZ").asNumber().result();

		if (spawnX.isPresent() && spawnY.isPresent() && spawnZ.isPresent()) {
			Dynamic<?> respawn = player.createMap(
				Map.of(
					player.createString("pos"),
					FixUtil.createBlockPos(
						player,
						spawnX.get().intValue(),
						spawnY.get().intValue(),
						spawnZ.get().intValue()
					)
				)
			);

			respawn = Dynamic.copyField(player, "SpawnAngle", respawn, "angle");
			respawn = Dynamic.copyField(player, "SpawnDimension", respawn, "dimension");
			respawn = Dynamic.copyField(player, "SpawnForced", respawn, "forced");

			player = player
				.remove("SpawnX").remove("SpawnY").remove("SpawnZ")
				.remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced")
				.set("respawn", respawn);
		}

		Optional<? extends Dynamic<?>> netherPos = player.get("enteredNetherPosition").result();

		if (netherPos.isPresent()) {
			Dynamic<?> pos = netherPos.get();
			player = player.remove("enteredNetherPosition").set(
				"entered_nether_pos",
				player.createList(Stream.of(
					player.createDouble(pos.get("x").asDouble(0.0)),
					player.createDouble(pos.get("y").asDouble(0.0)),
					player.createDouble(pos.get("z").asDouble(0.0))
				))
			);
		}

		return player;
	}

	private Dynamic<?> fixSleeping(Dynamic<?> entity) {
		return FixUtil.consolidateBlockPos(entity, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
	}

	private Dynamic<?> fixVexFields(Dynamic<?> vex) {
		return FixUtil.consolidateBlockPos(
			vex.renameField("LifeTicks", "life_ticks"),
			"BoundX", "BoundY", "BoundZ", "bound_pos"
		);
	}

	private Dynamic<?> fixPhantomFields(Dynamic<?> phantom) {
		return FixUtil.consolidateBlockPos(
			phantom.renameField("Size", "size"),
			"AX", "AY", "AZ", "anchor_pos"
		);
	}

	private Dynamic<?> fixTurtleFields(Dynamic<?> turtle) {
		turtle = turtle.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
		turtle = FixUtil.consolidateBlockPos(turtle, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
		return turtle.renameField("HasEgg", "has_egg");
	}

	private Dynamic<?> fixDecorationFields(Dynamic<?> decoration) {
		return FixUtil.consolidateBlockPos(decoration, "TileX", "TileY", "TileZ", "block_pos");
	}
}
