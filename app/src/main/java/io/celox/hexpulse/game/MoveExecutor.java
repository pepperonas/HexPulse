package io.celox.hexpulse.game;

import java.util.*;

/**
 * Executes validated moves according to Abalone rules
 */
public class MoveExecutor {
    
    private final Map<Hex, Player> board;
    private final Map<Player, Integer> scores;
    
    public MoveExecutor(Map<Hex, Player> board, Map<Player, Integer> scores) {
        this.board = board;
        this.scores = scores;
    }
    
    /**
     * Execute a validated move
     */
    public void executeMove(MoveValidator.ValidatedMove move, Player currentPlayer) {
        switch (move.type) {
            case SINGLE_MARBLE:
                executeSingleMarbleMove(move, currentPlayer);
                break;
            case INLINE_MOVE:
                executeInlineMove(move, currentPlayer);
                break;
            case SIDESTEP_MOVE:
                executeSidestepMove(move, currentPlayer);
                break;
        }
    }
    
    /**
     * Execute a single marble move
     */
    private void executeSingleMarbleMove(MoveValidator.ValidatedMove move, Player currentPlayer) {
        Hex marble = move.marbles.get(0);
        Hex target = move.targetPosition;
        
        // Clear old position and set new position
        board.put(marble, Player.EMPTY);
        board.put(target, currentPlayer);
    }
    
    /**
     * Execute an inline move (with or without pushing)
     */
    private void executeInlineMove(MoveValidator.ValidatedMove move, Player currentPlayer) {
        if (move.isPush) {
            executeInlineMoveWithPush(move, currentPlayer);
        } else {
            executeSimpleInlineMove(move, currentPlayer);
        }
    }
    
    /**
     * Execute a simple inline move without pushing
     */
    private void executeSimpleInlineMove(MoveValidator.ValidatedMove move, Player currentPlayer) {
        // Sort marbles by movement direction (back to front)
        List<Hex> sortedMarbles = getSortedMarblesForMovement(move.marbles, move.direction);
        
        // Move marbles from back to front to avoid overwriting
        for (Hex marble : sortedMarbles) {
            Hex newPosition = marble.neighbor(move.direction);
            board.put(marble, Player.EMPTY);
            board.put(newPosition, currentPlayer);
        }
    }
    
    /**
     * Execute an inline move with pushing
     */
    private void executeInlineMoveWithPush(MoveValidator.ValidatedMove move, Player currentPlayer) {
        Player opponent = currentPlayer.getOpponent();
        
        // First, handle pushed marbles (from farthest to nearest)
        List<Hex> reversedPushed = new ArrayList<>(move.pushedMarbles);
        Collections.reverse(reversedPushed);
        
        for (Hex pushedMarble : reversedPushed) {
            Hex newPosition = pushedMarble.neighbor(move.direction);
            
            if (!isValidBoardPosition(newPosition)) {
                // Marble pushed off board - increment score
                board.put(pushedMarble, Player.EMPTY);
                scores.put(currentPlayer, scores.getOrDefault(currentPlayer, 0) + 1);
            } else {
                // Move marble to new position
                board.put(pushedMarble, Player.EMPTY);
                board.put(newPosition, opponent);
            }
        }
        
        // Then move our marbles
        executeSimpleInlineMove(move, currentPlayer);
    }
    
    /**
     * Execute a sidestep (broadside) move
     */
    private void executeSidestepMove(MoveValidator.ValidatedMove move, Player currentPlayer) {
        // Clear all old positions first
        for (Hex marble : move.marbles) {
            board.put(marble, Player.EMPTY);
        }
        
        // Set all new positions
        for (Hex marble : move.marbles) {
            Hex newPosition = marble.neighbor(move.direction);
            board.put(newPosition, currentPlayer);
        }
    }
    
    /**
     * Sort marbles for movement execution (back to front)
     */
    private List<Hex> getSortedMarblesForMovement(List<Hex> marbles, int direction) {
        List<Hex> sorted = new ArrayList<>(marbles);
        int[] dirVector = Hex.DIRECTIONS[direction];
        
        // Sort from back to front (reverse of movement direction)
        sorted.sort((a, b) -> Integer.compare(
            b.q * dirVector[0] + b.r * dirVector[1],
            a.q * dirVector[0] + a.r * dirVector[1]
        ));
        
        return sorted;
    }
    
    /**
     * Check if position is valid on the board
     */
    private boolean isValidBoardPosition(Hex position) {
        return Math.abs(position.q) <= 4 && 
               Math.abs(position.r) <= 4 && 
               Math.abs(-position.q - position.r) <= 4;
    }
}