package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockStateManagers}.
 */
public class BlockStateManagers {

	private static final StateManager<Block, BlockState> ITEM_FRAME = createItemFrameStateManager();
	private static final StateManager<Block, BlockState> GLOW_ITEM_FRAME = createItemFrameStateManager();
	private static final Identifier GLOW_ITEM_FRAME_ID = Identifier.ofVanilla("glow_item_frame");
	private static final Identifier ITEM_FRAME_ID = Identifier.ofVanilla("item_frame");
	private static final Map<Identifier, StateManager<Block, BlockState>> STATIC_MANAGERS = Map.of(
			ITEM_FRAME_ID, ITEM_FRAME, GLOW_ITEM_FRAME_ID, GLOW_ITEM_FRAME
	);

	private static StateManager<Block, BlockState> createItemFrameStateManager() {
		return new StateManager.Builder<Block, BlockState>(Blocks.AIR)
				.add(Properties.MAP)
				.build(Block::getDefaultState, BlockState::new);
	}

	public static BlockState getStateForItemFrame(boolean hasGlow, boolean hasMap) {
		return (hasGlow ? GLOW_ITEM_FRAME : ITEM_FRAME).getDefaultState().with(Properties.MAP, hasMap);
	}

	static Function<Identifier, StateManager<Block, BlockState>> createIdToManagerMapper() {
		Map<Identifier, StateManager<Block, BlockState>> map = new HashMap<>(STATIC_MANAGERS);

		for (Block block : Registries.BLOCK) {
			map.put(block.getRegistryEntry().registryKey().getValue(), block.getStateManager());
		}

		return map::get;
	}
}
