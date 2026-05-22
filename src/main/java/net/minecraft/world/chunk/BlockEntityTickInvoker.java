package net.minecraft.world.chunk;

import net.minecraft.util.math.BlockPos;

/**
 * Обёртка над тикающим блок-энтити, позволяющая безопасно вызывать тик
 * и проверять актуальность блок-энтити без прямой ссылки на него.
 */
public interface BlockEntityTickInvoker {

	void tick();

	boolean isRemoved();

	BlockPos getPos();

	String getName();
}
