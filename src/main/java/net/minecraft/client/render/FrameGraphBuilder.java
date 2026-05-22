package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ClosableFactory;
import net.minecraft.client.util.ObjectAllocator;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Строитель графа кадра (Frame Graph) — декларативной системы управления проходами рендеринга.
 * Позволяет описать зависимости между проходами и ресурсами, после чего автоматически
 * определяет топологический порядок выполнения, управляет временем жизни GPU-ресурсов
 * (acquire/release) и исключает неиспользуемые проходы из выполнения.
 */
@Environment(EnvType.CLIENT)
public class FrameGraphBuilder {

	private final List<FrameGraphBuilder.ResourceNode<?>> resourceNodes = new ArrayList<>();
	private final List<FrameGraphBuilder.ObjectNode<?>> objectNodes = new ArrayList<>();
	private final List<FrameGraphBuilder.FramePassImpl> passes = new ArrayList<>();

	public FramePass createPass(String name) {
		FrameGraphBuilder.FramePassImpl pass = new FrameGraphBuilder.FramePassImpl(passes.size(), name);
		passes.add(pass);
		return pass;
	}

	public <T> net.minecraft.client.util.Handle<T> createObjectNode(String name, T object) {
		FrameGraphBuilder.ObjectNode<T> objectNode = new FrameGraphBuilder.ObjectNode<>(name, null, object);
		objectNodes.add(objectNode);
		return objectNode.handle;
	}

	public <T> net.minecraft.client.util.Handle<T> createResourceHandle(String name, ClosableFactory<T> factory) {
		return createResourceNode(name, factory, null).handle;
	}

	<T> FrameGraphBuilder.ResourceNode<T> createResourceNode(
			String name,
			ClosableFactory<T> factory,
			FrameGraphBuilder.@Nullable FramePassImpl stageNode
	) {
		int nodeId = resourceNodes.size();
		FrameGraphBuilder.ResourceNode<T> resourceNode = new FrameGraphBuilder.ResourceNode<>(nodeId, name, stageNode, factory);
		resourceNodes.add(resourceNode);
		return resourceNode;
	}

	public void run(ObjectAllocator allocator) {
		run(allocator, FrameGraphBuilder.Profiler.NONE);
	}

	/**
	 * Выполняет граф кадра: определяет топологический порядок проходов, управляет
	 * временем жизни ресурсов и вызывает рендерер каждого прохода.
	 *
	 * @param allocator аллокатор GPU-ресурсов
	 * @param profiler  профилировщик для замера времени acquire/release/render
	 */
	public void run(ObjectAllocator allocator, FrameGraphBuilder.Profiler profiler) {
		BitSet passesToVisit = collectPassesToVisit();
		List<FrameGraphBuilder.FramePassImpl> orderedPasses = new ArrayList<>(passesToVisit.cardinality());
		BitSet visiting = new BitSet(passes.size());

		for (FrameGraphBuilder.FramePassImpl pass : passes) {
			visit(pass, passesToVisit, visiting, orderedPasses);
		}

		checkResources(orderedPasses);

		for (FrameGraphBuilder.FramePassImpl pass : orderedPasses) {
			for (FrameGraphBuilder.ResourceNode<?> resource : pass.resourcesToAcquire) {
				profiler.acquire(resource.name);
				resource.acquire(allocator);
			}

			profiler.push(pass.name);
			pass.renderer.run();
			profiler.pop(pass.name);

			for (int resourceId = pass.resourcesToRelease.nextSetBit(0);
					resourceId >= 0;
					resourceId = pass.resourcesToRelease.nextSetBit(resourceId + 1)
			) {
				FrameGraphBuilder.ResourceNode<?> resource = resourceNodes.get(resourceId);
				profiler.release(resource.name);
				resource.release(allocator);
			}
		}
	}

	private BitSet collectPassesToVisit() {
		Deque<FrameGraphBuilder.FramePassImpl> queue = new ArrayDeque<>(passes.size());
		BitSet result = new BitSet(passes.size());

		for (FrameGraphBuilder.Node<?> node : objectNodes) {
			FrameGraphBuilder.FramePassImpl producer = node.handle.from;
			if (producer != null) {
				markForVisit(producer, result, queue);
			}
		}

		for (FrameGraphBuilder.FramePassImpl pass : passes) {
			if (pass.toBeVisited) {
				markForVisit(pass, result, queue);
			}
		}

		return result;
	}

	private void markForVisit(
			FrameGraphBuilder.FramePassImpl pass,
			BitSet result,
			Deque<FrameGraphBuilder.FramePassImpl> queue
	) {
		queue.add(pass);

		while (!queue.isEmpty()) {
			FrameGraphBuilder.FramePassImpl current = queue.poll();
			if (result.get(current.id)) {
				continue;
			}

			result.set(current.id);

			for (int requiredId = current.requiredPassIds.nextSetBit(0);
					requiredId >= 0;
					requiredId = current.requiredPassIds.nextSetBit(requiredId + 1)
			) {
				queue.add(passes.get(requiredId));
			}
		}
	}

	/**
	 * Выполняет топологическую сортировку графа проходов с обнаружением циклов.
	 * Результат записывается в {@code topologicalOrderOut} в порядке выполнения.
	 */
	private void visit(
			FrameGraphBuilder.FramePassImpl node,
			BitSet unvisited,
			BitSet visiting,
			List<FrameGraphBuilder.FramePassImpl> topologicalOrderOut
	) {
		if (visiting.get(node.id)) {
			String cycle = visiting.stream()
					.mapToObj(id -> passes.get(id).name)
					.collect(Collectors.joining(", "));
			throw new IllegalStateException("Frame graph cycle detected between " + cycle);
		}

		if (!unvisited.get(node.id)) {
			return;
		}

		visiting.set(node.id);
		unvisited.clear(node.id);

		for (int requiredId = node.requiredPassIds.nextSetBit(0);
				requiredId >= 0;
				requiredId = node.requiredPassIds.nextSetBit(requiredId + 1)
		) {
			visit(passes.get(requiredId), unvisited, visiting, topologicalOrderOut);
		}

		for (FrameGraphBuilder.Handle<?> handle : node.transferredHandles) {
			for (int dependentId = handle.dependents.nextSetBit(0);
					dependentId >= 0;
					dependentId = handle.dependents.nextSetBit(dependentId + 1)
			) {
				if (dependentId != node.id) {
					visit(passes.get(dependentId), unvisited, visiting, topologicalOrderOut);
				}
			}
		}

		topologicalOrderOut.add(node);
		visiting.clear(node.id);
	}

	/**
	 * Определяет, в каком проходе каждый ресурс должен быть захвачен и освобождён,
	 * минимизируя время жизни ресурсов в памяти GPU.
	 */
	private void checkResources(Collection<FrameGraphBuilder.FramePassImpl> orderedPasses) {
		FrameGraphBuilder.FramePassImpl[] lastUsers = new FrameGraphBuilder.FramePassImpl[resourceNodes.size()];

		for (FrameGraphBuilder.FramePassImpl pass : orderedPasses) {
			for (int resourceId = pass.requiredResourceIds.nextSetBit(0);
					resourceId >= 0;
					resourceId = pass.requiredResourceIds.nextSetBit(resourceId + 1)
			) {
				FrameGraphBuilder.ResourceNode<?> resource = resourceNodes.get(resourceId);
				FrameGraphBuilder.FramePassImpl previousUser = lastUsers[resourceId];
				lastUsers[resourceId] = pass;

				if (previousUser == null) {
					pass.resourcesToAcquire.add(resource);
				}
				else {
					previousUser.resourcesToRelease.clear(resourceId);
				}

				pass.resourcesToRelease.set(resourceId);
			}
		}
	}

	/** Реализация прохода рендеринга, хранящая зависимости и список ресурсов. */
	@Environment(EnvType.CLIENT)
	class FramePassImpl implements FramePass {

		final int id;
		final String name;
		final List<FrameGraphBuilder.Handle<?>> transferredHandles = new ArrayList<>();
		final BitSet requiredResourceIds = new BitSet();
		final BitSet requiredPassIds = new BitSet();
		Runnable renderer = () -> {};
		final List<FrameGraphBuilder.ResourceNode<?>> resourcesToAcquire = new ArrayList<>();
		final BitSet resourcesToRelease = new BitSet();
		boolean toBeVisited;

		public FramePassImpl(final int id, final String name) {
			this.id = id;
			this.name = name;
		}

		private <T> void addRequired(FrameGraphBuilder.Handle<T> handle) {
			if (handle.parent instanceof FrameGraphBuilder.ResourceNode<?> resourceNode) {
				requiredResourceIds.set(resourceNode.id);
			}
		}

		private void addRequired(FrameGraphBuilder.FramePassImpl child) {
			requiredPassIds.set(child.id);
		}

		@Override
		public <T> net.minecraft.client.util.Handle<T> addRequiredResource(String name, ClosableFactory<T> factory) {
			FrameGraphBuilder.ResourceNode<T> resourceNode = FrameGraphBuilder.this.createResourceNode(name, factory, this);
			requiredResourceIds.set(resourceNode.id);
			return resourceNode.handle;
		}

		@Override
		public <T> void dependsOn(net.minecraft.client.util.Handle<T> handle) {
			dependsOn((FrameGraphBuilder.Handle<T>) handle);
		}

		private <T> void dependsOn(FrameGraphBuilder.Handle<T> handle) {
			addRequired(handle);
			if (handle.from != null) {
				addRequired(handle.from);
			}

			handle.dependents.set(id);
		}

		@Override
		public <T> net.minecraft.client.util.Handle<T> transfer(net.minecraft.client.util.Handle<T> handle) {
			return transfer((FrameGraphBuilder.Handle<T>) handle);
		}

		@Override
		public void addRequired(FramePass pass) {
			requiredPassIds.set(((FrameGraphBuilder.FramePassImpl) pass).id);
		}

		@Override
		public void markToBeVisited() {
			toBeVisited = true;
		}

		private <T> FrameGraphBuilder.Handle<T> transfer(FrameGraphBuilder.Handle<T> handle) {
			transferredHandles.add(handle);
			dependsOn(handle);
			return handle.moveTo(this);
		}

		@Override
		public void setRenderer(Runnable renderer) {
			this.renderer = renderer;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Дескриптор ресурса или объекта в графе кадра, отслеживающий владельца и зависимых. */
	@Environment(EnvType.CLIENT)
	static class Handle<T> implements net.minecraft.client.util.Handle<T> {

		final FrameGraphBuilder.Node<T> parent;
		private final int id;
		final FrameGraphBuilder.@Nullable FramePassImpl from;
		final BitSet dependents = new BitSet();
		private FrameGraphBuilder.@Nullable Handle<T> movedTo;

		Handle(FrameGraphBuilder.Node<T> parent, int id, FrameGraphBuilder.@Nullable FramePassImpl from) {
			this.parent = parent;
			this.id = id;
			this.from = from;
		}

		@Override
		public T get() {
			return parent.get();
		}

		FrameGraphBuilder.Handle<T> moveTo(FrameGraphBuilder.FramePassImpl pass) {
			if (parent.handle != this) {
				throw new IllegalStateException(
						"Handle " + this + " is no longer valid, as its contents were moved into " + movedTo);
			}

			FrameGraphBuilder.Handle<T> newHandle = new FrameGraphBuilder.Handle<>(parent, id + 1, pass);
			parent.handle = newHandle;
			movedTo = newHandle;
			return newHandle;
		}

		@Override
		public String toString() {
			return from != null
					? parent + "#" + id + " (from " + from + ")"
					: parent + "#" + id;
		}
	}

	/** Базовый узел графа, хранящий имя и текущий активный дескриптор. */
	@Environment(EnvType.CLIENT)
	abstract static class Node<T> {

		public final String name;
		public FrameGraphBuilder.Handle<T> handle;

		public Node(String name, FrameGraphBuilder.@Nullable FramePassImpl from) {
			this.name = name;
			handle = new FrameGraphBuilder.Handle<>(this, 0, from);
		}

		public abstract T get();

		@Override
		public String toString() {
			return name;
		}
	}

	/** Узел, хранящий готовый объект (не управляемый аллокатором). */
	@Environment(EnvType.CLIENT)
	static class ObjectNode<T> extends FrameGraphBuilder.Node<T> {

		private final T value;

		public ObjectNode(String name, FrameGraphBuilder.@Nullable FramePassImpl parent, T value) {
			super(name, parent);
			this.value = value;
		}

		@Override
		public T get() {
			return value;
		}
	}

	/**
	 * Профилировщик событий графа кадра: захват/освобождение ресурсов и выполнение проходов.
	 * Реализация по умолчанию {@link #NONE} — пустые методы без накладных расходов.
	 */
	@Environment(EnvType.CLIENT)
	public interface Profiler {

		Profiler NONE = new Profiler() {};

		default void acquire(String name) {
		}

		default void release(String name) {
		}

		default void push(String location) {
		}

		default void pop(String location) {
		}
	}

	/** Узел управляемого ресурса, захватываемого и освобождаемого через {@link ObjectAllocator}. */
	@Environment(EnvType.CLIENT)
	static class ResourceNode<T> extends FrameGraphBuilder.Node<T> {

		final int id;
		private final ClosableFactory<T> factory;
		private @Nullable T resource;

		public ResourceNode(
				int id,
				String name,
				FrameGraphBuilder.@Nullable FramePassImpl from,
				ClosableFactory<T> factory
		) {
			super(name, from);
			this.id = id;
			this.factory = factory;
		}

		@Override
		public T get() {
			return Objects.requireNonNull(resource, "Resource is not currently available");
		}

		public void acquire(ObjectAllocator allocator) {
			if (resource != null) {
				throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
			}

			resource = allocator.acquire(factory);
		}

		public void release(ObjectAllocator allocator) {
			if (resource == null) {
				throw new IllegalStateException("Tried to release physical resource that was not allocated");
			}

			allocator.release(factory, resource);
			resource = null;
		}
	}
}
