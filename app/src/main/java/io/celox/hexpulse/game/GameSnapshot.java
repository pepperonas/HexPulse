package io.celox.hexpulse.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of the game state for multi-undo functionality
 */
public class GameSnapshot {
    private final Map<Hex, Player> board;
    private final Player currentPlayer;
    private final Map<Player, Integer> scores;
    private final int moveNumber;
    private final long timestamp;
    
    public GameSnapshot(Map<Hex, Player> board, Player currentPlayer, Map<Player, Integer> scores, int moveNumber) {
        this.board = new HashMap<>(board);
        this.currentPlayer = currentPlayer;
        this.scores = new HashMap<>(scores);
        this.moveNumber = moveNumber;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Map<Hex, Player> getBoard() {
        return new HashMap<>(board);
    }
    
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    
    public Map<Player, Integer> getScores() {
        return new HashMap<>(scores);
    }
    
    public int getMoveNumber() {
        return moveNumber;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}