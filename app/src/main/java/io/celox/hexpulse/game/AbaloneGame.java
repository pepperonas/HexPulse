package io.celox.hexpulse.game;

import java.util.*;

import java.util.Stack;

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
    private static final int MAX_UNDO_HISTORY = 10; // Maximum number of moves to keep for undo
    
    // Multi-Undo system
    private Stack<GameSnapshot> undoHistory;
    private int currentMoveNumber;
    
    // Debug functionality
    private Map<Hex, Player> preMoveBoardState;
    private List<Hex> debugSelectedMarbles;
    private Hex debugTargetPosition;
    private Player debugCurrentPlayer;
    
    public AbaloneGame() {
        board = new HashMap<>();
        scores = new HashMap<>();
        selectedMarbles = new ArrayList<>();
        validMoves = new HashSet<>();
        undoHistory = new Stack<>();
        currentMoveNumber = 0;
        preMoveBoardState = null;
        debugSelectedMarbles = new ArrayList<>();
        debugTargetPosition = null;
        debugCurrentPlayer = null;
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
     * FIXED: Enforce maximum 3 marbles and they must be in a straight line
     */
    public boolean selectMarble(Hex position) {
        if (!isValidPosition(position)) {
            return false;
        }
        
        Player marble = board.get(position);
        
        // Can only select current player's marbles
        if (marble == currentPlayer) {
            if (selectedMarbles.contains(position)) {
                // Deselect marble
                selectedMarbles.remove(position);
            } else {
                // Try to select marble
                List<Hex> tempSelection = new ArrayList<>(selectedMarbles);
                tempSelection.add(position);
                
                // Check maximum of 3 marbles
                if (tempSelection.size() > 3) {
                    return false; // Cannot select more than 3 marbles
                }
                
                // Check if all selected marbles form a straight line
                if (tempSelection.size() > 1 && !areMarblesStraightLine(tempSelection)) {
                    return false; // Selected marbles must form a straight line
                }
                
                selectedMarbles.add(position);
            }
            updateValidMoves();
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if marbles form a straight line (helper method for selection validation)
     */
    private boolean areMarblesStraightLine(List<Hex> marbles) {
        if (marbles.size() <= 1) {
            return true;
        }
        
        // Use new MoveValidator to check if marbles form a valid column
        MoveValidator validator = new MoveValidator(board, currentPlayer);
        List<MoveValidator.ValidatedMove> moves = validator.getValidMoves(marbles);
        return !moves.isEmpty(); // If there are valid moves, marbles form a line
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
        
        // Save current state before making the move
        saveGameState();
        
        // Save debug information before move
        saveDebugPreMoveState(targetPosition);
        
        // Find and execute the validated move using new logic
        MoveValidator validator = new MoveValidator(board, currentPlayer);
        List<MoveValidator.ValidatedMove> validatedMoves = validator.getValidMoves(selectedMarbles);
        
        // Find the move that matches the target position
        MoveValidator.ValidatedMove selectedMove = null;
        for (MoveValidator.ValidatedMove move : validatedMoves) {
            if (move.targetPosition.equals(targetPosition)) {
                selectedMove = move;
                break;
            }
        }
        
        if (selectedMove == null) {
            return false;
        }
        
        // Execute the validated move
        MoveExecutor executor = new MoveExecutor(board, scores);
        executor.executeMove(selectedMove, currentPlayer);
        
        // Increment move number and switch players
        currentMoveNumber++;
        currentPlayer = currentPlayer.getOpponent();
        clearSelection();
        
        return true;
    }
    
    /**
     * Update valid moves for current selection using new rule-compliant logic
     */
    private void updateValidMoves() {
        validMoves.clear();
        
        if (selectedMarbles.isEmpty()) {
            return;
        }
        
        // Use new MoveValidator for rule-compliant validation
        MoveValidator validator = new MoveValidator(board, currentPlayer);
        List<MoveValidator.ValidatedMove> validatedMoves = validator.getValidMoves(selectedMarbles);
        
        // Convert ValidatedMoves to target positions for UI
        for (MoveValidator.ValidatedMove move : validatedMoves) {
            validMoves.add(move.targetPosition);
        }
    }
    
    
    
    /**
     * Check for winner
     */
    public Player checkWinner() {
        if (scores.getOrDefault(Player.BLACK, 0) >= WINNING_SCORE) {
            return Player.BLACK;
        }
        if (scores.getOrDefault(Player.WHITE, 0) >= WINNING_SCORE) {
            return Player.WHITE;
        }
        return null;
    }
    
    /**
     * Save current game state to undo history
     */
    private void saveGameState() {
        GameSnapshot snapshot = new GameSnapshot(board, currentPlayer, scores, currentMoveNumber);
        undoHistory.push(snapshot);
        
        // Limit undo history size
        while (undoHistory.size() > MAX_UNDO_HISTORY) {
            undoHistory.removeElementAt(0); // Remove oldest snapshot
        }
    }
    
    /**
     * Undo the last move
     */
    public boolean undoLastMove() {
        if (undoHistory.isEmpty()) {
            return false; // No move to undo
        }
        
        // Restore previous state
        GameSnapshot snapshot = undoHistory.pop();
        board = snapshot.getBoard();
        currentPlayer = snapshot.getCurrentPlayer();
        scores = snapshot.getScores();
        currentMoveNumber = snapshot.getMoveNumber();
        
        // Clear selection and valid moves
        clearSelection();
        
        return true;
    }
    
    /**
     * Undo multiple moves
     */
    public boolean undoMoves(int count) {
        if (count <= 0 || count > undoHistory.size()) {
            return false;
        }
        
        for (int i = 0; i < count; i++) {
            if (!undoLastMove()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }
    
    /**
     * Get number of moves that can be undone
     */
    public int getUndoCount() {
        return undoHistory.size();
    }
    
    /**
     * Get current move number
     */
    public int getCurrentMoveNumber() {
        return currentMoveNumber;
    }
    
    /**
     * Save debug information before making a move
     */
    private void saveDebugPreMoveState(Hex targetPosition) {
        preMoveBoardState = new HashMap<>(board);
        debugSelectedMarbles = new ArrayList<>(selectedMarbles);
        debugTargetPosition = targetPosition;
        debugCurrentPlayer = currentPlayer;
    }
    
    /**
     * Get debug information about the last move
     */
    public DebugMoveInfo getLastMoveDebugInfo() {
        if (preMoveBoardState == null) {
            return null;
        }
        
        return new DebugMoveInfo(
            new HashMap<>(preMoveBoardState),
            new HashMap<>(board),
            new ArrayList<>(debugSelectedMarbles),
            debugTargetPosition,
            debugCurrentPlayer
        );
    }
    
    /**
     * Clear debug information
     */
    public void clearDebugInfo() {
        preMoveBoardState = null;
        debugSelectedMarbles.clear();
        debugTargetPosition = null;
        debugCurrentPlayer = null;
    }
    
    /**
     * Check if debug information is available
     */
    public boolean hasDebugInfo() {
        return preMoveBoardState != null;
    }
    
    /**
     * Reset the game
     */
    public void resetGame() {
        undoHistory.clear();
        currentMoveNumber = 0;
        clearDebugInfo();
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
        this.undoHistory = new Stack<>(); // Don't copy undo state for AI copies
        this.currentMoveNumber = other.currentMoveNumber;
        this.preMoveBoardState = null; // Don't copy debug state for AI copies
        this.debugSelectedMarbles = new ArrayList<>();
        this.debugTargetPosition = null;
        this.debugCurrentPlayer = null;
    }
}