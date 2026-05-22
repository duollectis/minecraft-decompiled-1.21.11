package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность статуи медного голема. Хранит пользовательское имя и позу,
 * а также управляет созданием живого голема при активации.
 */
public class CopperGolemStatueBlockEntity extends BlockEntity {

	public CopperGolemStatueBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.COPPER_GOLEM_STATUE, pos, state);
	}

	public void copyDataFrom(CopperGolemEntity copperGolemEntity) {
		setComponents(ComponentMap
				.builder()
				.addAll(getComponents())
				.add(DataComponentTypes.CUSTOM_NAME, copperGolemEntity.getCustomName())
				.build());
		super.markDirty();
	}

	/**
	 * Создаёт живого медного голема из данных статуи, позиционируя его
	 * в центре блока с направлением из {@link CopperGolemStatueBlock#FACING}.
	 */
	public @Nullable CopperGolemEntity createCopperGolem(BlockState state) {
		CopperGolemEntity golem = EntityType.COPPER_GOLEM.create(world, SpawnReason.TRIGGERED);
		if (golem == null) {
			return null;
		}

		golem.setCustomName(getComponents().get(DataComponentTypes.CUSTOM_NAME));
		return setupEntity(state, golem);
	}

	private CopperGolemEntity setupEntity(BlockState state, CopperGolemEntity entity) {
		BlockPos blockPos = getPos();
		entity.refreshPositionAndAngles(
				blockPos.toCenterPos().x,
				blockPos.getY(),
				blockPos.toCenterPos().z,
				state.get(CopperGolemStatueBlock.FACING).getPositiveHorizontalDegrees(),
				0.0F
		);
		entity.headYaw = entity.getYaw();
		entity.bodyYaw = entity.getYaw();
		entity.playSpawnSound();
		return entity;
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	public ItemStack withComponents(ItemStack stack, CopperGolemStatueBlock.Pose pose) {
		stack.applyComponentsFrom(createComponentMap());
		stack.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT.with(CopperGolemStatueBlock.POSE, pose));
		return stack;
	}
}
