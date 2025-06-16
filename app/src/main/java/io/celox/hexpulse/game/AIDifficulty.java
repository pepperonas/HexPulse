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
                return 3;   // Strategic depth for competent play
            case MEDIUM:
                return 5;   // Good tactical awareness
            case HARD:
                return 7;   // Deep strategic planning
            default:
                return 4;
        }
    }
    
    /**
     * Get maximum moves to evaluate for performance
     */
    public int getMaxMovesToEvaluate() {
        switch (this) {
            case EASY:
                return 15;   // Solid move evaluation
            case MEDIUM:
                return 25;   // Comprehensive search
            case HARD:
                return 40;   // Exhaustive analysis
            default:
                return 20;
        }
    }
    
    /**
     * Get randomness factor for move selection
     */
    public double getRandomnessFactor() {
        switch (this) {
            case EASY:
                return 0.08;  // 8% random moves for some unpredictability
            case MEDIUM:
                return 0.02;  // 2% random moves to avoid perfect play  
            case HARD:
                return 0.0;   // No random moves - pure strategic play
            default:
                return 0.05;
        }
    }
    
    /**
     * Get evaluation complexity factor
     */
    public boolean useAdvancedEvaluation() {
        return this == MEDIUM || this == HARD;
    }
    
    /**
     * Get time limit for move calculation (milliseconds)
     */
    public long getTimeLimit() {
        switch (this) {
            case EASY:
                return 800;    // Fast response
            case MEDIUM:
                return 1500;   // Quick but thorough
            case HARD:
                return 2500;   // Deep analysis but responsive
            default:
                return 1200;
        }
    }
}