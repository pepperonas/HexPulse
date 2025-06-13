package io.celox.hexpulse.game;

/**
 * Represents the different players and empty spaces on the board
 */
public enum Player {
    BLACK('B'),
    WHITE('W'),
    EMPTY(' ');
    
    private final char symbol;
    
    Player(char symbol) {
        this.symbol = symbol;
    }
    
    public char getSymbol() {
        return symbol;
    }
    
    /**
     * Get the opponent player
     */
    public Player getOpponent() {
        switch (this) {
            case BLACK:
                return WHITE;
            case WHITE:
                return BLACK;
            default:
                return EMPTY;
        }
    }
    
    /**
     * Check if this is a valid player (not empty)
     */
    public boolean isPlayer() {
        return this != EMPTY;
    }
}