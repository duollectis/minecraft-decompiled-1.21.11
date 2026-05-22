package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public abstract class AbstractBannerBlock extends BlockWithEntity {

	private final DyeColor color;

	public DyeColor getColor() {
		return color;
	}

	protected AbstractBannerBlock(DyeColor color, AbstractBlock.Settings settings) {
		super(settings);
		this.color = color;
	}

	@Override
	protected abstract MapCodec<? extends AbstractBannerBlock> getCodec();

	@Override
	public boolean canMobSpawnInside(BlockState state) {
		return true;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BannerBlockEntity(pos, state, color);
	}

	/**
	 * Возвращает предмет для режима выбора блока (средняя кнопка мыши),
	 * сохраняя паттерны баннера из {@link BannerBlockEntity} в NBT стека.
	 * Без делегирования к сущности блока паттерны были бы утеряны.
	 */
	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return world.getBlockEntity(pos) instanceof BannerBlockEntity bannerEntity
			? bannerEntity.getPickStack()
			: super.getPickStack(world, pos, state, includeData);
	}
}
