package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * {@code OxidizableCopperGolemStatueBlock}.
 */
public class OxidizableCopperGolemStatueBlock extends CopperGolemStatueBlock implements Oxidizable {

	public static final MapCodec<OxidizableCopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							Oxidizable.OxidationLevel.CODEC
									.fieldOf("weathering_state")
									.forGetter(Degradable::getDegradationLevel), createSettingsCodec()
					)
					.apply(instance, OxidizableCopperGolemStatueBlock::new)
	);

	@Override
	public MapCodec<OxidizableCopperGolemStatueBlock> getCodec() {
		return CODEC;
	}

	public OxidizableCopperGolemStatueBlock(Oxidizable.OxidationLevel oxidationLevel, AbstractBlock.Settings settings) {
		super(oxidationLevel, settings);
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return Oxidizable.getIncreasedOxidationBlock(state.getBlock()).isPresent();
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		this.tickDegradation(state, world, pos, random);
	}

	public Oxidizable.OxidationLevel getDegradationLevel() {
		return this.getOxidationLevel();
	}

	@Override
	protected ActionResult onUseWithItem(
			ItemStack stack,
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		if (world.getBlockEntity(pos) instanceof CopperGolemStatueBlockEntity copperGolemStatueBlockEntity) {
			if (!stack.isIn(ItemTags.AXES)) {
				if (stack.isOf(Items.HONEYCOMB)) {
					return ActionResult.PASS;
				}

				this.changePose(world, state, pos, player);
				return ActionResult.SUCCESS;
			}

			if (this.getDegradationLevel().equals(Oxidizable.OxidationLevel.UNAFFECTED)) {
				CopperGolemEntity copperGolemEntity = copperGolemStatueBlockEntity.createCopperGolem(state);
				stack.damage(1, player, hand.getEquipmentSlot());
				if (copperGolemEntity != null) {
					world.spawnEntity(copperGolemEntity);
					world.removeBlock(pos, false);
					return ActionResult.SUCCESS;
				}
			}
		}

		return ActionResult.PASS;
	}
}
