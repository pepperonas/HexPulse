package io.celox.hexpulse.game;

import java.util.*;

/**
 * Validates moves according to official Abalone rules
 */
public class MoveValidator {
    
    /**
     * Represents a validated move with all necessary information
     */
    public static class ValidatedMove {
        public final List<Hex> marbles;
        public final Hex targetPosition;
        public final int direction;
        public final MoveType type;
        public final boolean isPush;
        public final List<Hex> pushedMarbles;
        
        public ValidatedMove(List<Hex> marbles, Hex targetPosition, int direction, 
                           MoveType type, boolean isPush, List<Hex> pushedMarbles) {
            this.marbles = new ArrayList<>(marbles);
            this.targetPosition = targetPosition;
            this.direction = direction;
            this.type = type;
            this.isPush = isPush;
            this.pushedMarbles = pushedMarbles != null ? new ArrayList<>(pushedMarbles) : new ArrayList<>();
        }
    }
    
    public enum MoveType {
        SINGLE_MARBLE,
        INLINE_MOVE,
        SIDESTEP_MOVE
    }
    
    private final Map<Hex, Player> board;
    private final Player currentPlayer;
    
    public MoveValidator(Map<Hex, Player> board, Player currentPlayer) {
        this.board = board;
        this.currentPlayer = currentPlayer;
    }
    
    /**
     * Get all valid moves for selected marbles according to Abalone rules
     */
    public List<ValidatedMove> getValidMoves(List<Hex> selectedMarbles) {
        List<ValidatedMove> validMoves = new ArrayList<>();
        
        if (selectedMarbles.isEmpty() || selectedMarbles.size() > 3) {
            return validMoves;
        }
        
        // Verify all marbles belong to current player
        for (Hex marble : selectedMarbles) {
            if (board.getOrDefault(marble, Player.EMPTY) != currentPlayer) {
                return validMoves;
            }
        }
        
        if (selectedMarbles.size() == 1) {
            validMoves.addAll(getSingleMarbleMoves(selectedMarbles.get(0)));
        } else {
            // Multiple marbles - must form a column (straight line)
            Column column = identifyColumn(selectedMarbles);
            if (column != null) {
                validMoves.addAll(getColumnMoves(column));
            }
        }
        
        return validMoves;
    }
    
    /**
     * Get all valid moves for a single marble
     */
    private List<ValidatedMove> getSingleMarbleMoves(Hex marble) {
        List<ValidatedMove> moves = new ArrayList<>();
        
        for (int dir = 0; dir < 6; dir++) {
            Hex target = marble.neighbor(dir);
            
            if (!isValidBoardPosition(target)) {
                continue;
            }
            
            Player targetPlayer = board.getOrDefault(target, Player.EMPTY);
            
            if (targetPlayer == Player.EMPTY) {
                // Simple move to empty space
                moves.add(new ValidatedMove(
                    Arrays.asList(marble), target, dir, 
                    MoveType.SINGLE_MARBLE, false, null
                ));
            }
            // Note: Single marble cannot push according to rules
        }
        
        return moves;
    }
    
    /**
     * Get all valid moves for a column of marbles
     */
    private List<ValidatedMove> getColumnMoves(Column column) {
        List<ValidatedMove> moves = new ArrayList<>();
        
        for (int dir = 0; dir < 6; dir++) {
            // Check inline moves (along column axis)
            if (dir == column.direction || dir == (column.direction + 3) % 6) {
                ValidatedMove inlineMove = validateInlineMove(column, dir);
                if (inlineMove != null) {
                    moves.add(inlineMove);
                }
            } else {
                // Check sidestep moves (perpendicular to column axis)
                ValidatedMove sidestepMove = validateSidestepMove(column, dir);
                if (sidestepMove != null) {
                    moves.add(sidestepMove);
                }
            }
        }
        return moves;
    }
    
    /**
     * Validate an inline move (potentially with pushing)
     */
    private ValidatedMove validateInlineMove(Column column, int direction) {
        // Determine lead marble (front of the column in movement direction)
        Hex leadMarble = getLeadMarble(column, direction);
        Hex target = leadMarble.neighbor(direction);
        
        if (!isValidBoardPosition(target)) {
            return null;
        }
        
        Player targetPlayer = board.getOrDefault(target, Player.EMPTY);
        
        if (targetPlayer == Player.EMPTY) {
            // Simple inline move to empty space
            return new ValidatedMove(
                column.marbles, target, direction,
                MoveType.INLINE_MOVE, false, null
            );
        } else if (targetPlayer == currentPlayer.getOpponent()) {
            // Potential push - validate Sumito rules
            return validateSumito(column, direction, leadMarble);
        }
        
        // Cannot move into own marble
        return null;
    }
    
    /**
     * Validate a sidestep (broadside) move
     */
    private ValidatedMove validateSidestepMove(Column column, int direction) {
        // According to rules: "A side-step move CANNOT be used to push"
        // All target positions must be empty
        
        for (Hex marble : column.marbles) {
            Hex target = marble.neighbor(direction);
            
            if (!isValidBoardPosition(target) || 
                board.getOrDefault(target, Player.EMPTY) != Player.EMPTY) {
                return null;
            }
        }
        
        // All targets are empty - valid sidestep move
        Hex representativeTarget = column.marbles.get(0).neighbor(direction);
        return new ValidatedMove(
            column.marbles, representativeTarget, direction,
            MoveType.SIDESTEP_MOVE, false, null
        );
    }
    
    /**
     * Validate Sumito (pushing) rules
     */
    private ValidatedMove validateSumito(Column column, int direction, Hex leadMarble) {
        Player opponent = currentPlayer.getOpponent();
        
        // Count consecutive opponent marbles in push direction
        List<Hex> opponentMarbles = new ArrayList<>();
        Hex current = leadMarble.neighbor(direction);
        
        while (isValidBoardPosition(current) && board.getOrDefault(current, Player.EMPTY) == opponent) {
            opponentMarbles.add(current);
            current = current.neighbor(direction);
        }
        
        if (opponentMarbles.isEmpty()) {
            return null;
        }
        
        // Apply Sumito rules:
        // 2 marbles can push 1
        // 3 marbles can push 1 or 2
        // Cannot push 3 or more
        if (opponentMarbles.size() >= 3) {
            return null; // Cannot push 3 or more opponent marbles
        }
        
        if (column.marbles.size() <= opponentMarbles.size()) {
            return null; // Need numerical superiority
        }
        
        // Check for sandwiched marbles (forbidden to push)
        if (isOpponentSandwiched(opponentMarbles, direction)) {
            return null;
        }
        
        // Check if there's space behind the last opponent marble
        Hex lastOpponent = opponentMarbles.get(opponentMarbles.size() - 1);
        Hex spaceAfter = lastOpponent.neighbor(direction);
        
        // Valid if pushing off board or into empty space
        if (!isValidBoardPosition(spaceAfter) || 
            board.getOrDefault(spaceAfter, Player.EMPTY) == Player.EMPTY) {
            
            // For push moves, the target position should be the first opponent marble
            // This is what the player clicks on in the UI
            Hex targetPosition = opponentMarbles.get(0);
            
            return new ValidatedMove(
                column.marbles, targetPosition, direction,
                MoveType.INLINE_MOVE, true, opponentMarbles
            );
        }
        return null;
    }
    
    /**
     * Check if opponent marbles are sandwiched (have friendly marble behind them)
     */
    private boolean isOpponentSandwiched(List<Hex> opponentMarbles, int direction) {
        if (opponentMarbles.isEmpty()) {
            return false;
        }
        
        Hex lastOpponent = opponentMarbles.get(opponentMarbles.size() - 1);
        Hex behind = lastOpponent.neighbor(direction);
        
        return isValidBoardPosition(behind) && 
               board.getOrDefault(behind, Player.EMPTY) == currentPlayer;
    }
    
    /**
     * Identify and validate a column from selected marbles
     */
    private Column identifyColumn(List<Hex> marbles) {
        if (marbles.size() < 2 || marbles.size() > 3) {
            return null;
        }
        
        // Try each direction to see if marbles form a line
        for (int dir = 0; dir < 6; dir++) {
            if (checkColumnInDirection(marbles, dir)) {
                return new Column(marbles, dir);
            }
        }
        
        return null;
    }
    
    /**
     * Check if marbles form a valid column in given direction
     */
    private boolean checkColumnInDirection(List<Hex> marbles, int direction) {
        // Sort marbles along the direction axis
        List<Hex> sorted = new ArrayList<>(marbles);
        int[] dirVector = Hex.DIRECTIONS[direction];
        
        sorted.sort((a, b) -> {
            int aProj = a.q * dirVector[0] + a.r * dirVector[1];
            int bProj = b.q * dirVector[0] + b.r * dirVector[1];
            return Integer.compare(aProj, bProj);
        });
        
        // Check if they form consecutive positions
        for (int i = 1; i < sorted.size(); i++) {
            Hex expected = sorted.get(i - 1).neighbor(direction);
            if (!expected.equals(sorted.get(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get the lead marble (front) for movement in given direction
     */
    private Hex getLeadMarble(Column column, int direction) {
        int[] dirVector = Hex.DIRECTIONS[direction];
        
        return column.marbles.stream()
            .max((a, b) -> Integer.compare(
                a.q * dirVector[0] + a.r * dirVector[1],
                b.q * dirVector[0] + b.r * dirVector[1]
            ))
            .orElse(column.marbles.get(0));
    }
    
    /**
     * Check if position is valid on the board
     */
    private boolean isValidBoardPosition(Hex position) {
        // Hexagonal board with radius 4
        return Math.abs(position.q) <= 4 && 
               Math.abs(position.r) <= 4 && 
               Math.abs(-position.q - position.r) <= 4;
    }
    
    /**
     * Represents a column of marbles
     */
    private static class Column {
        final List<Hex> marbles;
        final int direction;
        
        Column(List<Hex> marbles, int direction) {
            this.marbles = new ArrayList<>(marbles);
            this.direction = direction;
        }
    }
}