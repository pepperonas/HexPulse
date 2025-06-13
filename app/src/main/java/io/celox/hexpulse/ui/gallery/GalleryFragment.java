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
import io.celox.hexpulse.ui.views.HexagonalBoardView;

import java.util.Map;

public class GalleryFragment extends Fragment implements HexagonalBoardView.BoardTouchListener {

    private FragmentGalleryBinding binding;
    private AbaloneGame game;
    private AbaloneAI ai;
    private String gameMode = "PVP"; // Default to Player vs Player
    private boolean isAiThinking = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Get game mode from arguments
        if (getArguments() != null) {
            gameMode = getArguments().getString("game_mode", "PVP");
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
                            // Execute AI move
                            game.clearSelection();
                            for (Hex marble : move.selectedMarbles) {
                                game.selectMarble(marble);
                            }
                            
                            if (game.makeMove(move.target)) {
                                updateUI();
                            } else {
                                Toast.makeText(getContext(), "AI move failed", Toast.LENGTH_SHORT).show();
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
        if (game == null || isAiThinking) return;

        // In AI mode, only allow human player (BLACK) to move
        if (ai != null && game.getCurrentPlayer() == Player.WHITE) {
            return;
        }

        Player playerAtPosition = game.getPlayerAt(position);

        // If clicking on current player's marble, select/deselect it
        if (playerAtPosition == game.getCurrentPlayer()) {
            game.selectMarble(position);
            updateUI();
        }
        // If clicking on valid move position, make the move
        else if (game.getValidMoves().contains(position)) {
            if (game.makeMove(position)) {
                updateUI();
            } else {
                Toast.makeText(getContext(), "Invalid move", Toast.LENGTH_SHORT).show();
            }
        }
        // If clicking elsewhere, clear selection
        else {
            clearSelection();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up AI resources
        if (ai != null) {
            ai.shutdown();
        }
        
        binding = null;
    }
}