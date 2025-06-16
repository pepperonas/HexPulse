package io.celox.hexpulse.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.celox.hexpulse.R;
import io.celox.hexpulse.databinding.FragmentGalleryBinding;
import io.celox.hexpulse.game.AIDifficulty;
import io.celox.hexpulse.game.AbaloneAI;
import io.celox.hexpulse.game.AbaloneGame;
import io.celox.hexpulse.game.Hex;
import io.celox.hexpulse.game.MoveValidator;
import io.celox.hexpulse.game.Player;
import io.celox.hexpulse.game.Theme;
import io.celox.hexpulse.network.GameClient;
import io.celox.hexpulse.settings.GameSettings;
import io.celox.hexpulse.ui.views.HexagonalBoardView;
import io.celox.hexpulse.debug.DebugLogger;

import android.widget.PopupMenu;

public class GalleryFragment extends Fragment implements HexagonalBoardView.BoardTouchListener, GameClient.GameEventListener {

    private FragmentGalleryBinding binding;
    private AbaloneGame game;
    private AbaloneAI ai;
    private String gameMode = "PVP"; // Default to Player vs Player
    private boolean isAiThinking = false;
    private Hex pendingMoveTarget = null; // Target position for pending move
    private List<Hex> pendingSelectedMarbles = new ArrayList<>(); // Selected marbles for pending move
    
    // Online game variables
    private GameClient gameClient;
    private String roomCode;
    private String playerId;
    private boolean isHost;
    private Player myPlayerColor;
    private boolean isOnlineGame = false;
    
    // Debug functionality
    private DebugLogger debugLogger;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Get game mode from arguments
        if (getArguments() != null) {
            gameMode = getArguments().getString("game_mode", "PVP");
            
            // Handle online game mode
            if ("ONLINE".equals(gameMode)) {
                isOnlineGame = true;
                roomCode = getArguments().getString("room_code");
                playerId = getArguments().getString("player_id");
                isHost = getArguments().getBoolean("is_host", false);
                
                android.util.Log.d("GalleryFragment", "Online mode - roomCode: " + roomCode + 
                    ", playerId: " + playerId + ", isHost: " + isHost);
                
                // Initialize GameClient
                gameClient = GameClient.getInstance();
                android.util.Log.d("GalleryFragment", "GalleryFragment - GameClient instance: " + gameClient.hashCode());
                // CRITICAL: Set this as the event listener to receive move-made events
                gameClient.setEventListener(this);
                android.util.Log.d("GalleryFragment", "GalleryFragment set as GameClient event listener");
                
                // Set initial player color based on host status (will be updated by onRoomJoined)
                myPlayerColor = isHost ? Player.BLACK : Player.WHITE;
                android.util.Log.d("GalleryFragment", "Initial player color set: " + myPlayerColor);
                
                android.util.Log.d("GalleryFragment", "GameClient state - isConnected: " + gameClient.isConnected() + 
                    ", roomCode: " + gameClient.getCurrentRoomCode() + ", playerId: " + gameClient.getPlayerId());
                
                // Always set room information from fragment arguments
                android.util.Log.d("GalleryFragment", "Setting room info in GameClient - roomCode: " + roomCode + ", playerId: " + playerId + ", isHost: " + isHost);
                gameClient.setRoomInfo(roomCode, playerId, isHost);
                
                // Only reconnect if not connected
                if (!gameClient.isConnected()) {
                    android.util.Log.d("GalleryFragment", "Socket not connected, connecting...");
                    gameClient.connect();
                    
                    // Wait a moment for connection to establish and rejoin room
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        android.util.Log.d("GalleryFragment", "Post-connect check - isConnected: " + gameClient.isConnected());
                        if (gameClient.isConnected()) {
                            android.util.Log.d("GalleryFragment", "Rejoining room after reconnect");
                            gameClient.joinRoom(roomCode, playerId, isHost);
                        }
                    }, 1000);
                } else {
                    android.util.Log.d("GalleryFragment", "Socket already connected, no reconnection needed");
                }
            }
        }

        // Initialize game
        initializeGame();
        setupUI();
        updateUI();

        return root;
    }

    private void initializeGame() {
        game = new AbaloneGame();
        
        // Initialize debug logger
        debugLogger = new DebugLogger(getContext());
        
        // Get settings
        GameSettings settings = GameSettings.getInstance(requireContext());
        
        // Setup AI if needed
        if ("AI".equals(gameMode)) {
            AIDifficulty difficulty = settings.getAIDifficulty();
            ai = new AbaloneAI(difficulty);
        } else {
            ai = null;
        }
        
        // Set up board
        binding.hexagonalBoard.setGame(game);
        binding.hexagonalBoard.setBoardTouchListener(this);
        
        // Apply theme from settings
        Theme selectedTheme = settings.getTheme();
        binding.hexagonalBoard.setTheme(selectedTheme);
    }

    private void setupUI() {
        // Set up button listeners
        binding.btnResetGame.setOnClickListener(v -> resetGame());
        binding.btnUndo.setOnClickListener(v -> undoLastMove());
        binding.btnUndo.setOnLongClickListener(v -> {
            showUndoMenu(v);
            return true;
        });
        binding.btnClearSelection.setOnClickListener(v -> clearSelection());
        binding.btnLogCriticalMove.setOnClickListener(v -> logCriticalMove());
        
        // Update button states
        updateUndoButton();
        updateDebugButton();
        updateDebugButtonsVisibility();
    }

    private void updateUI() {
        if (game == null) return;

        // Update current player text
        String currentPlayerText = getString(R.string.current_player, 
            game.getCurrentPlayer() == Player.BLACK ? 
            getString(R.string.black_player) : getString(R.string.white_player));
        binding.textCurrentPlayer.setText(currentPlayerText);

        // Update undo button state
        updateUndoButton();
        
        // Update debug button state
        updateDebugButton();

        // Update scores
        Map<Player, Integer> scores = game.getScores();
        String scoresText = getString(R.string.scores_display, 
            scores.get(Player.BLACK), scores.get(Player.WHITE));
        binding.textScores.setText(scoresText);

        // Check for winner
        Player winner = game.checkWinner();
        if (winner != null) {
            String winnerText = getString(R.string.game_winner, 
                winner == Player.BLACK ? getString(R.string.black_player) : getString(R.string.white_player));
            binding.textWinner.setText(winnerText);
            binding.textWinner.setVisibility(View.VISIBLE);
        } else {
            binding.textWinner.setVisibility(View.GONE);
        }

        // Update board
        binding.hexagonalBoard.invalidate();

        // Handle AI turn
        if (ai != null && game.getCurrentPlayer() == Player.WHITE && winner == null && !isAiThinking) {
            makeAiMove();
        }
    }

    private void makeAiMove() {
        isAiThinking = true;
        binding.textAiThinking.setVisibility(View.VISIBLE);

        ai.getBestMoveAsync(game, Player.WHITE, new AbaloneAI.MoveCallback() {
            @Override
            public void onMoveCalculated(AbaloneAI.Move move) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isAiThinking = false;
                        binding.textAiThinking.setVisibility(View.GONE);

                        if (move != null) {
                            // Execute AI move with animation
                            game.clearSelection();
                            for (Hex marble : move.selectedMarbles) {
                                game.selectMarble(marble);
                            }
                            
                            // Store target and selected marbles for animation
                            pendingMoveTarget = move.target;
                            pendingSelectedMarbles = new ArrayList<>(move.selectedMarbles);
                            
                            // Get validated move information for AI push animation
                            MoveValidator.ValidatedMove moveInfo = game.getValidatedMoveForTarget(move.target);
                            if (moveInfo != null && moveInfo.isPush && !moveInfo.pushedMarbles.isEmpty()) {
                                // Use animation with push for AI moves that push opponent marbles
                                binding.hexagonalBoard.animateMoveWithPush(move.selectedMarbles, moveInfo.pushedMarbles, moveInfo.direction);
                            } else {
                                // Use regular animation for non-push AI moves
                                binding.hexagonalBoard.animateMove(move.selectedMarbles, move.target);
                            }
                        } else {
                            Toast.makeText(getContext(), "AI couldn't find a move", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onMoveError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isAiThinking = false;
                        binding.textAiThinking.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "AI Error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onHexTouched(Hex position) {
        if (game == null || isAiThinking || binding.hexagonalBoard.isAnimating()) return;

        // In AI mode, only allow human player (BLACK) to move
        if (ai != null && game.getCurrentPlayer() == Player.WHITE) {
            return;
        }
        
        // In online mode, only allow moves when it's our turn
        if (isOnlineGame && myPlayerColor != null && game.getCurrentPlayer() != myPlayerColor) {
            android.util.Log.d("GalleryFragment", "Turn blocked - currentPlayer: " + game.getCurrentPlayer() + 
                ", myPlayerColor: " + myPlayerColor);
            Toast.makeText(getContext(), "Wait for your turn! Current: " + game.getCurrentPlayer() + 
                ", You: " + myPlayerColor, Toast.LENGTH_SHORT).show();
            return;
        }

        Player playerAtPosition = game.getPlayerAt(position);

        // DEBUG LOGGING FOR SUMITO CLICK
        android.util.Log.d("GalleryFragment", "=== HEX TOUCHED DEBUG ===");
        android.util.Log.d("GalleryFragment", "Clicked position: " + position);
        android.util.Log.d("GalleryFragment", "Player at position: " + playerAtPosition);
        android.util.Log.d("GalleryFragment", "Current player: " + game.getCurrentPlayer());
        android.util.Log.d("GalleryFragment", "Selected marbles: " + game.getSelectedMarbles());
        android.util.Log.d("GalleryFragment", "Valid moves: " + game.getValidMoves());
        android.util.Log.d("GalleryFragment", "Is valid move: " + game.getValidMoves().contains(position));

        // If clicking on current player's marble, select/deselect it
        if (playerAtPosition == game.getCurrentPlayer()) {
            android.util.Log.d("GalleryFragment", "CASE 1: Selecting/deselecting own marble");
            game.selectMarble(position);
            updateUI();
        }
        // If clicking on valid move position, make the move with animation
        else if (game.getValidMoves().contains(position)) {
            android.util.Log.d("GalleryFragment", "CASE 2: Executing valid move");
            List<Hex> selectedMarbles = game.getSelectedMarbles();
            if (!selectedMarbles.isEmpty()) {
                // Store target and selected marbles for later execution
                pendingMoveTarget = position;
                pendingSelectedMarbles = new ArrayList<>(selectedMarbles); // Copy the list
                
                // Get validated move information for push animation
                MoveValidator.ValidatedMove moveInfo = game.getValidatedMoveForTarget(position);
                if (moveInfo != null && moveInfo.isPush && !moveInfo.pushedMarbles.isEmpty()) {
                    // Use animation with push for moves that push opponent marbles
                    binding.hexagonalBoard.animateMoveWithPush(selectedMarbles, moveInfo.pushedMarbles, moveInfo.direction);
                } else {
                    // Use regular animation for non-push moves
                    binding.hexagonalBoard.animateMove(selectedMarbles, position);
                }
                // Move will be executed when animation completes
            } else {
                android.util.Log.w("GalleryFragment", "No marbles selected for valid move!");
            }
        }
        // If clicking elsewhere, clear selection
        else {
            android.util.Log.d("GalleryFragment", "CASE 3: Clearing selection");
            clearSelection();
        }
        android.util.Log.d("GalleryFragment", "=== END HEX TOUCHED DEBUG ===");
    }
    
    @Override
    public void onAnimationComplete() {
        // Execute the actual move after animation
        if (game != null && pendingMoveTarget != null) {
            android.util.Log.d("GalleryFragment", "Executing local move. Current player before: " + 
                game.getCurrentPlayer());
                
            if (game.makeMove(pendingMoveTarget)) {
                android.util.Log.d("GalleryFragment", "Local move executed. Current player after: " + 
                    game.getCurrentPlayer());
                    
                // Send move to server if in online mode
                if (isOnlineGame && gameClient != null) {
                    sendMoveToServer(pendingMoveTarget, pendingSelectedMarbles);
                }
                pendingMoveTarget = null;
                pendingSelectedMarbles.clear();
                updateUI();
            } else {
                Toast.makeText(getContext(), "Move execution failed", Toast.LENGTH_SHORT).show();
                pendingMoveTarget = null;
                pendingSelectedMarbles.clear();
            }
        }
    }
    
    private void sendMoveToServer(Hex target, List<Hex> selectedMarbles) {
        android.util.Log.d("GalleryFragment", "=== SEND MOVE TO SERVER START ===");
        android.util.Log.d("GalleryFragment", "sendMoveToServer - INPUT: target=" + target);
        android.util.Log.d("GalleryFragment", "sendMoveToServer - GAME STATE: isOnlineGame=" + isOnlineGame + ", myPlayerColor=" + myPlayerColor);
        android.util.Log.d("GalleryFragment", "sendMoveToServer - CLIENT STATE: gameClient=" + (gameClient != null));
        
        if (gameClient != null) {
            android.util.Log.d("GalleryFragment", "sendMoveToServer - GAMECLIENT INSTANCE: " + gameClient.hashCode());
            android.util.Log.d("GalleryFragment", "sendMoveToServer - GAMECLIENT ROOM STATE: roomCode=" + 
                gameClient.getCurrentRoomCode() + ", playerId=" + gameClient.getPlayerId() + 
                ", isConnected=" + gameClient.isConnected());
        }
        
        // Only send move if we're in an online game and have a valid player color
        if (!isOnlineGame || myPlayerColor == null || gameClient == null) {
            android.util.Log.w("GalleryFragment", "sendMoveToServer - SKIPPING: isOnlineGame=" + isOnlineGame + 
                ", myPlayerColor=" + myPlayerColor + ", gameClient=" + (gameClient != null));
            return;
        }
        
        try {
            JSONObject moveData = new JSONObject();
            moveData.put("target", target.toString());
            moveData.put("player", myPlayerColor.toString());
            
            // Add selected marbles information as JSON array
            JSONArray marblesArray = new JSONArray();
            for (Hex marble : selectedMarbles) {
                marblesArray.put(marble.toString());
            }
            moveData.put("selectedMarbles", marblesArray);
            moveData.put("marbleCount", selectedMarbles.size());
            
            android.util.Log.d("GalleryFragment", "sendMoveToServer - MOVE DATA: " + moveData);
            android.util.Log.d("GalleryFragment", "sendMoveToServer - Selected marbles: " + selectedMarbles.size());
            android.util.Log.d("GalleryFragment", "sendMoveToServer - CALLING gameClient.makeMove()");
            
            gameClient.makeMove(moveData);
            
            android.util.Log.d("GalleryFragment", "sendMoveToServer - SUCCESS: Move sent to server");
        } catch (JSONException e) {
            android.util.Log.e("GalleryFragment", "sendMoveToServer - ERROR: Failed to create move JSON", e);
            Toast.makeText(getContext(), "Failed to send move", Toast.LENGTH_SHORT).show();
        }
        android.util.Log.d("GalleryFragment", "=== SEND MOVE TO SERVER END ===");
    }

    private void resetGame() {
        if (game != null) {
            game.resetGame();
            updateUI();
            updateUndoButton();
            updateDebugButton();
        }
    }

    private void undoLastMove() {
        if (game != null && game.canUndo()) {
            // Don't allow undo during AI thinking or animations
            if (isAiThinking || binding.hexagonalBoard.isAnimating()) {
                return;
            }
            
            // Don't allow undo in online games
            if (isOnlineGame) {
                Toast.makeText(getContext(), "Undo not available in online games", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (game.undoLastMove()) {
                updateUI();
                updateUndoButton();
                Toast.makeText(getContext(), "Move undone", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearSelection() {
        if (game != null) {
            game.clearSelection();
            updateUI();
        }
    }
    
    private void updateUndoButton() {
        if (game != null && binding != null) {
            boolean canUndo = game.canUndo() && !isOnlineGame && !isAiThinking;
            binding.btnUndo.setEnabled(canUndo);
            
            // Update button text to show undo count
            if (canUndo) {
                int undoCount = game.getUndoCount();
                binding.btnUndo.setText(getString(R.string.undo_move) + " (" + undoCount + ")");
            } else {
                binding.btnUndo.setText(getString(R.string.undo_move));
            }
        }
    }
    
    private void showUndoMenu(View anchor) {
        if (game == null || !game.canUndo()) {
            return;
        }
        
        int undoCount = game.getUndoCount();
        if (undoCount <= 1) {
            // Only one move to undo, just do it directly
            undoLastMove();
            return;
        }
        
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        
        // Add undo options
        popup.getMenu().add(0, 1, 0, "Undo 1 move");
        
        if (undoCount >= 2) {
            popup.getMenu().add(0, 2, 0, "Undo 2 moves");
        }
        if (undoCount >= 3) {
            popup.getMenu().add(0, 3, 0, "Undo 3 moves");
        }
        if (undoCount >= 5) {
            popup.getMenu().add(0, 5, 0, "Undo 5 moves");
        }
        if (undoCount >= 2) {
            popup.getMenu().add(0, undoCount, 0, "Undo all (" + undoCount + " moves)");
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int movesToUndo = item.getItemId();
            if (movesToUndo == undoCount) {
                // Undo all moves
                undoMoves(undoCount);
            } else {
                // Undo specific number of moves
                undoMoves(movesToUndo);
            }
            return true;
        });
        
        popup.show();
    }
    
    /**
     * Undo multiple moves
     */
    private void undoMoves(int count) {
        if (game != null && game.undoMoves(count)) {
            updateUI();
            updateUndoButton();
            Toast.makeText(getContext(), "Undone " + count + " moves", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to undo " + count + " moves", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateDebugButton() {
        if (game != null && binding != null) {
            boolean hasDebugInfo = game.hasDebugInfo() && !isAiThinking;
            binding.btnLogCriticalMove.setEnabled(hasDebugInfo);
        }
    }
    
    private void updateDebugButtonsVisibility() {
        if (binding != null) {
            GameSettings settings = GameSettings.getInstance(requireContext());
            boolean debugModeEnabled = settings.isDebugModeEnabled();
            
            // Show/hide Undo button based on debug mode
            binding.btnUndo.setVisibility(debugModeEnabled ? View.VISIBLE : View.GONE);
            
            // Show/hide Log Critical Move button based on debug mode
            binding.btnLogCriticalMove.setVisibility(debugModeEnabled ? View.VISIBLE : View.GONE);
        }
    }
    
    private void logCriticalMove() {
        if (game != null && game.hasDebugInfo() && debugLogger != null) {
            try {
                io.celox.hexpulse.game.DebugMoveInfo debugInfo = game.getLastMoveDebugInfo();
                if (debugInfo != null) {
                    debugLogger.logCriticalMove(
                        debugInfo.getBeforeState(),
                        debugInfo.getAfterState(),
                        debugInfo.getSelectedMarbles(),
                        debugInfo.getTargetPosition(),
                        debugInfo.getCurrentPlayer(),
                        "Critical move logged by user"
                    );
                    
                    String logPath = debugLogger.getLogDirectoryPath();
                    Toast.makeText(getContext(), 
                        "Critical move logged!\nPath: " + logPath, 
                        Toast.LENGTH_LONG).show();
                    
                    // Clear debug info after logging
                    game.clearDebugInfo();
                    updateDebugButton();
                } else {
                    Toast.makeText(getContext(), "No debug information available", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to log critical move: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // GameClient.GameEventListener implementation
    @Override
    public void onConnected() {
        // Connection handled by OnlineGameFragment
    }

    @Override
    public void onDisconnected() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Disconnected from game server", Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onRoomJoined(String roomCode, String playerColor) {
        // Determine player color
        myPlayerColor = "black".equalsIgnoreCase(playerColor) ? Player.BLACK : Player.WHITE;
        
        android.util.Log.d("GalleryFragment", "onRoomJoined - roomCode: " + roomCode + 
            ", playerColor: " + playerColor + ", myPlayerColor: " + myPlayerColor);
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), 
                    "You are playing as: " + (myPlayerColor == Player.BLACK ? "BLACK" : "WHITE"), 
                    Toast.LENGTH_LONG).show();
                updateUI();
            });
        }
    }

    @Override
    public void onPlayerJoined(String playerId, String playerColor) {
        // Handle when another player joins
    }

    @Override
    public void onPlayerDisconnected(String playerId) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Opponent disconnected", Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onGameStarted() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Game started!", Toast.LENGTH_SHORT).show();
                updateUI();
            });
        }
    }

    @Override
    public void onMoveMade(String playerId, JSONObject move) {
        android.util.Log.d("GalleryFragment", "=== GALLERYFRAGMENT MOVE-MADE RECEIVED ===");
        android.util.Log.d("GalleryFragment", "onMoveMade - from playerId: " + playerId + ", move: " + move);
        android.util.Log.d("GalleryFragment", "onMoveMade - my playerId: " + this.playerId);
        
        // Only process moves from other players
        if (!this.playerId.equals(playerId)) {
            android.util.Log.d("GalleryFragment", "onMoveMade - PROCESSING OPPONENT MOVE");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        // Parse and execute opponent's move
                        android.util.Log.d("GalleryFragment", "onMoveMade - EXECUTING OPPONENT MOVE");
                        executeOpponentMove(move);
                        updateUI();
                        android.util.Log.d("GalleryFragment", "onMoveMade - OPPONENT MOVE EXECUTED AND UI UPDATED");
                    } catch (JSONException e) {
                        android.util.Log.e("GalleryFragment", "onMoveMade - ERROR processing opponent move", e);
                        Toast.makeText(getContext(), "Error processing opponent's move", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                android.util.Log.w("GalleryFragment", "onMoveMade - Activity is null, cannot process move");
            }
        } else {
            android.util.Log.d("GalleryFragment", "onMoveMade - IGNORING OWN MOVE");
        }
        android.util.Log.d("GalleryFragment", "=== GALLERYFRAGMENT MOVE-MADE END ===");
    }
    
    private void executeOpponentMove(JSONObject moveData) throws JSONException {
        android.util.Log.d("GalleryFragment", "executeOpponentMove - START");
        
        android.util.Log.d("GalleryFragment", "executeOpponentMove - moveData: " + moveData);
        
        // Parse the move and execute it on our local game
        String targetStr = moveData.getString("target");
        Hex target = Hex.fromString(targetStr);
        
        if (target == null) {
            android.util.Log.e("GalleryFragment", "executeOpponentMove - Invalid target: " + targetStr);
            return;
        }
        
        android.util.Log.d("GalleryFragment", "executeOpponentMove - Target position: " + target);
        android.util.Log.d("GalleryFragment", "executeOpponentMove - Current player before move: " + game.getCurrentPlayer());
        
        // Parse selected marbles if available
        List<Hex> selectedMarbles = new ArrayList<>();
        if (moveData.has("selectedMarbles")) {
            try {
                Object selectedMarblesObj = moveData.get("selectedMarbles");
                if (selectedMarblesObj instanceof JSONArray) {
                    JSONArray marblesArray = (JSONArray) selectedMarblesObj;
                    for (int i = 0; i < marblesArray.length(); i++) {
                        Hex marble = Hex.fromString(marblesArray.getString(i));
                        if (marble != null) {
                            selectedMarbles.add(marble);
                        }
                    }
                } else {
                    // Fallback: parse as string
                    selectedMarbles = parseSelectedMarbles(selectedMarblesObj.toString());
                }
            } catch (Exception e) {
                android.util.Log.w("GalleryFragment", "executeOpponentMove - Failed to parse selectedMarbles", e);
            }
        }
        
        Player opponentColor = (myPlayerColor == Player.WHITE) ? Player.BLACK : Player.WHITE;
        android.util.Log.d("GalleryFragment", "executeOpponentMove - Opponent color: " + opponentColor);
        android.util.Log.d("GalleryFragment", "executeOpponentMove - Selected marbles count: " + selectedMarbles.size());
        
        boolean moveMade = false;
        
        // If we have the selected marbles info, use it for precise move execution
        if (!selectedMarbles.isEmpty()) {
            android.util.Log.d("GalleryFragment", "executeOpponentMove - Using precise marble selection");
            
            // Clear and select the exact marbles
            game.clearSelection();
            for (Hex marble : selectedMarbles) {
                android.util.Log.d("GalleryFragment", "executeOpponentMove - Selecting marble: " + marble);
                game.selectMarble(marble);
            }
            
            // Verify selection matches
            List<Hex> currentSelection = game.getSelectedMarbles();
            android.util.Log.d("GalleryFragment", "executeOpponentMove - Current selection: " + currentSelection.size() + " marbles");
            
            if (game.getValidMoves().contains(target)) {
                android.util.Log.d("GalleryFragment", "executeOpponentMove - Target is valid, executing move with animation");
                
                // Get validated move information for opponent push animation
                MoveValidator.ValidatedMove moveInfo = game.getValidatedMoveForTarget(target);
                if (moveInfo != null && moveInfo.isPush && !moveInfo.pushedMarbles.isEmpty()) {
                    // Store move data for execution after animation
                    pendingMoveTarget = target;
                    pendingSelectedMarbles = new ArrayList<>(selectedMarbles);
                    // Use animation with push for opponent moves that push our marbles
                    binding.hexagonalBoard.animateMoveWithPush(selectedMarbles, moveInfo.pushedMarbles, moveInfo.direction);
                    moveMade = true; // Mark as handled (will be executed after animation)
                } else {
                    // Store move data for execution after animation
                    pendingMoveTarget = target;
                    pendingSelectedMarbles = new ArrayList<>(selectedMarbles);
                    // Use regular animation for non-push opponent moves
                    binding.hexagonalBoard.animateMove(selectedMarbles, target);
                    moveMade = true; // Mark as handled (will be executed after animation)
                }
            } else {
                android.util.Log.w("GalleryFragment", "executeOpponentMove - Target not valid for selected marbles");
            }
        }
        
        // Fallback: find any valid marble that can move to target (for backward compatibility)
        if (!moveMade) {
            android.util.Log.d("GalleryFragment", "executeOpponentMove - Using fallback single marble detection");
            
            for (Hex marblePos : game.getAllPositions()) {
                Player marbleColor = game.getPlayerAt(marblePos);
                
                if (marbleColor == opponentColor) {
                    game.clearSelection();
                    game.selectMarble(marblePos);
                    
                    if (game.getValidMoves().contains(target)) {
                        android.util.Log.d("GalleryFragment", "executeOpponentMove - Found valid move from " + marblePos + " to " + target);
                        
                        // Get current selection for animation
                        List<Hex> currentSelection = game.getSelectedMarbles();
                        
                        // Get validated move information for opponent push animation
                        MoveValidator.ValidatedMove moveInfo = game.getValidatedMoveForTarget(target);
                        if (moveInfo != null && moveInfo.isPush && !moveInfo.pushedMarbles.isEmpty()) {
                            // Store move data for execution after animation
                            pendingMoveTarget = target;
                            pendingSelectedMarbles = new ArrayList<>(currentSelection);
                            // Use animation with push for opponent moves that push our marbles
                            binding.hexagonalBoard.animateMoveWithPush(currentSelection, moveInfo.pushedMarbles, moveInfo.direction);
                            moveMade = true;
                            break;
                        } else {
                            // Store move data for execution after animation
                            pendingMoveTarget = target;
                            pendingSelectedMarbles = new ArrayList<>(currentSelection);
                            // Use regular animation for non-push opponent moves
                            binding.hexagonalBoard.animateMove(currentSelection, target);
                            moveMade = true;
                            break;
                        }
                    }
                }
            }
        }
        
        if (!moveMade) {
            android.util.Log.e("GalleryFragment", "executeOpponentMove - FAILED to execute move!");
            // Force UI update for failed moves (no animation)
            binding.hexagonalBoard.invalidate();
            updateUI();
        }
        // Note: For successful moves, UI will be updated after animation completes in onAnimationComplete()
        
        android.util.Log.d("GalleryFragment", "executeOpponentMove - END");
    }
    
    private List<Hex> parseSelectedMarbles(String selectedMarblesStr) {
        List<Hex> marbles = new ArrayList<>();
        try {
            // Remove brackets and split by comma
            String clean = selectedMarblesStr.replace("[", "").replace("]", "").trim();
            if (clean.isEmpty()) return marbles;
            
            String[] parts = clean.split(",");
            for (String part : parts) {
                Hex hex = Hex.fromString(part.trim());
                if (hex != null) {
                    marbles.add(hex);
                }
            }
        } catch (Exception e) {
            // Return empty list on parse error
        }
        return marbles;
    }

    @Override
    public void onGameStateUpdated(JSONObject gameState) {
        // Sync game state if needed
    }

    @Override
    public void onMessageReceived(String playerId, String message) {
        // Handle chat messages if implemented
    }

    @Override
    public void onGameEnded(String winner, JSONObject scores) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Game ended! Winner: " + winner, Toast.LENGTH_LONG).show();
                updateUI();
            });
        }
    }

    @Override
    public void onRematchRequested(String playerId) {
        // Handle rematch requests
    }

    @Override
    public void onGameRestarted() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                resetGame();
                Toast.makeText(getContext(), "Game restarted!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Game error: " + error, Toast.LENGTH_LONG).show();
            });
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // Update theme and debug buttons visibility when returning from settings
        if (binding != null && binding.hexagonalBoard != null) {
            GameSettings settings = GameSettings.getInstance(requireContext());
            binding.hexagonalBoard.setTheme(settings.getTheme());
            updateDebugButtonsVisibility();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up AI resources
        if (ai != null) {
            ai.shutdown();
        }
        
        // Clean up GameClient listener
        if (gameClient != null) {
            gameClient.setEventListener(null);
        }
        
        binding = null;
    }
}