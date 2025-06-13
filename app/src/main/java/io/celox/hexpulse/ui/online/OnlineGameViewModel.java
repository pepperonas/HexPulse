package io.celox.hexpulse.ui.online;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.celox.hexpulse.network.GameClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class OnlineGameViewModel extends AndroidViewModel implements GameClient.GameEventListener {
    private static final String TAG = "OnlineGameViewModel";

    // Connection state
    private MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>(ConnectionStatus.DISCONNECTED);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> statusMessage = new MutableLiveData<>();

    // Room state
    private MutableLiveData<String> currentRoomCode = new MutableLiveData<>();
    private MutableLiveData<List<Player>> players = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<Boolean> isHost = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> canStartGame = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isInRoom = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Game state
    private MutableLiveData<Boolean> gameStarted = new MutableLiveData<>(false);

    private GameClient gameClient;
    private String playerId;

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public static class Player {
        public String id;
        public String color;
        public boolean isHost;
        public String displayName;

        public Player(String id, String color, boolean isHost) {
            this.id = id;
            this.color = color;
            this.isHost = isHost;
            this.displayName = isHost ? "Host" : "Player";
        }
    }

    public OnlineGameViewModel(@NonNull Application application) {
        super(application);
        gameClient = GameClient.getInstance();
        gameClient.setEventListener(this);
        playerId = "Player_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // LiveData getters
    public LiveData<ConnectionStatus> getConnectionStatus() { return connectionStatus; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<String> getCurrentRoomCode() { return currentRoomCode; }
    public LiveData<List<Player>> getPlayers() { return players; }
    public LiveData<Boolean> getIsHost() { return isHost; }
    public LiveData<Boolean> getCanStartGame() { return canStartGame; }
    public LiveData<Boolean> getIsInRoom() { return isInRoom; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getGameStarted() { return gameStarted; }

    public String getPlayerId() { return playerId; }

    // Connection methods
    public void connect() {
        if (connectionStatus.getValue() == ConnectionStatus.CONNECTED) {
            return;
        }

        connectionStatus.setValue(ConnectionStatus.CONNECTING);
        statusMessage.setValue("Connecting to server...");
        gameClient.connect();
    }

    public void disconnect() {
        gameClient.disconnect();
        connectionStatus.setValue(ConnectionStatus.DISCONNECTED);
        resetRoomState();
    }

    // Room management methods
    public void createRoom() {
        if (connectionStatus.getValue() != ConnectionStatus.CONNECTED) {
            errorMessage.setValue("Not connected to server");
            return;
        }

        isLoading.setValue(true);
        statusMessage.setValue("Creating room...");

        gameClient.createRoom(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to create room", e);
                isLoading.postValue(false);
                errorMessage.postValue("Failed to create room: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String roomCode = jsonResponse.getString("roomCode");
                        
                        // Join the created room as host
                        joinRoom(roomCode, true);
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing create room response", e);
                        errorMessage.postValue("Error creating room: Invalid response");
                    }
                } else {
                    errorMessage.postValue("Failed to create room: Server error");
                }
            }
        });
    }

    public void joinRoom(String roomCode) {
        joinRoom(roomCode, false);
    }

    private void joinRoom(String roomCode, boolean asHost) {
        if (connectionStatus.getValue() != ConnectionStatus.CONNECTED) {
            errorMessage.setValue("Not connected to server");
            return;
        }

        if (roomCode == null || roomCode.trim().isEmpty()) {
            errorMessage.setValue("Please enter a room code");
            return;
        }

        isLoading.setValue(true);
        statusMessage.setValue("Joining room...");

        gameClient.joinRoom(roomCode.toUpperCase().trim(), playerId, asHost);
    }

    public void leaveRoom() {
        gameClient.disconnect();
        resetRoomState();
        
        // Reconnect to server
        connect();
    }

    public void startGame() {
        if (!Boolean.TRUE.equals(isHost.getValue())) {
            errorMessage.setValue("Only the host can start the game");
            return;
        }

        List<Player> currentPlayers = players.getValue();
        if (currentPlayers == null || currentPlayers.size() < 2) {
            errorMessage.setValue("Need at least 2 players to start the game");
            return;
        }

        // Start the game through GameClient
        // This would typically involve sending a start-game event
        statusMessage.setValue("Starting game...");
    }

    // GameClient.GameEventListener implementation
    @Override
    public void onConnected() {
        connectionStatus.postValue(ConnectionStatus.CONNECTED);
        statusMessage.postValue("Connected to server");
    }

    @Override
    public void onDisconnected() {
        connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
        statusMessage.postValue("Disconnected from server");
        resetRoomState();
    }

    @Override
    public void onRoomJoined(String roomCode, String playerColor) {
        isLoading.postValue(false);
        currentRoomCode.postValue(roomCode);
        isInRoom.postValue(true);
        statusMessage.postValue("Successfully joined room " + roomCode);
        
        // Add self to players list
        List<Player> currentPlayers = new ArrayList<>();
        boolean hostStatus = Boolean.TRUE.equals(isHost.getValue());
        currentPlayers.add(new Player(playerId, playerColor, hostStatus));
        players.postValue(currentPlayers);
        
        updateGameStartAvailability();
    }

    @Override
    public void onPlayerJoined(String playerId, String playerColor) {
        statusMessage.postValue("Player joined the room");
        
        List<Player> currentPlayers = players.getValue();
        if (currentPlayers == null) {
            currentPlayers = new ArrayList<>();
        } else {
            currentPlayers = new ArrayList<>(currentPlayers);
        }
        
        currentPlayers.add(new Player(playerId, playerColor, false));
        players.postValue(currentPlayers);
        
        updateGameStartAvailability();
    }

    @Override
    public void onPlayerDisconnected(String playerId) {
        statusMessage.postValue("Player left the room");
        
        List<Player> currentPlayers = players.getValue();
        if (currentPlayers != null) {
            currentPlayers = new ArrayList<>(currentPlayers);
            currentPlayers.removeIf(player -> player.id.equals(playerId));
            players.postValue(currentPlayers);
        }
        
        updateGameStartAvailability();
    }

    @Override
    public void onGameStarted() {
        gameStarted.postValue(true);
        statusMessage.postValue("Game started!");
    }

    @Override
    public void onMoveMade(String playerId, JSONObject move) {
        // Handle move made by other player
        // This would be forwarded to the game logic
    }

    @Override
    public void onGameStateUpdated(JSONObject gameState) {
        // Handle game state updates
        // This would be forwarded to the game logic
    }

    @Override
    public void onMessageReceived(String playerId, String message) {
        // Handle chat messages if implemented
    }

    @Override
    public void onGameEnded(String winner, JSONObject scores) {
        gameStarted.postValue(false);
        statusMessage.postValue("Game ended. Winner: " + winner);
    }

    @Override
    public void onRematchRequested(String playerId) {
        statusMessage.postValue("Rematch requested by " + playerId);
    }

    @Override
    public void onGameRestarted() {
        gameStarted.postValue(true);
        statusMessage.postValue("Game restarted!");
    }

    @Override
    public void onError(String error) {
        isLoading.postValue(false);
        errorMessage.postValue(error);
        Log.e(TAG, "GameClient error: " + error);
    }

    // Helper methods
    private void resetRoomState() {
        currentRoomCode.postValue(null);
        players.postValue(new ArrayList<>());
        isHost.postValue(false);
        canStartGame.postValue(false);
        isInRoom.postValue(false);
        gameStarted.postValue(false);
        isLoading.postValue(false);
    }

    private void updateGameStartAvailability() {
        List<Player> currentPlayers = players.getValue();
        boolean hostStatus = Boolean.TRUE.equals(isHost.getValue());
        boolean hasEnoughPlayers = currentPlayers != null && currentPlayers.size() >= 2;
        
        canStartGame.postValue(hostStatus && hasEnoughPlayers);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        gameClient.setEventListener(null);
        gameClient.disconnect();
    }
}