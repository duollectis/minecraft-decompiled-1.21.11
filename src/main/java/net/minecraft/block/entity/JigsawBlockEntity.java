package net.minecraft.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.JigsawBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

/**
 * Блок-сущность пазла (Jigsaw). Хранит конфигурацию соединения структурных пулов:
 * имя, цель, пул, тип соединения и приоритеты размещения/выбора.
 */
public class JigsawBlockEntity extends BlockEntity {

	public static final Codec<RegistryKey<StructurePool>> STRUCTURE_POOL_KEY_CODEC =
			RegistryKey.createCodec(RegistryKeys.TEMPLATE_POOL);
	public static final Identifier DEFAULT_NAME = Identifier.ofVanilla("empty");
	public static final String DEFAULT_FINAL_STATE = "minecraft:air";
	public static final String TARGET_KEY = "target";
	public static final String POOL_KEY = "pool";
	public static final String JOINT_KEY = "joint";
	public static final String PLACEMENT_PRIORITY_KEY = "placement_priority";
	public static final String SELECTION_PRIORITY_KEY = "selection_priority";
	public static final String NAME_KEY = "name";
	public static final String FINAL_STATE_KEY = "final_state";

	private Identifier name = DEFAULT_NAME;
	private Identifier target = DEFAULT_NAME;
	private RegistryKey<StructurePool> pool = StructurePools.EMPTY;
	private Joint joint = Joint.ROLLABLE;
	private String finalState = DEFAULT_FINAL_STATE;
	private int placementPriority;
	private int selectionPriority;

	public JigsawBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.JIGSAW, pos, state);
	}

	public Identifier getName() {
		return name;
	}

	public Identifier getTarget() {
		return target;
	}

	public RegistryKey<StructurePool> getPool() {
		return pool;
	}

	public String getFinalState() {
		return finalState;
	}

	public Joint getJoint() {
		return joint;
	}

	public int getPlacementPriority() {
		return placementPriority;
	}

	public int getSelectionPriority() {
		return selectionPriority;
	}

	public void setName(Identifier name) {
		this.name = name;
	}

	public void setTarget(Identifier target) {
		this.target = target;
	}

	public void setPool(RegistryKey<StructurePool> pool) {
		this.pool = pool;
	}

	public void setFinalState(String finalState) {
		this.finalState = finalState;
	}

	public void setJoint(Joint joint) {
		this.joint = joint;
	}

	public void setPlacementPriority(int placementPriority) {
		this.placementPriority = placementPriority;
	}

	public void setSelectionPriority(int selectionPriority) {
		this.selectionPriority = selectionPriority;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.put("name", Identifier.CODEC, name);
		view.put("target", Identifier.CODEC, target);
		view.put("pool", STRUCTURE_POOL_KEY_CODEC, pool);
		view.putString("final_state", finalState);
		view.put("joint", Joint.CODEC, joint);
		view.putInt("placement_priority", placementPriority);
		view.putInt("selection_priority", selectionPriority);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		name = view.<Identifier>read("name", Identifier.CODEC).orElse(DEFAULT_NAME);
		target = view.<Identifier>read("target", Identifier.CODEC).orElse(DEFAULT_NAME);
		pool = view.<RegistryKey<StructurePool>>read("pool", STRUCTURE_POOL_KEY_CODEC).orElse(StructurePools.EMPTY);
		finalState = view.getString("final_state", DEFAULT_FINAL_STATE);
		joint = view.<Joint>read("joint", Joint.CODEC)
				.orElseGet(() -> StructureTemplate.getJointFromFacing(getCachedState()));
		placementPriority = view.getInt("placement_priority", 0);
		selectionPriority = view.getInt("selection_priority", 0);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	/** Запускает генерацию структуры из пула, начиная с блока перед этим пазлом. */
	public void generate(ServerWorld world, int maxDepth, boolean keepJigsaws) {
		BlockPos startPos = getPos().offset(getCachedState().get(JigsawBlock.ORIENTATION).getFacing());
		Registry<StructurePool> registry = world.getRegistryManager().getOrThrow(RegistryKeys.TEMPLATE_POOL);
		RegistryEntry<StructurePool> poolEntry = registry.getOrThrow(pool);
		StructurePoolBasedGenerator.generate(world, poolEntry, target, maxDepth, startPos, keepJigsaws);
	}

	/** Тип соединения пазла: вращаемый (rollable) или выровненный (aligned). */
	public enum Joint implements StringIdentifiable {
		ROLLABLE("rollable"),
		ALIGNED("aligned");

		public static final StringIdentifiable.EnumCodec<JigsawBlockEntity.Joint>
				CODEC =
				StringIdentifiable.createCodec(JigsawBlockEntity.Joint::values);
		private final String name;

		private Joint(final String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}

		public Text asText() {
			return Text.translatable("jigsaw_block.joint." + name);
		}
	}
}
