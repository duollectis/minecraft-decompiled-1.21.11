package net.minecraft.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Предмет «Зелье». Поддерживает использование водяного зелья на блоках
 * из тега {@link net.minecraft.registry.tag.BlockTags#CONVERTABLE_TO_MUD}
 * для превращения их в грязь. Название предмета формируется динамически
 * из компонента {@link DataComponentTypes#POTION_CONTENTS}.
 */
public class PotionItem extends Item {

	private static final int MUD_SPLASH_PARTICLE_COUNT = 5;

	public PotionItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack getDefaultStack() {
		ItemStack stack = super.getDefaultStack();
		stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.WATER));
		return stack;
	}

	/**
	 * Позволяет использовать водяное зелье на блоках, конвертируемых в грязь.
	 * При успехе заменяет зелье стеклянной бутылкой, воспроизводит звук и
	 * спавнит брызги частиц на сервере.
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		PotionContentsComponent potionContents = stack.getOrDefault(
			DataComponentTypes.POTION_CONTENTS,
			PotionContentsComponent.DEFAULT
		);
		BlockState blockState = world.getBlockState(pos);

		if (context.getSide() == Direction.DOWN
			|| !blockState.isIn(BlockTags.CONVERTABLE_TO_MUD)
			|| !potionContents.matches(Potions.WATER)
		) {
			return ActionResult.PASS;
		}

		world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1.0F, 1.0F);
		player.setStackInHand(
			context.getHand(),
			ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE))
		);

		if (!world.isClient()) {
			ServerWorld serverWorld = (ServerWorld) world;

			for (int i = 0; i < MUD_SPLASH_PARTICLE_COUNT; i++) {
				serverWorld.spawnParticles(
					ParticleTypes.SPLASH,
					pos.getX() + world.random.nextDouble(),
					pos.getY() + 1,
					pos.getZ() + world.random.nextDouble(),
					1,
					0.0,
					0.0,
					0.0,
					1.0
				);
			}
		}

		world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
		world.setBlockState(pos, Blocks.MUD.getDefaultState());

		return ActionResult.SUCCESS;
	}

	@Override
	public Text getName(ItemStack stack) {
		PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);

		return potionContents != null
			? potionContents.getName(translationKey + ".effect.")
			: super.getName(stack);
	}
}
