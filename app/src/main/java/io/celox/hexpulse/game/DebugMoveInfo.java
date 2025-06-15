package io.celox.hexpulse.game;

import java.util.List;
import java.util.Map;

/**
 * Container for debug information about a move
 */
public class DebugMoveInfo {
    private final Map<Hex, Player> beforeState;
    private final Map<Hex, Player> afterState;
    private final List<Hex> selectedMarbles;
    private final Hex targetPosition;
    private final Player currentPlayer;
    
    public DebugMoveInfo(Map<Hex, Player> beforeState, Map<Hex, Player> afterState,
                        List<Hex> selectedMarbles, Hex targetPosition, Player currentPlayer) {
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.selectedMarbles = selectedMarbles;
        this.targetPosition = targetPosition;
        this.currentPlayer = currentPlayer;
    }
    
    public Map<Hex, Player> getBeforeState() {
        return beforeState;
    }
    
    public Map<Hex, Player> getAfterState() {
        return afterState;
    }
    
    public List<Hex> getSelectedMarbles() {
        return selectedMarbles;
    }
    
    public Hex getTargetPosition() {
        return targetPosition;
    }
    
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
}