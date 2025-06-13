package io.celox.hexpulse.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import io.celox.hexpulse.R;
import io.celox.hexpulse.databinding.FragmentGalleryBinding;
import io.celox.hexpulse.game.*;
import io.celox.hexpulse.network.GameClient;
import io.celox.hexpulse.ui.views.HexagonalBoardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class GalleryFragment extends Fragment implements HexagonalBoardView.BoardTouchListener, GameClient.GameEventListener {

    private FragmentGalleryBinding binding;
    private AbaloneGame game;
    private AbaloneAI ai;
    private String gameMode = "PVP"; // Default to Player vs Player
    private boolean isAiThinking = false;
    private Hex pendingMoveTarget = null; // Target position for pending move
    
    // Online game variables
    private GameClient gameClient;
    private String roomCode;
    private String playerId;
    private boolean isHost;
    private Player myPlayerColor;
    private boolean isOnlineGame = false;

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
                
                // Initialize GameClient
                gameClient = GameClient.getInstance();
                gameClient.setEventListener(this);
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
        
        // Setup AI if needed
        if ("AI".equals(gameMode)) {
            ai = new AbaloneAI(AIDifficulty.MEDIUM); // Default to medium difficulty
        } else {
            ai = null;
        }
        
        // Set up board
        binding.hexagonalBoard.setGame(game);
        binding.hexagonalBoard.setBoardTouchListener(this);
        binding.hexagonalBoard.setTheme(Theme.CLASSIC); // Default theme
    }

    private void setupUI() {
        // Set up button listeners
        binding.btnResetGame.setOnClickListener(v -> resetGame());
        binding.btnClearSelection.setOnClickListener(v -> clearSelection());
    }

    private void updateUI() {
        if (game == null) return;

        // Update current player text
        String currentPlayerText = getString(R.string.current_player, 
            game.getCurrentPlayer() == Player.BLACK ? 
            getString(R.string.black_player) : getString(R.string.white_player));
        binding.textCurrentPlayer.setText(currentPlayerText);

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
                            
                            // Store target and animate AI move
                            pendingMoveTarget = move.target;
                            binding.hexagonalBoard.animateMove(move.selectedMarbles, move.target);
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
        
        // In online mode, only allow moves if it's the player's turn
        if (isOnlineGame && myPlayerColor != null && game.getCurrentPlayer() != myPlayerColor) {
            return;
        }

        Player playerAtPosition = game.getPlayerAt(position);

        // If clicking on current player's marble, select/deselect it
        if (playerAtPosition == game.getCurrentPlayer()) {
            game.selectMarble(position);
            updateUI();
        }
        // If clicking on valid move position, make the move with animation
        else if (game.getValidMoves().contains(position)) {
            List<Hex> selectedMarbles = game.getSelectedMarbles();
            if (!selectedMarbles.isEmpty()) {
                // Store target for later execution
                pendingMoveTarget = position;
                // Start animation
                binding.hexagonalBoard.animateMove(selectedMarbles, position);
                // Move will be executed when animation completes
            }
        }
        // If clicking elsewhere, clear selection
        else {
            clearSelection();
        }
    }
    
    @Override
    public void onAnimationComplete() {
        // Execute the actual move after animation
        if (game != null && pendingMoveTarget != null) {
            if (game.makeMove(pendingMoveTarget)) {
                // Send move to server if in online mode
                if (isOnlineGame && gameClient != null) {
                    sendMoveToServer(pendingMoveTarget);
                }
                pendingMoveTarget = null;
                updateUI();
            } else {
                Toast.makeText(getContext(), "Move execution failed", Toast.LENGTH_SHORT).show();
                pendingMoveTarget = null;
            }
        }
    }
    
    private void sendMoveToServer(Hex target) {
        try {
            JSONObject moveData = new JSONObject();
            moveData.put("target", target.toString());
            moveData.put("player", myPlayerColor.toString());
            
            // Add selected marbles information
            List<Hex> selectedMarbles = game.getSelectedMarbles();
            moveData.put("selectedMarbles", selectedMarbles.toString());
            
            gameClient.makeMove(moveData);
        } catch (JSONException e) {
            Toast.makeText(getContext(), "Failed to send move", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetGame() {
        if (game != null) {
            game.resetGame();
            updateUI();
        }
    }

    private void clearSelection() {
        if (game != null) {
            game.clearSelection();
            updateUI();
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
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
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
        // Only process moves from other players
        if (!this.playerId.equals(playerId)) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        // Parse and execute opponent's move
                        executeOpponentMove(move);
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Error processing opponent's move", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
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

    private void executeOpponentMove(JSONObject moveData) throws JSONException {
        // This would need to be implemented based on the move format
        // For now, just show that opponent made a move
        Toast.makeText(getContext(), "Opponent made a move", Toast.LENGTH_SHORT).show();
        updateUI();
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