package net.minecraft.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Arm;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Манекен — декоративная сущность, визуально повторяющая игрока.
 * Поддерживает скин через {@link ProfileComponent}, позы, скрытие частей модели,
 * режим неподвижности и отображение описания над головой.
 */
public class MannequinEntity extends PlayerLikeEntity {

	protected static final TrackedData<ProfileComponent> PROFILE =
			DataTracker.registerData(MannequinEntity.class, TrackedDataHandlerRegistry.PROFILE);
	private static final TrackedData<Boolean> IMMOVABLE =
			DataTracker.registerData(MannequinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Optional<Text>> DESCRIPTION = DataTracker.registerData(
			MannequinEntity.class, TrackedDataHandlerRegistry.OPTIONAL_TEXT_COMPONENT
	);

	/** Битовая маска всех частей модели игрока (все флаги включены). */
	private static final byte ALL_MODEL_PARTS = (byte) Arrays.stream(PlayerModelPart.values())
			.mapToInt(PlayerModelPart::getBitFlag)
			.reduce(0, (flagL, flagR) -> flagL | flagR);

	private static final Set<EntityPose> ALLOWED_POSES = Set.of(
			EntityPose.STANDING,
			EntityPose.CROUCHING,
			EntityPose.SWIMMING,
			EntityPose.GLIDING,
			EntityPose.SLEEPING
	);

	/**
	 * Кодек для поз манекена — принимает только позы из {@link #ALLOWED_POSES}.
	 */
	public static final Codec<EntityPose> POSE_CODEC = EntityPose.CODEC
			.validate(pose -> ALLOWED_POSES.contains(pose)
					? DataResult.success(pose)
					: DataResult.error(() -> "Invalid pose: " + pose.asString()));

	/**
	 * Кодек для скрытых частей модели: сериализует список скрытых частей,
	 * десериализует в битовую маску (инвертированную относительно ALL_MODEL_PARTS).
	 */
	private static final Codec<Byte> MODEL_PARTS_CODEC = PlayerModelPart.CODEC
			.listOf()
			.xmap(
					parts -> (byte) parts.stream()
							.mapToInt(PlayerModelPart::getBitFlag)
							.reduce(ALL_MODEL_PARTS, (flagL, flagR) -> flagL & ~flagR),
					bitFlag -> Arrays.stream(PlayerModelPart.values())
							.filter(part -> (bitFlag & part.getBitFlag()) == 0)
							.toList()
			);

	public static final ProfileComponent DEFAULT_INFO = ProfileComponent.Static.EMPTY;
	private static final Text DEFAULT_DESCRIPTION = Text.translatable("entity.minecraft.mannequin.label");

	protected static EntityType.EntityFactory<MannequinEntity> factory = MannequinEntity::new;

	private Text description = DEFAULT_DESCRIPTION;
	private boolean hideDescription = false;

	public MannequinEntity(EntityType<MannequinEntity> entityType, World world) {
		super(entityType, world);
		dataTracker.set(PLAYER_MODE_CUSTOMIZATION_ID, ALL_MODEL_PARTS);
	}

	protected MannequinEntity(World world) {
		this(EntityType.MANNEQUIN, world);
	}

	public static @Nullable MannequinEntity create(EntityType<MannequinEntity> type, World world) {
		return factory.create(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PROFILE, DEFAULT_INFO);
		builder.add(IMMOVABLE, false);
		builder.add(DESCRIPTION, Optional.of(DEFAULT_DESCRIPTION));
	}

	protected ProfileComponent getMannequinProfile() {
		return dataTracker.get(PROFILE);
	}

	private void setMannequinProfile(ProfileComponent profile) {
		dataTracker.set(PROFILE, profile);
	}

	private boolean isImmovable() {
		return dataTracker.get(IMMOVABLE);
	}

	private void setImmovable(boolean immovable) {
		dataTracker.set(IMMOVABLE, immovable);
	}

	protected @Nullable Text getDescription() {
		return dataTracker.get(DESCRIPTION).orElse(null);
	}

	private void setDescription(Text description) {
		this.description = description;
		updateTrackedDescription();
	}

	private void setHideDescription(boolean hideDescription) {
		this.hideDescription = hideDescription;
		updateTrackedDescription();
	}

	/** Синхронизирует описание с DataTracker: скрывает или показывает в зависимости от флага. */
	private void updateTrackedDescription() {
		dataTracker.set(DESCRIPTION, hideDescription ? Optional.empty() : Optional.of(description));
	}

	@Override
	protected boolean isImmobile() {
		return isImmovable() || super.isImmobile();
	}

	@Override
	public boolean canActVoluntarily() {
		return !isImmovable() && super.canActVoluntarily();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("profile", ProfileComponent.CODEC, getMannequinProfile());
		view.put("hidden_layers", MODEL_PARTS_CODEC, dataTracker.get(PLAYER_MODE_CUSTOMIZATION_ID));
		view.put("main_hand", Arm.CODEC, getMainArm());
		view.put("pose", POSE_CODEC, getPose());
		view.putBoolean("immovable", isImmovable());
		Text text = getDescription();
		if (text != null) {
			if (!text.equals(DEFAULT_DESCRIPTION)) {
				view.put("description", TextCodecs.CODEC, text);
			}
		} else {
			view.putBoolean("hide_description", true);
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		view.<ProfileComponent>read("profile", ProfileComponent.CODEC).ifPresent(this::setMannequinProfile);
		dataTracker.set(
				PLAYER_MODE_CUSTOMIZATION_ID,
				view.<Byte>read("hidden_layers", MODEL_PARTS_CODEC).orElse(ALL_MODEL_PARTS)
		);
		setMainArm(view.<Arm>read("main_hand", Arm.CODEC).orElse(MAIN_ARM));
		setPose(view.<EntityPose>read("pose", POSE_CODEC).orElse(EntityPose.STANDING));
		setImmovable(view.getBoolean("immovable", false));
		setHideDescription(view.getBoolean("hide_description", false));
		setDescription(view.<Text>read("description", TextCodecs.CODEC).orElse(DEFAULT_DESCRIPTION));
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.PROFILE
				? castComponentValue((ComponentType<T>) type, getMannequinProfile())
				: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.PROFILE);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.PROFILE) {
			setMannequinProfile(castComponentValue(DataComponentTypes.PROFILE, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}
}
