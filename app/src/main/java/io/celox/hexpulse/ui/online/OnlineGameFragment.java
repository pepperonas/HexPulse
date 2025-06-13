package io.celox.hexpulse.ui.online;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.celox.hexpulse.R;
import io.celox.hexpulse.databinding.FragmentOnlineGameBinding;

public class OnlineGameFragment extends Fragment {
    private static final String TAG = "OnlineGameFragment";
    
    private FragmentOnlineGameBinding binding;
    private OnlineGameViewModel viewModel;
    private PlayersAdapter playersAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnlineGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(OnlineGameViewModel.class);
        
        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
        
        // Auto-connect when fragment is created
        viewModel.connect();
    }

    private void setupRecyclerView() {
        playersAdapter = new PlayersAdapter(requireContext());
        binding.rvPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlayers.setAdapter(playersAdapter);
    }

    private void setupClickListeners() {
        binding.btnCreateRoom.setOnClickListener(v -> viewModel.createRoom());
        
        binding.btnJoinRoom.setOnClickListener(v -> {
            String roomCode = binding.etRoomCode.getText().toString().trim();
            viewModel.joinRoom(roomCode);
        });
        
        binding.btnCopyRoomCode.setOnClickListener(v -> copyRoomCodeToClipboard());
        
        binding.btnShareRoom.setOnClickListener(v -> shareRoomLink());
        
        binding.btnStartGame.setOnClickListener(v -> {
            viewModel.startGame();
            // Navigate to game screen
            navigateToGame();
        });
        
        binding.btnLeaveRoom.setOnClickListener(v -> {
            viewModel.leaveRoom();
            showCreateJoinInterface();
        });
    }

    private void observeViewModel() {
        // Connection status
        viewModel.getConnectionStatus().observe(getViewLifecycleOwner(), status -> {
            updateConnectionStatus(status);
        });
        
        // Error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        
        // Status messages
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                binding.tvLoadingMessage.setText(message);
            }
        });
        
        // Room state
        viewModel.getIsInRoom().observe(getViewLifecycleOwner(), inRoom -> {
            if (Boolean.TRUE.equals(inRoom)) {
                showRoomInterface();
            } else {
                showCreateJoinInterface();
            }
        });
        
        // Current room code
        viewModel.getCurrentRoomCode().observe(getViewLifecycleOwner(), roomCode -> {
            if (roomCode != null) {
                binding.tvCurrentRoomCode.setText(roomCode);
            }
        });
        
        // Players list
        viewModel.getPlayers().observe(getViewLifecycleOwner(), players -> {
            playersAdapter.updatePlayers(players);
            updatePlayerStatus(players != null ? players.size() : 0);
        });
        
        // Host status
        viewModel.getIsHost().observe(getViewLifecycleOwner(), isHost -> {
            binding.btnStartGame.setVisibility(Boolean.TRUE.equals(isHost) ? View.VISIBLE : View.GONE);
        });
        
        // Can start game
        viewModel.getCanStartGame().observe(getViewLifecycleOwner(), canStart -> {
            binding.btnStartGame.setEnabled(Boolean.TRUE.equals(canStart));
        });
        
        // Loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            binding.tvLoadingMessage.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });
        
        // Game started
        viewModel.getGameStarted().observe(getViewLifecycleOwner(), gameStarted -> {
            if (Boolean.TRUE.equals(gameStarted)) {
                navigateToGame();
            }
        });
    }

    private void updateConnectionStatus(OnlineGameViewModel.ConnectionStatus status) {
        switch (status) {
            case CONNECTED:
                binding.tvConnectionStatus.setText(R.string.connected);
                binding.statusIndicator.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.green));
                enableCreateJoinButtons(true);
                break;
            case CONNECTING:
                binding.tvConnectionStatus.setText(R.string.connecting);
                binding.statusIndicator.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.orange));
                enableCreateJoinButtons(false);
                break;
            case DISCONNECTED:
            default:
                binding.tvConnectionStatus.setText(R.string.disconnected);
                binding.statusIndicator.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.red));
                enableCreateJoinButtons(false);
                break;
        }
    }

    private void enableCreateJoinButtons(boolean enabled) {
        binding.btnCreateRoom.setEnabled(enabled);
        binding.btnJoinRoom.setEnabled(enabled);
    }

    private void showCreateJoinInterface() {
        binding.layoutCreateRoom.setVisibility(View.VISIBLE);
        binding.layoutJoinRoom.setVisibility(View.VISIBLE);
        binding.layoutRoomInfo.setVisibility(View.GONE);
    }

    private void showRoomInterface() {
        binding.layoutCreateRoom.setVisibility(View.GONE);
        binding.layoutJoinRoom.setVisibility(View.GONE);
        binding.layoutRoomInfo.setVisibility(View.VISIBLE);
        
        // Clear the room code input
        binding.etRoomCode.setText("");
    }

    private void updatePlayerStatus(int playerCount) {
        if (playerCount < 2) {
            binding.tvPlayerStatus.setText(R.string.waiting_for_players);
        } else {
            if (Boolean.TRUE.equals(viewModel.getIsHost().getValue())) {
                binding.tvPlayerStatus.setText(R.string.you_are_host);
            } else {
                binding.tvPlayerStatus.setText(R.string.waiting_for_host);
            }
        }
    }

    private void copyRoomCodeToClipboard() {
        String roomCode = viewModel.getCurrentRoomCode().getValue();
        if (roomCode != null) {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Room Code", roomCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), R.string.room_code_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareRoomLink() {
        String roomCode = viewModel.getCurrentRoomCode().getValue();
        if (roomCode != null) {
            String shareText = getString(R.string.room_link_format, roomCode);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_room_link)));
        }
    }

    private void navigateToGame() {
        // Navigate to the game screen with online multiplayer mode
        Bundle args = new Bundle();
        args.putString("game_mode", "ONLINE");
        args.putString("room_code", viewModel.getCurrentRoomCode().getValue());
        args.putString("player_id", viewModel.getPlayerId());
        args.putBoolean("is_host", Boolean.TRUE.equals(viewModel.getIsHost().getValue()));
        
        Navigation.findNavController(requireView()).navigate(R.id.nav_gallery, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) {
            viewModel.disconnect();
        }
        binding = null;
    }
}