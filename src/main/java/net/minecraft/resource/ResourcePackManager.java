package net.minecraft.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Менеджер ресурс-паков: управляет списком доступных и активных паков.
 * Сканирует паки через зарегистрированные {@link ResourcePackProvider} и
 * поддерживает включение/отключение паков.
 */
public class ResourcePackManager {

	private final Set<ResourcePackProvider> providers;
	private Map<String, ResourcePackProfile> profiles = ImmutableMap.of();
	private List<ResourcePackProfile> enabled = ImmutableList.of();

	public ResourcePackManager(ResourcePackProvider... providers) {
		this.providers = ImmutableSet.copyOf(providers);
	}

	/**
	 * Формирует строку со списком паков и пометкой о несовместимых.
	 *
	 * @param profiles коллекция профилей паков
	 * @return строка с перечислением паков
	 */
	public static String listPacks(Collection<ResourcePackProfile> profiles) {
		return profiles.stream()
			.map(profile -> profile.getId() + (profile.getCompatibility().isCompatible() ? "" : " (incompatible)"))
			.collect(Collectors.joining(", "));
	}

	/**
	 * Сканирует все провайдеры и обновляет список доступных паков.
	 * Сохраняет текущий список включённых паков, если они ещё доступны.
	 */
	public void scanPacks() {
		List<String> enabledIds = enabled.stream()
			.map(ResourcePackProfile::getId)
			.collect(ImmutableList.toImmutableList());
		profiles = providePackProfiles();
		enabled = buildEnabledProfiles(enabledIds);
	}

	private Map<String, ResourcePackProfile> providePackProfiles() {
		Map<String, ResourcePackProfile> map = Maps.newTreeMap();

		for (ResourcePackProvider provider : providers) {
			provider.register(profile -> map.put(profile.getId(), profile));
		}

		return ImmutableMap.copyOf(map);
	}

	/**
	 * Проверяет, включены ли какие-либо опциональные паки сверх обязательных.
	 *
	 * @return {@code true}, если включены опциональные паки
	 */
	public boolean hasOptionalProfilesEnabled() {
		List<ResourcePackProfile> defaultEnabled = buildEnabledProfiles(List.of());
		return !enabled.equals(defaultEnabled);
	}

	/**
	 * Устанавливает список включённых паков по их идентификаторам.
	 *
	 * @param enabled коллекция идентификаторов включаемых паков
	 */
	public void setEnabledProfiles(Collection<String> enabled) {
		this.enabled = buildEnabledProfiles(enabled);
	}

	/**
	 * Включает пак по идентификатору, если он существует и ещё не включён.
	 *
	 * @param profile идентификатор пака
	 * @return {@code true}, если пак был успешно включён
	 */
	public boolean enable(String profile) {
		ResourcePackProfile packProfile = profiles.get(profile);
		if (packProfile == null || enabled.contains(packProfile)) {
			return false;
		}

		List<ResourcePackProfile> list = Lists.newArrayList(enabled);
		list.add(packProfile);
		enabled = list;
		return true;
	}

	/**
	 * Отключает пак по идентификатору, если он существует и включён.
	 *
	 * @param profile идентификатор пака
	 * @return {@code true}, если пак был успешно отключён
	 */
	public boolean disable(String profile) {
		ResourcePackProfile packProfile = profiles.get(profile);
		if (packProfile == null || !enabled.contains(packProfile)) {
			return false;
		}

		List<ResourcePackProfile> list = Lists.newArrayList(enabled);
		list.remove(packProfile);
		enabled = list;
		return true;
	}

	private List<ResourcePackProfile> buildEnabledProfiles(Collection<String> enabledNames) {
		List<ResourcePackProfile> list = streamProfilesById(enabledNames).collect(Util.toArrayList());

		for (ResourcePackProfile profile : profiles.values()) {
			if (profile.isRequired() && !list.contains(profile)) {
				profile.getInitialPosition().insert(list, profile, ResourcePackProfile::getPosition, false);
			}
		}

		return ImmutableList.copyOf(list);
	}

	private Stream<ResourcePackProfile> streamProfilesById(Collection<String> ids) {
		return ids.stream().map(profiles::get).filter(Objects::nonNull);
	}

	public Collection<String> getIds() {
		return profiles.keySet();
	}

	public Collection<ResourcePackProfile> getProfiles() {
		return profiles.values();
	}

	public Collection<String> getEnabledIds() {
		return enabled.stream()
			.map(ResourcePackProfile::getId)
			.collect(ImmutableSet.toImmutableSet());
	}

	/**
	 * Возвращает объединённый набор фич всех включённых паков.
	 *
	 * @return набор запрошенных фич
	 */
	public FeatureSet getRequestedFeatures() {
		return getEnabledProfiles().stream()
			.map(ResourcePackProfile::getRequestedFeatures)
			.reduce(FeatureSet::combine)
			.orElse(FeatureSet.empty());
	}

	public Collection<ResourcePackProfile> getEnabledProfiles() {
		return enabled;
	}

	public @Nullable ResourcePackProfile getProfile(String id) {
		return profiles.get(id);
	}

	public boolean hasProfile(String id) {
		return profiles.containsKey(id);
	}

	/**
	 * Создаёт список открытых ресурс-паков из всех включённых профилей.
	 *
	 * @return список открытых паков
	 */
	public List<ResourcePack> createResourcePacks() {
		return enabled.stream()
			.map(ResourcePackProfile::createResourcePack)
			.collect(ImmutableList.toImmutableList());
	}
}
