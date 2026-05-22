package net.minecraft.item;

/**
 * Интерфейс для объектов, которые могут быть представлены как {@link Item}.
 * <p>Реализуется блоками ({@link net.minecraft.block.Block}), предметами ({@link Item})
 * и другими объектами, имеющими соответствующий предмет в инвентаре.</p>
 */
public interface ItemConvertible {

	Item asItem();
}
