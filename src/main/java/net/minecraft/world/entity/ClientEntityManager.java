package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.Entity;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.slf4j.Logger;

/**
 * Клиентский менеджер сущностей, управляющий жизненным циклом сущностей
 * в клиентском мире. Отслеживает, какие чанк-секции активно тикаются,
 * и уведомляет {@link EntityHandler} при изменении статуса сущностей.
 *
 * <p>Каждая добавленная сущность получает {@link Listener}, который автоматически
 * обновляет её позицию в {@link SectionedEntityCache} при перемещении или удалении.
 *
 * @param <T> тип сущностей (должен реализовывать {@link EntityLike})
 */
public class ClientEntityManager<T extends EntityLike> {

	static final Logger LOGGER = LogUtils.getLogger();

	final EntityHandler<T> handler;
	final EntityIndex<T> index;
	final SectionedEntityCache<T> cache;

	private final LongSet tickingChunkSections = new LongOpenHashSet();
	private final EntityLookup<T> lookup;

	public ClientEntityManager(Class<T> entityClass, EntityHandler<T> handler) {
		this.index = new EntityIndex<>();
		this.cache = new SectionedEntityCache<>(
			entityClass,
			pos -> tickingChunkSections.contains(pos)
				? EntityTrackingStatus.TICKING
				: EntityTrackingStatus.TRACKED
		);
		this.handler = handler;
		this.lookup = new SimpleEntityLookup<>(index, cache);
	}

	/**
	 * Переводит все секции указанного чанка в статус TICKING.
	 * Для каждой секции, которая ранее не тикалась, вызывает {@code startTicking}
	 * для всех не-игровых сущностей в ней.
	 *
	 * @param pos позиция чанка
	 */
	public void startTicking(ChunkPos pos) {
		long packedPos = pos.toLong();
		tickingChunkSections.add(packedPos);
		cache.getTrackingSections(packedPos).forEach(section -> {
			EntityTrackingStatus previous = section.swapStatus(EntityTrackingStatus.TICKING);
			if (!previous.shouldTick()) {
				section.stream().filter(entity -> !entity.isPlayer()).forEach(handler::startTicking);
			}
		});
	}

	/**
	 * Переводит все секции указанного чанка в статус TRACKED (без тикинга).
	 * Для каждой секции, которая ранее тикалась, вызывает {@code stopTicking}
	 * для всех не-игровых сущностей в ней.
	 *
	 * @param pos позиция чанка
	 */
	public void stopTicking(ChunkPos pos) {
		long packedPos = pos.toLong();
		tickingChunkSections.remove(packedPos);
		cache.getTrackingSections(packedPos).forEach(section -> {
			EntityTrackingStatus previous = section.swapStatus(EntityTrackingStatus.TRACKED);
			if (previous.shouldTick()) {
				section.stream().filter(entity -> !entity.isPlayer()).forEach(handler::stopTicking);
			}
		});
	}

	public EntityLookup<T> getLookup() {
		return lookup;
	}

	/**
	 * Добавляет сущность в менеджер: регистрирует в индексе, помещает в секцию кэша,
	 * назначает слушатель изменений и уведомляет handler о создании и начале отслеживания.
	 * Если секция тикается или сущность является игроком — также запускает тикинг.
	 *
	 * @param entity добавляемая сущность
	 */
	public void addEntity(T entity) {
		index.add(entity);
		long sectionPos = ChunkSectionPos.toLong(entity.getBlockPos());
		EntityTrackingSection<T> section = cache.getTrackingSection(sectionPos);
		section.add(entity);
		entity.setChangeListener(new Listener(entity, sectionPos, section));
		handler.create(entity);
		handler.startTracking(entity);

		if (entity.isPlayer() || section.getStatus().shouldTick()) {
			handler.startTicking(entity);
		}
	}

	@Debug
	public int getEntityCount() {
		return index.size();
	}

	@Debug
	public String getDebugString() {
		return index.size() + "," + cache.sectionCount() + "," + tickingChunkSections.size();
	}

	void removeIfEmpty(long packedChunkSection, EntityTrackingSection<T> section) {
		if (section.isEmpty()) {
			cache.removeSection(packedChunkSection);
		}
	}

	/**
	 * Внутренний слушатель, привязанный к конкретной сущности.
	 * Обновляет позицию сущности в {@link SectionedEntityCache} при её перемещении
	 * и корректно очищает все ссылки при удалении.
	 */
	class Listener implements EntityChangeListener {

		private final T entity;
		private long lastSectionPos;
		private EntityTrackingSection<T> section;

		Listener(T entity, long sectionPos, EntityTrackingSection<T> section) {
			this.entity = entity;
			this.lastSectionPos = sectionPos;
			this.section = section;
		}

		@Override
		public void updateEntityPosition() {
			BlockPos blockPos = entity.getBlockPos();
			long newSectionPos = ChunkSectionPos.toLong(blockPos);
			if (newSectionPos == lastSectionPos) {
				return;
			}

			EntityTrackingStatus previousStatus = section.getStatus();
			if (!section.remove(entity)) {
				LOGGER.warn(
					"Entity {} wasn't found in section {} (moving to {})",
					new Object[]{ entity, ChunkSectionPos.from(lastSectionPos), newSectionPos }
				);
			}

			ClientEntityManager.this.removeIfEmpty(lastSectionPos, section);

			EntityTrackingSection<T> newSection = ClientEntityManager.this.cache.getTrackingSection(newSectionPos);
			newSection.add(entity);
			section = newSection;
			lastSectionPos = newSectionPos;
			ClientEntityManager.this.handler.updateLoadStatus(entity);

			if (entity.isPlayer()) {
				return;
			}

			boolean wasTicking = previousStatus.shouldTick();
			boolean nowTicking = newSection.getStatus().shouldTick();
			if (wasTicking && !nowTicking) {
				ClientEntityManager.this.handler.stopTicking(entity);
			} else if (!wasTicking && nowTicking) {
				ClientEntityManager.this.handler.startTicking(entity);
			}
		}

		@Override
		public void remove(Entity.RemovalReason reason) {
			if (!section.remove(entity)) {
				LOGGER.warn(
					"Entity {} wasn't found in section {} (destroying due to {})",
					new Object[]{ entity, ChunkSectionPos.from(lastSectionPos), reason }
				);
			}

			if (section.getStatus().shouldTick() || entity.isPlayer()) {
				ClientEntityManager.this.handler.stopTicking(entity);
			}

			ClientEntityManager.this.handler.stopTracking(entity);
			ClientEntityManager.this.handler.destroy(entity);
			ClientEntityManager.this.index.remove(entity);
			entity.setChangeListener(NONE);
			ClientEntityManager.this.removeIfEmpty(lastSectionPos, section);
		}
	}
}
