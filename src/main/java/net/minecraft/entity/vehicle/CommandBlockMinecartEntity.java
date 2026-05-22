package net.minecraft.entity.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.CommandBlockExecutor;
import net.minecraft.world.World;

/**
 * Вагонетка с командным блоком — выполняет команду при проезде по активированному рельсу.
 *
 * <p>Команда и последний вывод синхронизируются через {@link DataTracker} для отображения
 * на клиенте. Выполнение команды ограничено кулдауном {@value #EXECUTE_TICK_COOLDOWN} тиков,
 * чтобы предотвратить многократный запуск на одном рельсе за один проезд.
 */
public class CommandBlockMinecartEntity extends AbstractMinecartEntity {

	private static final int EXECUTE_TICK_COOLDOWN = 4;

	static final TrackedData<String> COMMAND = DataTracker.registerData(
		CommandBlockMinecartEntity.class,
		TrackedDataHandlerRegistry.STRING
	);
	static final TrackedData<Text> LAST_OUTPUT = DataTracker.registerData(
		CommandBlockMinecartEntity.class,
		TrackedDataHandlerRegistry.TEXT_COMPONENT
	);

	private final CommandBlockExecutor commandExecutor = new CommandExecutor();
	private int lastExecuted;

	public CommandBlockMinecartEntity(EntityType<? extends CommandBlockMinecartEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected Item asItem() {
		return Items.MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.COMMAND_BLOCK_MINECART);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(COMMAND, "");
		builder.add(LAST_OUTPUT, ScreenTexts.EMPTY);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		commandExecutor.readData(view);
		getDataTracker().set(COMMAND, getCommandExecutor().getCommand());
		getDataTracker().set(LAST_OUTPUT, getCommandExecutor().getLastOutput());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		commandExecutor.writeData(view);
	}

	@Override
	public BlockState getDefaultContainedBlock() {
		return Blocks.COMMAND_BLOCK.getDefaultState();
	}

	public CommandBlockExecutor getCommandExecutor() {
		return commandExecutor;
	}

	/**
	 * Выполняет команду при проезде по активированному рельсу.
	 *
	 * <p>Кулдаун {@value #EXECUTE_TICK_COOLDOWN} тиков предотвращает повторное выполнение
	 * при медленном движении вагонетки над одним и тем же рельсом.
	 */
	@Override
	public void onActivatorRail(ServerWorld serverWorld, int x, int y, int z, boolean powered) {
		if (powered && age - lastExecuted >= EXECUTE_TICK_COOLDOWN) {
			getCommandExecutor().execute(serverWorld);
			lastExecuted = age;
		}
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (player.isCreativeLevelTwoOp()) {
			if (player.getEntityWorld().isClient()) {
				player.openCommandBlockMinecartScreen(this);
			}

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	/**
	 * Синхронизирует состояние командного блока при изменении отслеживаемых данных.
	 *
	 * <p>Обновление {@code LAST_OUTPUT} обёрнуто в try-catch, так как десериализация
	 * текстового компонента может завершиться ошибкой при получении некорректных данных от сервера.
	 */
	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);

		if (LAST_OUTPUT.equals(data)) {
			try {
				commandExecutor.setLastOutput(getDataTracker().get(LAST_OUTPUT));
			} catch (Throwable throwable) {
				// Некорректный текстовый компонент от сервера — игнорируем, чтобы не крашить клиент
			}
		} else if (COMMAND.equals(data)) {
			commandExecutor.setCommand(getDataTracker().get(COMMAND));
		}
	}

	/**
	 * Реализация {@link CommandBlockExecutor} для вагонетки с командным блоком.
	 * Синхронизирует команду и вывод через {@link DataTracker} родительской сущности.
	 */
	class CommandExecutor extends CommandBlockExecutor {

		@Override
		public void markDirty(ServerWorld world) {
			CommandBlockMinecartEntity.this.getDataTracker().set(COMMAND, getCommand());
			CommandBlockMinecartEntity.this.getDataTracker().set(LAST_OUTPUT, getLastOutput());
		}

		@Override
		public ServerCommandSource getSource(ServerWorld world, CommandOutput output) {
			return new ServerCommandSource(
				output,
				CommandBlockMinecartEntity.this.getEntityPos(),
				CommandBlockMinecartEntity.this.getRotationClient(),
				world,
				LeveledPermissionPredicate.GAMEMASTERS,
				getName().getString(),
				CommandBlockMinecartEntity.this.getDisplayName(),
				world.getServer(),
				CommandBlockMinecartEntity.this
			);
		}

		@Override
		public boolean isEditable() {
			return !CommandBlockMinecartEntity.this.isRemoved();
		}
	}
}
