package net.minecraft.sound;

import net.minecraft.registry.entry.RegistryEntry;

/**
 * Реестр предопределённых конфигураций фоновой музыки для различных игровых контекстов.
 *
 * <p>Каждая константа описывает музыку для конкретного экрана или измерения.
 * Внутриигровая музыка ({@link #GAME}, {@link #CREATIVE}, {@link #UNDERWATER})
 * использует стандартные задержки {@link #GAME_MIN_DELAY}–{@link #GAME_MAX_DELAY}.
 */
public class MusicType {

	private static final int MENU_MIN_DELAY = 20;
	private static final int MENU_MAX_DELAY = 600;
	private static final int GAME_MIN_DELAY = 12000;
	private static final int GAME_MAX_DELAY = 24000;
	private static final int END_MIN_DELAY = 6000;

	public static final MusicSound MENU = new MusicSound(SoundEvents.MUSIC_MENU, MENU_MIN_DELAY, MENU_MAX_DELAY, true);
	public static final MusicSound CREATIVE = new MusicSound(SoundEvents.MUSIC_CREATIVE, GAME_MIN_DELAY, GAME_MAX_DELAY, false);
	public static final MusicSound CREDITS = new MusicSound(SoundEvents.MUSIC_CREDITS, 0, 0, true);
	public static final MusicSound DRAGON = new MusicSound(SoundEvents.MUSIC_DRAGON, 0, 0, true);
	public static final MusicSound END = new MusicSound(SoundEvents.MUSIC_END, END_MIN_DELAY, GAME_MAX_DELAY, true);
	public static final MusicSound UNDERWATER = createIngameMusic(SoundEvents.MUSIC_UNDER_WATER);
	public static final MusicSound GAME = createIngameMusic(SoundEvents.MUSIC_GAME);

	/**
	 * Создаёт конфигурацию внутриигровой музыки со стандартными задержками
	 * и без принудительной замены текущего трека.
	 *
	 * @param sound ссылка на звуковое событие трека
	 * @return новый экземпляр {@link MusicSound} с игровыми задержками
	 */
	public static MusicSound createIngameMusic(RegistryEntry<SoundEvent> sound) {
		return new MusicSound(sound, GAME_MIN_DELAY, GAME_MAX_DELAY, false);
	}
}
