package net.minecraft.entity.damage;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Место падения сущности, используемое для выбора варианта сообщения о смерти
 * при падении (лестница, лоза, строительные леса, вода и т.д.).
 */
public record FallLocation(String id) {

	public static final FallLocation GENERIC = new FallLocation("generic");
	public static final FallLocation LADDER = new FallLocation("ladder");
	public static final FallLocation VINES = new FallLocation("vines");
	public static final FallLocation WEEPING_VINES = new FallLocation("weeping_vines");
	public static final FallLocation TWISTING_VINES = new FallLocation("twisting_vines");
	public static final FallLocation SCAFFOLDING = new FallLocation("scaffolding");
	public static final FallLocation OTHER_CLIMBABLE = new FallLocation("other_climbable");
	public static final FallLocation WATER = new FallLocation("water");

	/**
	 * Определяет место падения по состоянию блока, на котором находилась сущность.
	 *
	 * @param state состояние блока
	 * @return соответствующее место падения
	 */
	public static FallLocation fromBlockState(BlockState state) {
		if (state.isOf(Blocks.LADDER) || state.isIn(BlockTags.TRAPDOORS)) {
			return LADDER;
		}

		if (state.isOf(Blocks.VINE)) {
			return VINES;
		}

		if (state.isOf(Blocks.WEEPING_VINES) || state.isOf(Blocks.WEEPING_VINES_PLANT)) {
			return WEEPING_VINES;
		}

		if (state.isOf(Blocks.TWISTING_VINES) || state.isOf(Blocks.TWISTING_VINES_PLANT)) {
			return TWISTING_VINES;
		}

		return state.isOf(Blocks.SCAFFOLDING) ? SCAFFOLDING : OTHER_CLIMBABLE;
	}

	/**
	 * Определяет место падения для живой сущности: проверяет блок карабкания
	 * или контакт с водой. Возвращает {@code null}, если сущность падает в воздухе.
	 *
	 * @param entity сущность, для которой определяется место падения
	 * @return место падения или {@code null}
	 */
	public static @Nullable FallLocation fromEntity(LivingEntity entity) {
		Optional<BlockPos> climbingPos = entity.getClimbingPos();
		if (climbingPos.isPresent()) {
			BlockState blockState = entity.getEntityWorld().getBlockState(climbingPos.get());
			return fromBlockState(blockState);
		}

		return entity.isTouchingWater() ? WATER : null;
	}

	public String getDeathMessageKey() {
		return "death.fell.accident." + id;
	}
}
