package io.celox.hexpulse.game;

/**
 * Represents different AI difficulty levels
 */
public enum AIDifficulty {
    EASY(1, "Easy"),
    MEDIUM(2, "Medium"), 
    HARD(3, "Hard");
    
    private final int level;
    private final String displayName;
    
    AIDifficulty(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get search depth for minimax based on difficulty
     */
    public int getSearchDepth() {
        switch (this) {
            case EASY:
                return 1;
            case MEDIUM:
                return 2;
            case HARD:
                return 3;
            default:
                return 2;
        }
    }
    
    /**
     * Get maximum moves to evaluate for performance
     */
    public int getMaxMovesToEvaluate() {
        switch (this) {
            case EASY:
                return 5;
            case MEDIUM:
                return 10;
            case HARD:
                return 15;
            default:
                return 10;
        }
    }
}