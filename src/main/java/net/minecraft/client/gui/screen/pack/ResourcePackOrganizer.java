package net.minecraft.client.gui.screen.pack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Организатор пакетов ресурсов — управляет списками включённых и отключённых паков,
 * обеспечивает их перемещение, включение/отключение и применение изменений.
 */
@Environment(EnvType.CLIENT)
public class ResourcePackOrganizer {

	private final ResourcePackManager resourcePackManager;
	final List<ResourcePackProfile> enabledPacks;
	final List<ResourcePackProfile> disabledPacks;
	final Function<ResourcePackProfile, Identifier> iconIdSupplier;
	final Consumer<ResourcePackOrganizer.AbstractPack> updateCallback;
	private final Consumer<ResourcePackManager> applier;

	public ResourcePackOrganizer(
			Consumer<ResourcePackOrganizer.AbstractPack> updateCallback,
			Function<ResourcePackProfile, Identifier> iconIdSupplier,
			ResourcePackManager resourcePackManager,
			Consumer<ResourcePackManager> applier
	) {
		this.updateCallback = updateCallback;
		this.iconIdSupplier = iconIdSupplier;
		this.resourcePackManager = resourcePackManager;
		enabledPacks = Lists.newArrayList(resourcePackManager.getEnabledProfiles());
		Collections.reverse(enabledPacks);
		disabledPacks = Lists.newArrayList(resourcePackManager.getProfiles());
		disabledPacks.removeAll(enabledPacks);
		this.applier = applier;
	}

	public Stream<ResourcePackOrganizer.Pack> getDisabledPacks() {
		return disabledPacks.stream().map(pack -> new ResourcePackOrganizer.DisabledPack(pack));
	}

	public Stream<ResourcePackOrganizer.Pack> getEnabledPacks() {
		return enabledPacks.stream().map(pack -> new ResourcePackOrganizer.EnabledPack(pack));
	}

	void refreshEnabledProfiles() {
		resourcePackManager.setEnabledProfiles(
				Lists.reverse(enabledPacks)
						.stream()
						.map(ResourcePackProfile::getId)
						.collect(ImmutableList.toImmutableList())
		);
	}

	public void apply() {
		refreshEnabledProfiles();
		applier.accept(resourcePackManager);
	}

	public void refresh() {
		resourcePackManager.scanPacks();
		enabledPacks.retainAll(resourcePackManager.getProfiles());
		disabledPacks.clear();
		disabledPacks.addAll(resourcePackManager.getProfiles());
		disabledPacks.removeAll(enabledPacks);
	}

	/**
	 * Базовый класс пака — делегирует операции к профилю и спискам организатора.
	 */
	@Environment(EnvType.CLIENT)
	public abstract class AbstractPack implements ResourcePackOrganizer.Pack {

		private final ResourcePackProfile profile;

		public AbstractPack(final ResourcePackProfile profile) {
			this.profile = profile;
		}

		protected abstract List<ResourcePackProfile> getCurrentList();

		protected abstract List<ResourcePackProfile> getOppositeList();

		@Override
		public Identifier getIconId() {
			return ResourcePackOrganizer.this.iconIdSupplier.apply(profile);
		}

		@Override
		public ResourcePackCompatibility getCompatibility() {
			return profile.getCompatibility();
		}

		@Override
		public String getName() {
			return profile.getId();
		}

		@Override
		public Text getDisplayName() {
			return profile.getDisplayName();
		}

		@Override
		public Text getDescription() {
			return profile.getDescription();
		}

		@Override
		public ResourcePackSource getSource() {
			return profile.getSource();
		}

		@Override
		public boolean isPinned() {
			return profile.isPinned();
		}

		@Override
		public boolean isAlwaysEnabled() {
			return profile.isRequired();
		}

		protected void toggle() {
			getCurrentList().remove(profile);
			profile.getInitialPosition().insert(getOppositeList(), profile, ResourcePackProfile::getPosition, true);
			ResourcePackOrganizer.this.updateCallback.accept(this);
			ResourcePackOrganizer.this.refreshEnabledProfiles();
			toggleHighContrastOption();
		}

		private void toggleHighContrastOption() {
			if (!profile.getId().equals("high_contrast")) {
				return;
			}

			SimpleOption<Boolean> highContrast = MinecraftClient.getInstance().options.getHighContrast();
			highContrast.setValue(!highContrast.getValue());
		}

		protected void move(int offset) {
			List<ResourcePackProfile> list = getCurrentList();
			int currentIndex = list.indexOf(profile);
			list.remove(currentIndex);
			list.add(currentIndex + offset, profile);
			ResourcePackOrganizer.this.updateCallback.accept(this);
		}

		@Override
		public boolean canMoveTowardStart() {
			List<ResourcePackProfile> list = getCurrentList();
			int currentIndex = list.indexOf(profile);
			return currentIndex > 0 && !list.get(currentIndex - 1).isPinned();
		}

		@Override
		public void moveTowardStart() {
			move(-1);
		}

		@Override
		public boolean canMoveTowardEnd() {
			List<ResourcePackProfile> list = getCurrentList();
			int currentIndex = list.indexOf(profile);
			return currentIndex >= 0 && currentIndex < list.size() - 1 && !list.get(currentIndex + 1).isPinned();
		}

		@Override
		public void moveTowardEnd() {
			move(1);
		}
	}

	/**
	 * Отключённый пак — может быть включён.
	 */
	@Environment(EnvType.CLIENT)
	class DisabledPack extends ResourcePackOrganizer.AbstractPack {

		public DisabledPack(final ResourcePackProfile resourcePackProfile) {
			super(resourcePackProfile);
		}

		@Override
		protected List<ResourcePackProfile> getCurrentList() {
			return ResourcePackOrganizer.this.disabledPacks;
		}

		@Override
		protected List<ResourcePackProfile> getOppositeList() {
			return ResourcePackOrganizer.this.enabledPacks;
		}

		@Override
		public boolean isEnabled() {
			return false;
		}

		@Override
		public void enable() {
			toggle();
		}

		@Override
		public void disable() {
		}
	}

	/**
	 * Включённый пак — может быть отключён.
	 */
	@Environment(EnvType.CLIENT)
	class EnabledPack extends ResourcePackOrganizer.AbstractPack {

		public EnabledPack(final ResourcePackProfile resourcePackProfile) {
			super(resourcePackProfile);
		}

		@Override
		protected List<ResourcePackProfile> getCurrentList() {
			return ResourcePackOrganizer.this.enabledPacks;
		}

		@Override
		protected List<ResourcePackProfile> getOppositeList() {
			return ResourcePackOrganizer.this.disabledPacks;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public void enable() {
		}

		@Override
		public void disable() {
			toggle();
		}
	}

	/**
	 * Интерфейс пака ресурсов — описывает контракт для включения, отключения и перемещения.
	 */
	@Environment(EnvType.CLIENT)
	public interface Pack {

		Identifier getIconId();

		ResourcePackCompatibility getCompatibility();

		String getName();

		Text getDisplayName();

		Text getDescription();

		ResourcePackSource getSource();

		default Text getDecoratedDescription() {
			return getSource().decorate(getDescription());
		}

		boolean isPinned();

		boolean isAlwaysEnabled();

		void enable();

		void disable();

		void moveTowardStart();

		void moveTowardEnd();

		boolean isEnabled();

		default boolean canBeEnabled() {
			return !isEnabled();
		}

		default boolean canBeDisabled() {
			return isEnabled() && !isAlwaysEnabled();
		}

		boolean canMoveTowardStart();

		boolean canMoveTowardEnd();
	}
}
