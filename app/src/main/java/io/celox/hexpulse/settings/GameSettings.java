package io.celox.hexpulse.settings;

import android.content.Context;
import android.content.SharedPreferences;
import io.celox.hexpulse.game.AIDifficulty;
import io.celox.hexpulse.game.Theme;

/**
 * Manages game settings using SharedPreferences
 */
public class GameSettings {
    private static final String PREF_NAME = "hexpulse_settings";
    private static final String KEY_AI_DIFFICULTY = "ai_difficulty";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    
    private static GameSettings instance;
    private SharedPreferences preferences;
    
    private GameSettings(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized GameSettings getInstance(Context context) {
        if (instance == null) {
            instance = new GameSettings(context);
        }
        return instance;
    }
    
    // AI Difficulty Settings
    public AIDifficulty getAIDifficulty() {
        String difficultyName = preferences.getString(KEY_AI_DIFFICULTY, AIDifficulty.MEDIUM.name());
        try {
            return AIDifficulty.valueOf(difficultyName);
        } catch (IllegalArgumentException e) {
            return AIDifficulty.MEDIUM; // Default fallback
        }
    }
    
    public void setAIDifficulty(AIDifficulty difficulty) {
        preferences.edit().putString(KEY_AI_DIFFICULTY, difficulty.name()).apply();
    }
    
    // Theme Settings
    public Theme getTheme() {
        String themeName = preferences.getString(KEY_THEME, Theme.CLASSIC.name());
        try {
            return Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return Theme.CLASSIC; // Default fallback
        }
    }
    
    public void setTheme(Theme theme) {
        preferences.edit().putString(KEY_THEME, theme.name()).apply();
    }
    
    // Sound Settings
    public boolean isSoundEnabled() {
        return preferences.getBoolean(KEY_SOUND_ENABLED, true); // Default enabled
    }
    
    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
    
    // Utility method to get display name for difficulty
    public static String getDifficultyDisplayName(AIDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return "Easy";
            case MEDIUM:
                return "Medium";
            case HARD:
                return "Hard";
            default:
                return "Medium";
        }
    }
    
    // Utility method to get display name for theme
    public static String getThemeDisplayName(Theme theme) {
        switch (theme) {
            case CLASSIC:
                return "Classic";
            case DARK:
                return "Dark";
            case OCEAN:
                return "Ocean";
            case FOREST:
                return "Forest";
            default:
                return "Classic";
        }
    }
}