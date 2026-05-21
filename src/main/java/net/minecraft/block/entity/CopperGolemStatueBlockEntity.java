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
 * {@code CopperGolemStatueBlockEntity}.
 */
public class CopperGolemStatueBlockEntity extends BlockEntity {

	public CopperGolemStatueBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.COPPER_GOLEM_STATUE, pos, state);
	}

	/**
	 * Создаёт копию data from.
	 *
	 * @param copperGolemEntity copper golem entity
	 */
	public void copyDataFrom(CopperGolemEntity copperGolemEntity) {
		this.setComponents(ComponentMap
				.builder()
				.addAll(this.getComponents())
				.add(DataComponentTypes.CUSTOM_NAME, copperGolemEntity.getCustomName())
				.build());
		super.markDirty();
	}

	/**
	 * Создаёт copper golem.
	 *
	 * @param state state
	 *
	 * @return @Nullable CopperGolemEntity — результат операции
	 */
	public @Nullable CopperGolemEntity createCopperGolem(BlockState state) {
		CopperGolemEntity copperGolemEntity = EntityType.COPPER_GOLEM.create(this.world, SpawnReason.TRIGGERED);
		if (copperGolemEntity != null) {
			copperGolemEntity.setCustomName(this.getComponents().get(DataComponentTypes.CUSTOM_NAME));
			return this.setupEntity(state, copperGolemEntity);
		}
		else {
			return null;
		}
	}

	private CopperGolemEntity setupEntity(BlockState state, CopperGolemEntity entity) {
		BlockPos blockPos = this.getPos();
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

	/**
	 * To update packet.
	 *
	 * @return BlockEntityUpdateS2CPacket — результат операции
	 */
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	/**
	 * With components.
	 *
	 * @param stack stack
	 * @param pose pose
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack withComponents(ItemStack stack, CopperGolemStatueBlock.Pose pose) {
		stack.applyComponentsFrom(this.createComponentMap());
		stack.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT.with(CopperGolemStatueBlock.POSE, pose));
		return stack;
	}
}
