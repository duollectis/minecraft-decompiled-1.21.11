package net.minecraft.block.enums;

import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.vault.VaultConfig;
import net.minecraft.block.vault.VaultServerData;
import net.minecraft.block.vault.VaultSharedData;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Состояние блока хранилища (Vault).
 * Управляет жизненным циклом хранилища: от ожидания игроков до выброса наград.
 */
public enum VaultState implements StringIdentifiable {

	/**
	 * Хранилище неактивно — поблизости нет подходящих игроков.
	 * Очищает отображаемый предмет и посылает событие деактивации (3016).
	 */
	INACTIVE("inactive", VaultState.Light.HALF_LIT) {
		@Override
		protected void onChangedTo(
				ServerWorld world,
				BlockPos pos,
				VaultConfig config,
				VaultSharedData sharedData,
				boolean ominous
		) {
			sharedData.setDisplayItem(ItemStack.EMPTY);
			world.syncWorldEvent(3016, pos, ominous ? 1 : 0);
		}
	},

	/**
	 * Хранилище активно — рядом есть игрок, который ещё не получал награду.
	 * Обновляет отображаемый предмет и посылает событие активации (3015).
	 */
	ACTIVE("active", VaultState.Light.LIT) {
		@Override
		protected void onChangedTo(
				ServerWorld world,
				BlockPos pos,
				VaultConfig config,
				VaultSharedData sharedData,
				boolean ominous
		) {
			if (sharedData.hasDisplayItem() == false) {
				VaultBlockEntity.Server.updateDisplayItem(world, this, config, sharedData, pos);
			}

			world.syncWorldEvent(3015, pos, ominous ? 1 : 0);
		}
	},

	/**
	 * Хранилище разблокируется — игрок вставил ключ.
	 * Воспроизводит звук вставки предмета.
	 */
	UNLOCKING("unlocking", VaultState.Light.LIT) {
		@Override
		protected void onChangedTo(
				ServerWorld world,
				BlockPos pos,
				VaultConfig config,
				VaultSharedData sharedData,
				boolean ominous
		) {
			world.playSound(null, pos, SoundEvents.BLOCK_VAULT_INSERT_ITEM, SoundCategory.BLOCKS);
		}
	},

	/**
	 * Хранилище выбрасывает награды.
	 * При входе в состояние открывает заслонку, при выходе — закрывает.
	 */
	EJECTING("ejecting", VaultState.Light.LIT) {
		@Override
		protected void onChangedTo(
				ServerWorld world,
				BlockPos pos,
				VaultConfig config,
				VaultSharedData sharedData,
				boolean ominous
		) {
			world.playSound(null, pos, SoundEvents.BLOCK_VAULT_OPEN_SHUTTER, SoundCategory.BLOCKS);
		}

		@Override
		protected void onChangedFrom(ServerWorld world, BlockPos pos, VaultConfig config, VaultSharedData sharedData) {
			world.playSound(null, pos, SoundEvents.BLOCK_VAULT_CLOSE_SHUTTER, SoundCategory.BLOCKS);
		}
	};

	private static final int UNLOCKING_TO_EJECTING_DELAY = 20;
	private static final int EJECTING_ITEM_DELAY = 20;
	private static final int ACTIVE_STATE_UPDATE_DELAY = 20;
	private static final int INACTIVE_STATE_UPDATE_DELAY = 20;

	private final String id;
	private final VaultState.Light light;

	VaultState(final String id, final VaultState.Light light) {
		this.id = id;
		this.light = light;
	}

	@Override
	public String asString() {
		return id;
	}

	public int getLuminance() {
		return light.luminance;
	}

	/**
	 * Вычисляет следующее состояние хранилища на основе текущего и данных сервера.
	 * Управляет полным циклом: активация → разблокировка → выброс наград → деактивация.
	 *
	 * @param world      серверный мир
	 * @param pos        позиция блока хранилища
	 * @param config     конфигурация хранилища (радиусы, лут-таблицы)
	 * @param serverData серверные данные (очередь предметов, таймеры)
	 * @param sharedData общие данные (отображаемый предмет, список игроков)
	 * @return следующее состояние хранилища
	 */
	public VaultState update(
			ServerWorld world,
			BlockPos pos,
			VaultConfig config,
			VaultServerData serverData,
			VaultSharedData sharedData
	) {
		return switch (this) {
			case INACTIVE -> updateActiveState(world, pos, config, serverData, sharedData, config.activationRange());
			case ACTIVE -> updateActiveState(world, pos, config, serverData, sharedData, config.deactivationRange());
			case UNLOCKING -> {
				serverData.setStateUpdatingResumeTime(world.getTime() + UNLOCKING_TO_EJECTING_DELAY);
				yield EJECTING;
			}
			case EJECTING -> {
				if (serverData.getItemsToEject().isEmpty()) {
					serverData.finishEjecting();
					yield updateActiveState(world, pos, config, serverData, sharedData, config.deactivationRange());
				}

				float pitchModifier = serverData.getEjectSoundPitchModifier();
				ejectItem(world, pos, serverData.getItemToEject(), pitchModifier);
				sharedData.setDisplayItem(serverData.getItemToDisplay());

				// Задержка одинакова для последнего и промежуточных предметов — намеренное поведение ванилы
				serverData.setStateUpdatingResumeTime(world.getTime() + EJECTING_ITEM_DELAY);
				yield EJECTING;
			}
		};
	}

	private static VaultState updateActiveState(
			ServerWorld world,
			BlockPos pos,
			VaultConfig config,
			VaultServerData serverData,
			VaultSharedData sharedData,
			double radius
	) {
		sharedData.updateConnectedPlayers(world, pos, serverData, config, radius);
		serverData.setStateUpdatingResumeTime(world.getTime() + ACTIVE_STATE_UPDATE_DELAY);
		return sharedData.hasConnectedPlayers() ? ACTIVE : INACTIVE;
	}

	/**
	 * Уведомляет текущее и новое состояния о переходе между ними.
	 * Вызывает {@link #onChangedFrom} у текущего и {@link #onChangedTo} у нового состояния.
	 *
	 * @param world     серверный мир
	 * @param pos       позиция блока
	 * @param newState  новое состояние, в которое переходит хранилище
	 * @param config    конфигурация хранилища
	 * @param sharedData общие данные хранилища
	 * @param ominous   является ли хранилище зловещим (ominous vault)
	 */
	public void onStateChange(
			ServerWorld world,
			BlockPos pos,
			VaultState newState,
			VaultConfig config,
			VaultSharedData sharedData,
			boolean ominous
	) {
		onChangedFrom(world, pos, config, sharedData);
		newState.onChangedTo(world, pos, config, sharedData, ominous);
	}

	protected void onChangedTo(
			ServerWorld world,
			BlockPos pos,
			VaultConfig config,
			VaultSharedData sharedData,
			boolean ominous
	) {
	}

	protected void onChangedFrom(ServerWorld world, BlockPos pos, VaultConfig config, VaultSharedData sharedData) {
	}

	private void ejectItem(ServerWorld world, BlockPos pos, ItemStack stack, float pitchModifier) {
		ItemDispenserBehavior.spawnItem(
				world,
				stack,
				2,
				Direction.UP,
				Vec3d.ofBottomCenter(pos).offset(Direction.UP, 1.2)
		);
		world.syncWorldEvent(3017, pos, 0);
		world.playSound(
				null,
				pos,
				SoundEvents.BLOCK_VAULT_EJECT_ITEM,
				SoundCategory.BLOCKS,
				1.0F,
				0.8F + 0.4F * pitchModifier
		);
	}

	/** Уровень освещённости блока хранилища в зависимости от состояния. */
	enum Light {
		/** Половинная яркость — хранилище неактивно. */
		HALF_LIT(6),
		/** Полная яркость — хранилище активно или выбрасывает предметы. */
		LIT(12);

		final int luminance;

		Light(final int luminance) {
			this.luminance = luminance;
		}
	}
}
