package net.minecraft.util.packrat;

/**
 * Маркер «отсечения» (cut) в PEG-парсере. После вызова {@link #cut()} альтернативные
 * ветви в {@code anyOf} перестают проверяться, что предотвращает бесконечный откат
 * и делает грамматику детерминированной в точке отсечения.
 */
public interface Cut {

	Cut NOOP = new Cut() {
		@Override
		public void cut() {
		}

		@Override
		public boolean isCut() {
			return false;
		}
	};

	void cut();

	boolean isCut();
}
