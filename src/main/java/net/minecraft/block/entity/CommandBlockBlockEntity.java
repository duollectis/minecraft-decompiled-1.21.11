package net.minecraft.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.CommandBlockExecutor;

/**
 * Блок-сущность командного блока. Хранит состояние питания, режим авто-выполнения
 * и результат проверки условия для цепочечных командных блоков.
 */
public class CommandBlockBlockEntity extends BlockEntity {

	private boolean powered;
	private boolean auto;
	private boolean conditionMet;
	private final CommandBlockExecutor commandExecutor = new CommandBlockExecutor() {
		@Override
		public void setCommand(String command) {
			super.setCommand(command);
			CommandBlockBlockEntity.this.markDirty();
		}

		@Override
		public void markDirty(ServerWorld world) {
			BlockState blockState = world.getBlockState(CommandBlockBlockEntity.this.pos);
			world.updateListeners(CommandBlockBlockEntity.this.pos, blockState, blockState, 3);
		}

		@Override
		public ServerCommandSource getSource(ServerWorld world, CommandOutput output) {
			Direction direction = CommandBlockBlockEntity.this.getCachedState().get(CommandBlock.FACING);
			return new ServerCommandSource(
					output,
					Vec3d.ofCenter(CommandBlockBlockEntity.this.pos),
					new Vec2f(0.0F, direction.getPositiveHorizontalDegrees()),
					world,
					LeveledPermissionPredicate.GAMEMASTERS,
					this.getName().getString(),
					this.getName(),
					world.getServer(),
					null
			);
		}

		@Override
		public boolean isEditable() {
			return !CommandBlockBlockEntity.this.isRemoved();
		}
	};

	public CommandBlockBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.COMMAND_BLOCK, pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		commandExecutor.writeData(view);
		view.putBoolean("powered", isPowered());
		view.putBoolean("conditionMet", isConditionMet());
		view.putBoolean("auto", isAuto());
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		commandExecutor.readData(view);
		powered = view.getBoolean("powered", false);
		conditionMet = view.getBoolean("conditionMet", false);
		setAuto(view.getBoolean("auto", false));
	}

	public CommandBlockExecutor getCommandExecutor() {
		return commandExecutor;
	}

	public void setPowered(boolean powered) {
		this.powered = powered;
	}

	public boolean isPowered() {
		return powered;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		boolean wasAuto = this.auto;
		this.auto = auto;
		if (!wasAuto && auto && !powered && world != null
				&& getCommandBlockType() != Type.SEQUENCE
		) {
			scheduleAutoTick();
		}
	}

	/**
	 * Планирует авто-тик для повторяющегося командного блока, если он активен
	 * (получает питание или работает в режиме авто).
	 */
	public void updateCommandBlock() {
		if (getCommandBlockType() == Type.AUTO && (powered || auto) && world != null) {
			scheduleAutoTick();
		}
	}

	private void scheduleAutoTick() {
		Block block = getCachedState().getBlock();
		if (block instanceof CommandBlock) {
			updateConditionMet();
			world.scheduleBlockTick(pos, block, 1);
		}
	}

	public boolean isConditionMet() {
		return conditionMet;
	}

	/**
	 * Проверяет, выполнено ли условие для цепочечного командного блока:
	 * предыдущий блок в цепочке должен был успешно выполнить команду.
	 */
	public boolean updateConditionMet() {
		conditionMet = true;
		if (isConditionalCommandBlock()) {
			BlockPos prevPos = pos.offset(world.getBlockState(pos).get(CommandBlock.FACING).getOpposite());
			if (world.getBlockState(prevPos).getBlock() instanceof CommandBlock) {
				conditionMet = world.getBlockEntity(prevPos) instanceof CommandBlockBlockEntity prevBlock
						&& prevBlock.getCommandExecutor().getSuccessCount() > 0;
			} else {
				conditionMet = false;
			}
		}

		return conditionMet;
	}

	public Type getCommandBlockType() {
		BlockState blockState = getCachedState();
		if (blockState.isOf(Blocks.COMMAND_BLOCK)) {
			return Type.REDSTONE;
		} else if (blockState.isOf(Blocks.REPEATING_COMMAND_BLOCK)) {
			return Type.AUTO;
		}

		return blockState.isOf(Blocks.CHAIN_COMMAND_BLOCK) ? Type.SEQUENCE : Type.REDSTONE;
	}

	public boolean isConditionalCommandBlock() {
		BlockState blockState = world.getBlockState(getPos());
		return blockState.getBlock() instanceof CommandBlock
				? blockState.get(CommandBlock.CONDITIONAL)
				: false;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		commandExecutor.setCustomName(components.get(DataComponentTypes.CUSTOM_NAME));
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CUSTOM_NAME, commandExecutor.getCustomName());
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		super.removeFromCopiedStackData(view);
		view.remove("CustomName");
		view.remove("conditionMet");
		view.remove("powered");
	}

	public enum Type {
		SEQUENCE,
		AUTO,
		REDSTONE
	}
}
