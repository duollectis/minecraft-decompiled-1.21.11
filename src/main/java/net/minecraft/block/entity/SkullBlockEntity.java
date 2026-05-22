package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность черепа/головы. Отслеживает состояние питания от редстоуна
 * для анимации вращения черепа крипера, хранит профиль владельца (для голов игроков),
 * звук нотного блока и кастомное имя.
 */
public class SkullBlockEntity extends BlockEntity {

	private static final String PROFILE_NBT_KEY = "profile";
	private static final String NOTE_BLOCK_SOUND_NBT_KEY = "note_block_sound";
	private static final String CUSTOM_NAME_NBT_KEY = "custom_name";
	private @Nullable ProfileComponent owner;
	private @Nullable Identifier noteBlockSound;
	private int poweredTicks;
	private boolean powered;
	private @Nullable Text customName;

	public SkullBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.SKULL, pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putNullable(PROFILE_NBT_KEY, ProfileComponent.CODEC, owner);
		view.putNullable(NOTE_BLOCK_SOUND_NBT_KEY, Identifier.CODEC, noteBlockSound);
		view.putNullable(CUSTOM_NAME_NBT_KEY, TextCodecs.CODEC, customName);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		owner = view.<ProfileComponent>read(PROFILE_NBT_KEY, ProfileComponent.CODEC).orElse(null);
		noteBlockSound = view.<Identifier>read(NOTE_BLOCK_SOUND_NBT_KEY, Identifier.CODEC).orElse(null);
		customName = tryParseCustomName(view, CUSTOM_NAME_NBT_KEY);
	}

	public static void tick(World world, BlockPos pos, BlockState state, SkullBlockEntity blockEntity) {
		if (state.contains(SkullBlock.POWERED) && state.get(SkullBlock.POWERED)) {
			blockEntity.powered = true;
			blockEntity.poweredTicks++;
		} else {
			blockEntity.powered = false;
		}
	}

	public float getPoweredTicks(float tickProgress) {
		return powered ? poweredTicks + tickProgress : poweredTicks;
	}

	public @Nullable ProfileComponent getOwner() {
		return owner;
	}

	public @Nullable Identifier getNoteBlockSound() {
		return noteBlockSound;
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		owner = components.get(DataComponentTypes.PROFILE);
		noteBlockSound = components.get(DataComponentTypes.NOTE_BLOCK_SOUND);
		customName = components.get(DataComponentTypes.CUSTOM_NAME);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.PROFILE, owner);
		builder.add(DataComponentTypes.NOTE_BLOCK_SOUND, noteBlockSound);
		builder.add(DataComponentTypes.CUSTOM_NAME, customName);
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		super.removeFromCopiedStackData(view);
		view.remove(PROFILE_NBT_KEY);
		view.remove(NOTE_BLOCK_SOUND_NBT_KEY);
		view.remove(CUSTOM_NAME_NBT_KEY);
	}
}
