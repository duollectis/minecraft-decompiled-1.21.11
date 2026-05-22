package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность стола зачарований. Управляет анимацией книги: поворотом к ближайшему игроку,
 * перелистыванием страниц и плавным вращением.
 */
public class EnchantingTableBlockEntity extends BlockEntity implements Nameable {

	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.enchant");
	private static final Random RANDOM = Random.create();
	private static final float PLAYER_DETECTION_RADIUS = 3.0F;
	private static final float BOOK_ROTATION_LERP_FACTOR = 0.4F;
	private static final float PAGE_FLIP_LERP_FACTOR = 0.9F;
	private static final float PAGE_FLIP_CLAMP = 0.2F;
	private static final float PAGE_FLIP_SPEED = 0.4F;
	private static final float TURNING_SPEED_STEP = 0.1F;
	private static final float IDLE_ROTATION_SPEED = 0.02F;
	private static final int PAGE_FLIP_RANDOM_INTERVAL = 40;

	public int ticks;
	public float nextPageAngle;
	public float pageAngle;
	public float flipRandom;
	public float flipTurn;
	public float nextPageTurningSpeed;
	public float pageTurningSpeed;
	public float bookRotation;
	public float lastBookRotation;
	public float targetBookRotation;
	private @Nullable Text customName;

	public EnchantingTableBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.ENCHANTING_TABLE, pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putNullable("CustomName", TextCodecs.CODEC, customName);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		customName = tryParseCustomName(view, "CustomName");
	}

	/**
	 * Обновляет анимацию книги: поворот к ближайшему игроку, перелистывание страниц.
	 * Нормализует углы в диапазон [-π, π] для корректной интерполяции.
	 */
	public static void tick(World world, BlockPos pos, BlockState state, EnchantingTableBlockEntity blockEntity) {
		blockEntity.pageTurningSpeed = blockEntity.nextPageTurningSpeed;
		blockEntity.lastBookRotation = blockEntity.bookRotation;

		PlayerEntity nearestPlayer = world.getClosestPlayer(
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, PLAYER_DETECTION_RADIUS, false
		);

		if (nearestPlayer != null) {
			double dx = nearestPlayer.getX() - (pos.getX() + 0.5);
			double dz = nearestPlayer.getZ() - (pos.getZ() + 0.5);
			blockEntity.targetBookRotation = (float) MathHelper.atan2(dz, dx);
			blockEntity.nextPageTurningSpeed += TURNING_SPEED_STEP;

			if (blockEntity.nextPageTurningSpeed < 0.5F || RANDOM.nextInt(PAGE_FLIP_RANDOM_INTERVAL) == 0) {
				float prevFlipRandom = blockEntity.flipRandom;
				do {
					blockEntity.flipRandom += RANDOM.nextInt(4) - RANDOM.nextInt(4);
				} while (prevFlipRandom == blockEntity.flipRandom);
			}
		} else {
			blockEntity.targetBookRotation += IDLE_ROTATION_SPEED;
			blockEntity.nextPageTurningSpeed -= TURNING_SPEED_STEP;
		}

		while (blockEntity.bookRotation >= (float) Math.PI) {
			blockEntity.bookRotation -= (float) (Math.PI * 2);
		}

		while (blockEntity.bookRotation < (float) -Math.PI) {
			blockEntity.bookRotation += (float) (Math.PI * 2);
		}

		while (blockEntity.targetBookRotation >= (float) Math.PI) {
			blockEntity.targetBookRotation -= (float) (Math.PI * 2);
		}

		while (blockEntity.targetBookRotation < (float) -Math.PI) {
			blockEntity.targetBookRotation += (float) (Math.PI * 2);
		}

		float rotationDelta = blockEntity.targetBookRotation - blockEntity.bookRotation;

		while (rotationDelta >= (float) Math.PI) {
			rotationDelta -= (float) (Math.PI * 2);
		}

		while (rotationDelta < (float) -Math.PI) {
			rotationDelta += (float) (Math.PI * 2);
		}

		blockEntity.bookRotation += rotationDelta * BOOK_ROTATION_LERP_FACTOR;
		blockEntity.nextPageTurningSpeed = MathHelper.clamp(blockEntity.nextPageTurningSpeed, 0.0F, 1.0F);
		blockEntity.ticks++;
		blockEntity.pageAngle = blockEntity.nextPageAngle;

		float pageFlipDelta = MathHelper.clamp(
				(blockEntity.flipRandom - blockEntity.nextPageAngle) * PAGE_FLIP_SPEED,
				-PAGE_FLIP_CLAMP,
				PAGE_FLIP_CLAMP
		);
		blockEntity.flipTurn += (pageFlipDelta - blockEntity.flipTurn) * PAGE_FLIP_LERP_FACTOR;
		blockEntity.nextPageAngle += blockEntity.flipTurn;
	}

	@Override
	public Text getName() {
		return customName != null ? customName : CONTAINER_NAME_TEXT;
	}

	public void setCustomName(@Nullable Text customName) {
		this.customName = customName;
	}

	@Override
	public @Nullable Text getCustomName() {
		return customName;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		customName = components.get(DataComponentTypes.CUSTOM_NAME);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CUSTOM_NAME, customName);
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("CustomName");
	}
}
