package io.celox.hexpulse.game;

import java.util.*;

/**
 * Core game logic for Abalone
 */
public class AbaloneGame {
    private Map<Hex, Player> board;
    private Player currentPlayer;
    private Map<Player, Integer> scores;
    private List<Hex> selectedMarbles;
    private Set<Hex> validMoves;
    private static final int WINNING_SCORE = 6;
    
    public AbaloneGame() {
        board = new HashMap<>();
        scores = new HashMap<>();
        selectedMarbles = new ArrayList<>();
        validMoves = new HashSet<>();
        initializeGame();
    }
    
    /**
     * Initialize a new game
     */
    private void initializeGame() {
        currentPlayer = Player.BLACK;
        scores.put(Player.BLACK, 0);
        scores.put(Player.WHITE, 0);
        selectedMarbles.clear();
        validMoves.clear();
        createBoard();
        setupInitialPosition();
    }
    
    /**
     * Create the hexagonal board
     */
    private void createBoard() {
        board.clear();
        for (int q = -4; q <= 4; q++) {
            for (int r = -4; r <= 4; r++) {
                int s = -q - r;
                if (s >= -4 && s <= 4) {
                    board.put(new Hex(q, r), Player.EMPTY);
                }
            }
        }
    }
    
    /**
     * Setup initial marble positions
     */
    private void setupInitialPosition() {
        // Black marbles (top)
        List<Hex> blackPositions = Arrays.asList(
            new Hex(-4, 0), new Hex(-3, -1), new Hex(-2, -2), new Hex(-1, -3), new Hex(0, -4),
            new Hex(-4, 1), new Hex(-3, 0), new Hex(-2, -1), new Hex(-1, -2), new Hex(0, -3), new Hex(1, -4),
            new Hex(-2, 0), new Hex(-1, -1), new Hex(0, -2)
        );
        
        // White marbles (bottom)
        List<Hex> whitePositions = Arrays.asList(
            new Hex(4, 0), new Hex(3, 1), new Hex(2, 2), new Hex(1, 3), new Hex(0, 4),
            new Hex(4, -1), new Hex(3, 0), new Hex(2, 1), new Hex(1, 2), new Hex(0, 3), new Hex(-1, 4),
            new Hex(2, 0), new Hex(1, 1), new Hex(0, 2)
        );
        
        for (Hex pos : blackPositions) {
            board.put(pos, Player.BLACK);
        }
        
        for (Hex pos : whitePositions) {
            board.put(pos, Player.WHITE);
        }
    }
    
    /**
     * Get player at position
     */
    public Player getPlayerAt(Hex position) {
        return board.getOrDefault(position, Player.EMPTY);
    }
    
    /**
     * Check if position is valid on board
     */
    public boolean isValidPosition(Hex position) {
        return board.containsKey(position);
    }
    
    /**
     * Get current player
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    
    /**
     * Get scores
     */
    public Map<Player, Integer> getScores() {
        return new HashMap<>(scores);
    }
    
    /**
     * Get selected marbles
     */
    public List<Hex> getSelectedMarbles() {
        return new ArrayList<>(selectedMarbles);
    }
    
    /**
     * Get valid moves for current selection
     */
    public Set<Hex> getValidMoves() {
        return new HashSet<>(validMoves);
    }
    
    /**
     * Select or deselect a marble
     */
    public boolean selectMarble(Hex position) {
        if (!isValidPosition(position)) {
            return false;
        }
        
        Player marble = board.get(position);
        
        // Can only select current player's marbles
        if (marble == currentPlayer) {
            if (selectedMarbles.contains(position)) {
                selectedMarbles.remove(position);
            } else {
                selectedMarbles.add(position);
            }
            updateValidMoves();
            return true;
        }
        
        return false;
    }
    
    /**
     * Clear selection
     */
    public void clearSelection() {
        selectedMarbles.clear();
        validMoves.clear();
    }
    
    /**
     * Attempt to make a move
     */
    public boolean makeMove(Hex targetPosition) {
        if (!validMoves.contains(targetPosition)) {
            return false;
        }
        
        if (selectedMarbles.isEmpty()) {
            return false;
        }
        
        // Find movement direction
        Integer direction = findMoveDirection(targetPosition);
        if (direction == null) {
            return false;
        }
        
        // Execute the move
        if (isInlineMove(direction)) {
            executeInlineMove(direction);
        } else {
            executeBroadsideMove(direction);
        }
        
        // Switch players and clear selection
        currentPlayer = currentPlayer.getOpponent();
        clearSelection();
        
        return true;
    }
    
    /**
     * Update valid moves for current selection
     */
    private void updateValidMoves() {
        validMoves.clear();
        
        if (selectedMarbles.isEmpty()) {
            return;
        }
        
        // Check if all selected marbles belong to current player
        for (Hex marble : selectedMarbles) {
            if (board.get(marble) != currentPlayer) {
                validMoves.clear();
                return;
            }
        }
        
        if (selectedMarbles.size() == 1) {
            // Single marble - check all 6 directions
            Hex marble = selectedMarbles.get(0);
            for (int dir = 0; dir < 6; dir++) {
                Hex target = marble.neighbor(dir);
                if (isValidPosition(target) && board.get(target) == Player.EMPTY) {
                    validMoves.add(target);
                }
            }
        } else if (selectedMarbles.size() <= 3 && areMarblesInLine()) {
            // Multiple marbles in line
            for (int dir = 0; dir < 6; dir++) {
                if (canMoveInDirection(dir)) {
                    Hex target = getTargetForDirection(dir);
                    if (target != null) {
                        validMoves.add(target);
                    }
                }
            }
        }
    }
    
    /**
     * Check if selected marbles are in a line
     */
    private boolean areMarblesInLine() {
        if (selectedMarbles.size() <= 1) {
            return true;
        }
        
        return getLineDirection() != null;
    }
    
    /**
     * Get the direction of the line of marbles
     */
    private Integer getLineDirection() {
        if (selectedMarbles.size() < 2) {
            return null;
        }
        
        // Sort marbles for consistent processing
        List<Hex> sortedMarbles = new ArrayList<>(selectedMarbles);
        sortedMarbles.sort((a, b) -> {
            if (a.q != b.q) return Integer.compare(a.q, b.q);
            return Integer.compare(a.r, b.r);
        });
        
        // Check all 6 directions
        for (int dir = 0; dir < 6; dir++) {
            if (checkLineInDirection(sortedMarbles, dir)) {
                return dir;
            }
        }
        
        return null;
    }
    
    /**
     * Check if marbles form a line in given direction
     */
    private boolean checkLineInDirection(List<Hex> marbles, int direction) {
        int[] dirVector = Hex.DIRECTIONS[direction];
        
        for (int i = 1; i < marbles.size(); i++) {
            int expectedQ = marbles.get(0).q + dirVector[0] * i;
            int expectedR = marbles.get(0).r + dirVector[1] * i;
            
            boolean found = false;
            for (Hex marble : marbles) {
                if (marble.q == expectedQ && marble.r == expectedR) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if movement in direction is possible
     */
    private boolean canMoveInDirection(int direction) {
        if (selectedMarbles.size() == 1) {
            Hex target = selectedMarbles.get(0).neighbor(direction);
            return isValidPosition(target) && board.get(target) == Player.EMPTY;
        }
        
        // For multiple marbles, check inline and broadside moves
        return canMoveInline(direction) || canMoveBroadside(direction);
    }
    
    /**
     * Check if inline move is possible
     */
    private boolean canMoveInline(int direction) {
        Integer lineDir = getLineDirection();
        if (lineDir == null) {
            return false;
        }
        
        // Inline move if direction is parallel to line
        if (lineDir == direction || lineDir == (direction + 3) % 6) {
            Hex leadMarble = getLeadMarble(direction);
            Hex target = leadMarble.neighbor(direction);
            
            if (!isValidPosition(target)) {
                return false;
            }
            
            Player targetPlayer = board.get(target);
            
            // Empty target - simple move
            if (targetPlayer == Player.EMPTY) {
                return true;
            }
            
            // Own marble - not allowed
            if (targetPlayer == currentPlayer) {
                return false;
            }
            
            // Opponent marble - check if can push (Sumito)
            return canPush(direction);
        }
        
        return false;
    }
    
    /**
     * Check if broadside move is possible
     */
    private boolean canMoveBroadside(int direction) {
        if (selectedMarbles.size() < 2) {
            return false;
        }
        
        Integer lineDir = getLineDirection();
        if (lineDir == null) {
            return false;
        }
        
        // Broadside move if direction is perpendicular to line
        if (lineDir != direction && lineDir != (direction + 3) % 6) {
            // Check if all target positions are empty
            for (Hex marble : selectedMarbles) {
                Hex target = marble.neighbor(direction);
                if (!isValidPosition(target) || board.get(target) != Player.EMPTY) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the lead marble for inline movement
     */
    private Hex getLeadMarble(int direction) {
        int[] dirVector = Hex.DIRECTIONS[direction];
        
        return selectedMarbles.stream()
            .max((a, b) -> Integer.compare(
                a.q * dirVector[0] + a.r * dirVector[1],
                b.q * dirVector[0] + b.r * dirVector[1]
            ))
            .orElse(selectedMarbles.get(0));
    }
    
    /**
     * Check if can push opponent marbles (Sumito)
     */
    private boolean canPush(int direction) {
        Hex leadMarble = getLeadMarble(direction);
        Player opponent = currentPlayer.getOpponent();
        
        // Count opponent marbles in push direction
        List<Hex> opponentMarbles = new ArrayList<>();
        Hex current = leadMarble.neighbor(direction);
        
        while (isValidPosition(current) && board.get(current) == opponent) {
            opponentMarbles.add(current);
            current = current.neighbor(direction);
        }
        
        // Need numerical superiority
        if (selectedMarbles.size() <= opponentMarbles.size()) {
            return false;
        }
        
        // Check if space behind last opponent marble
        if (!opponentMarbles.isEmpty()) {
            Hex lastOpponent = opponentMarbles.get(opponentMarbles.size() - 1);
            Hex behind = lastOpponent.neighbor(direction);
            
            // Can push off board or to empty space
            return !isValidPosition(behind) || board.get(behind) == Player.EMPTY;
        }
        
        return false;
    }
    
    /**
     * Find movement direction to target
     */
    private Integer findMoveDirection(Hex target) {
        if (selectedMarbles.size() == 1) {
            Hex marble = selectedMarbles.get(0);
            for (int dir = 0; dir < 6; dir++) {
                if (marble.neighbor(dir).equals(target)) {
                    return dir;
                }
            }
        } else {
            for (int dir = 0; dir < 6; dir++) {
                Hex expectedTarget = getTargetForDirection(dir);
                if (target.equals(expectedTarget)) {
                    return dir;
                }
            }
        }
        return null;
    }
    
    /**
     * Get target position for direction
     */
    private Hex getTargetForDirection(int direction) {
        if (canMoveInline(direction)) {
            Hex leadMarble = getLeadMarble(direction);
            return leadMarble.neighbor(direction);
        } else if (canMoveBroadside(direction)) {
            return selectedMarbles.get(0).neighbor(direction);
        }
        return null;
    }
    
    /**
     * Check if this is an inline move
     */
    private boolean isInlineMove(int direction) {
        if (selectedMarbles.size() == 1) {
            return true;
        }
        
        Integer lineDir = getLineDirection();
        return lineDir != null && (lineDir == direction || lineDir == (direction + 3) % 6);
    }
    
    /**
     * Execute inline move
     */
    private void executeInlineMove(int direction) {
        // Sort marbles in movement direction
        int[] dirVector = Hex.DIRECTIONS[direction];
        List<Hex> sortedMarbles = new ArrayList<>(selectedMarbles);
        sortedMarbles.sort((a, b) -> Integer.compare(
            b.q * dirVector[0] + b.r * dirVector[1],
            a.q * dirVector[0] + a.r * dirVector[1]
        ));
        
        Hex leadMarble = sortedMarbles.get(0);
        Hex target = leadMarble.neighbor(direction);
        
        // Handle pushing if needed
        if (isValidPosition(target) && board.get(target) != Player.EMPTY) {
            pushOpponentMarbles(leadMarble, direction);
        }
        
        // Move own marbles
        List<Hex> oldPositions = new ArrayList<>(sortedMarbles);
        for (Hex marble : oldPositions) {
            board.put(marble, Player.EMPTY);
        }
        
        for (Hex marble : oldPositions) {
            Hex newPos = marble.neighbor(direction);
            board.put(newPos, currentPlayer);
        }
    }
    
    /**
     * Execute broadside move
     */
    private void executeBroadsideMove(int direction) {
        List<Hex> oldPositions = new ArrayList<>(selectedMarbles);
        
        // Clear old positions
        for (Hex marble : oldPositions) {
            board.put(marble, Player.EMPTY);
        }
        
        // Set new positions
        for (Hex marble : oldPositions) {
            Hex newPos = marble.neighbor(direction);
            board.put(newPos, currentPlayer);
        }
    }
    
    /**
     * Push opponent marbles
     */
    private void pushOpponentMarbles(Hex fromPosition, int direction) {
        Player opponent = currentPlayer.getOpponent();
        List<Hex> marblesToPush = new ArrayList<>();
        
        Hex current = fromPosition.neighbor(direction);
        while (isValidPosition(current) && board.get(current) == opponent) {
            marblesToPush.add(current);
            current = current.neighbor(direction);
        }
        
        // Push from back to front
        Collections.reverse(marblesToPush);
        for (Hex marble : marblesToPush) {
            Hex newPos = marble.neighbor(direction);
            
            if (!isValidPosition(newPos)) {
                // Marble pushed off board
                board.put(marble, Player.EMPTY);
                scores.put(currentPlayer, scores.get(currentPlayer) + 1);
            } else {
                // Move marble to new position
                board.put(newPos, opponent);
                board.put(marble, Player.EMPTY);
            }
        }
    }
    
    /**
     * Check for winner
     */
    public Player checkWinner() {
        if (scores.get(Player.BLACK) >= WINNING_SCORE) {
            return Player.BLACK;
        }
        if (scores.get(Player.WHITE) >= WINNING_SCORE) {
            return Player.WHITE;
        }
        return null;
    }
    
    /**
     * Reset the game
     */
    public void resetGame() {
        initializeGame();
    }
    
    /**
     * Get all board positions
     */
    public Set<Hex> getAllPositions() {
        return new HashSet<>(board.keySet());
    }
    
    /**
     * Copy constructor for AI
     */
    public AbaloneGame(AbaloneGame other) {
        this.board = new HashMap<>(other.board);
        this.currentPlayer = other.currentPlayer;
        this.scores = new HashMap<>(other.scores);
        this.selectedMarbles = new ArrayList<>(other.selectedMarbles);
        this.validMoves = new HashSet<>(other.validMoves);
    }
}