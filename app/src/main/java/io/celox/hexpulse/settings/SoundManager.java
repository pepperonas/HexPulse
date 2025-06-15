package io.celox.hexpulse.settings;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import io.celox.hexpulse.R;

/**
 * Manages game sound effects
 */
public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;
    private GameSettings settings;
    
    // Sound IDs (to be implemented with actual sound files)
    private int marbleSelectSound = -1;
    private int marbleMoveSound = -1;
    private int gameWinSound = -1;
    
    private SoundManager(Context context) {
        settings = GameSettings.getInstance(context);
        initializeSoundPool(context);
    }
    
    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }
    
    private void initializeSoundPool(Context context) {
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .build();
        
        // TODO: Load actual sound files when they are added to res/raw/
        // marbleSelectSound = soundPool.load(context, R.raw.marble_select, 1);
        // marbleMoveSound = soundPool.load(context, R.raw.marble_move, 1);
        // gameWinSound = soundPool.load(context, R.raw.game_win, 1);
    }
    
    public void playMarbleSelect() {
        if (settings.isSoundEnabled() && marbleSelectSound != -1) {
            soundPool.play(marbleSelectSound, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }
    
    public void playMarbleMove() {
        if (settings.isSoundEnabled() && marbleMoveSound != -1) {
            soundPool.play(marbleMoveSound, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }
    
    public void playGameWin() {
        if (settings.isSoundEnabled() && gameWinSound != -1) {
            soundPool.play(gameWinSound, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }
    
    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}