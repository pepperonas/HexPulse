package io.celox.hexpulse.network;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Game client for online multiplayer functionality
 */
public class GameClient {
    private static final String TAG = "GameClient";
    private static final String BASE_URL = "https://mrx3k1.de";
    private static final String API_PATH = "/api/hexpulse";
    
    private static volatile GameClient instance;
    private Socket socket;
    private OkHttpClient httpClient;
    private Gson gson;
    private GameEventListener eventListener;
    
    // Player info
    private String currentRoomCode;
    private String playerId;
    private String playerColor;
    private boolean isHost;
    
    public interface GameEventListener {
        void onConnected();
        void onDisconnected();
        void onRoomJoined(String roomCode, String playerColor);
        void onPlayerJoined(String playerId, String playerColor);
        void onPlayerDisconnected(String playerId);
        void onGameStarted();
        void onMoveMade(String playerId, JSONObject move);
        void onGameStateUpdated(JSONObject gameState);
        void onMessageReceived(String playerId, String message);
        void onGameEnded(String winner, JSONObject scores);
        void onRematchRequested(String playerId);
        void onGameRestarted();
        void onError(String error);
    }
    
    private GameClient() {
        Log.d(TAG, "GameClient constructor called - creating new instance: " + this.hashCode());
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        gson = new Gson();
    }
    
    public static GameClient getInstance() {
        if (instance == null) {
            synchronized (GameClient.class) {
                if (instance == null) {
                    Log.d(TAG, "Creating new GameClient instance");
                    instance = new GameClient();
                } else {
                    Log.d(TAG, "Using existing GameClient instance from synchronized block");
                }
            }
        } else {
            Log.d(TAG, "Using existing GameClient instance: " + instance.hashCode());
        }
        Log.d(TAG, "getInstance() returning instance: " + instance.hashCode());
        return instance;
    }
    
    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Create a new game room
     */
    public void createRoom(Callback callback) {
        String url = BASE_URL + API_PATH + "/rooms";
        Log.d(TAG, "=== CREATE ROOM START ===");
        Log.d(TAG, "Creating room with URL: " + url);
        Log.d(TAG, "Current connection state: " + (socket != null ? "socket exists" : "no socket") + 
                   ", connected: " + isConnected());
        
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create("{}", MediaType.parse("application/json")))
            .build();
            
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Create room request failed", e);
                callback.onFailure(call, e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Create room response code: " + response.code());
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Create room response body: " + responseBody);
                    
                    // Recreate response for callback since body can only be read once
                    Response newResponse = response.newBuilder()
                        .body(ResponseBody.create(responseBody, response.body().contentType()))
                        .build();
                    callback.onResponse(call, newResponse);
                } else {
                    Log.d(TAG, "Create room response body is null");
                    callback.onResponse(call, response);
                }
            }
        });
    }
    
    /**
     * Get room information
     */
    public void getRoomInfo(String roomCode, Callback callback) {
        Request request = new Request.Builder()
            .url(BASE_URL + API_PATH + "/rooms/" + roomCode)
            .get()
            .build();
            
        httpClient.newCall(request).enqueue(callback);
    }
    
    /**
     * Connect to game server via WebSocket
     */
    public void connect() {
        try {
            Log.d(TAG, "=== CONNECT START ===");
            Log.d(TAG, "connect() - CURRENT STATE: roomCode=" + currentRoomCode + ", playerId=" + playerId + ", isHost=" + isHost);
            
            // Store current room state before reconnecting - CRITICAL for preserving state
            String savedRoomCode = currentRoomCode;
            String savedPlayerId = playerId;
            String savedPlayerColor = playerColor;
            boolean savedIsHost = isHost;
            
            Log.d(TAG, "connect() - SAVED STATE: roomCode=" + savedRoomCode + ", playerId=" + savedPlayerId + ", isHost=" + savedIsHost);
            
            // Disconnect existing socket if present (but don't clear state variables)
            if (socket != null) {
                Log.d(TAG, "connect() - Disconnecting existing socket before reconnecting");
                socket.disconnect();
                socket.off();
                socket = null; // Explicitly set to null for cleanup
            } else {
                Log.d(TAG, "connect() - No existing socket to disconnect");
            }
            
            Log.d(TAG, "connect() - Creating new socket connection to " + BASE_URL);
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket", "polling"};
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1000;
            options.timeout = 20000; // 20 second timeout
            options.path = "/socket.io/"; // Use default path until nginx is configured
            
            socket = IO.socket(URI.create(BASE_URL), options);
            Log.d(TAG, "connect() - Socket created: " + (socket != null));
            
            // CRITICAL: Restore room state after creating new socket
            // This ensures room info survives socket reconnections
            if (savedRoomCode != null && savedPlayerId != null) {
                currentRoomCode = savedRoomCode;
                playerId = savedPlayerId;
                playerColor = savedPlayerColor;
                isHost = savedIsHost;
                Log.d(TAG, "connect() - RESTORED STATE: roomCode=" + currentRoomCode + ", playerId=" + playerId + ", isHost=" + isHost);
            } else {
                Log.d(TAG, "connect() - NO STATE TO RESTORE: savedRoomCode=" + savedRoomCode + ", savedPlayerId=" + savedPlayerId);
            }
            
            setupSocketListeners();
            socket.connect();
            Log.d(TAG, "connect() - Socket connection initiated");
            Log.d(TAG, "=== CONNECT END ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Socket connection error", e);
            if (eventListener != null) {
                eventListener.onError("Connection failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Join a game room
     */
    public void joinRoom(String roomCode, String playerId, boolean isHost) {
        Log.d(TAG, "=== JOIN ROOM START ===");
        Log.d(TAG, "joinRoom - socket: " + (socket != null) + ", connected: " + (socket != null && socket.connected()));
        
        if (socket == null || !socket.connected()) {
            Log.e(TAG, "joinRoom - FAILED: Not connected to server");
            if (eventListener != null) {
                eventListener.onError("Not connected to server");
            }
            return;
        }
        
        // Store room info
        this.currentRoomCode = roomCode;
        this.playerId = playerId;
        this.isHost = isHost;
        
        Log.d(TAG, "joinRoom called - storing roomCode: " + roomCode + ", playerId: " + playerId + ", isHost: " + isHost);
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", roomCode);
            data.put("playerId", playerId);
            data.put("isHost", isHost);
            
            Log.d(TAG, "joinRoom - EMITTING join-room event with data: " + data.toString());
            socket.emit("join-room", data);
            Log.d(TAG, "joinRoom - join-room event emitted successfully");
        } catch (JSONException e) {
            Log.e(TAG, "Error joining room", e);
        }
        Log.d(TAG, "=== JOIN ROOM END ===");
    }
    
    /**
     * Make a move
     */
    public void makeMove(JSONObject move) {
        Log.d(TAG, "=== MAKE MOVE START ===");
        Log.d(TAG, "makeMove - INSTANCE: " + this.hashCode());
        Log.d(TAG, "makeMove - INPUT: move=" + move);
        Log.d(TAG, "makeMove - SOCKET STATE: socket=" + (socket != null) + ", connected=" + (socket != null && socket.connected()));
        Log.d(TAG, "makeMove - ROOM STATE: roomCode=" + currentRoomCode + ", playerId=" + playerId + ", isHost=" + isHost);
            
        if (socket == null) {
            Log.e(TAG, "makeMove - CRITICAL: socket is null, attempting to reconnect");
            connect();
            
            // Wait a bit for connection
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            Log.d(TAG, "makeMove - AFTER RECONNECT: socket=" + (socket != null) + ", roomCode=" + currentRoomCode + ", playerId=" + playerId);
            
            if (socket == null) {
                Log.e(TAG, "makeMove - FAILED: socket still null after reconnect attempt");
                return;
            }
        }
        
        if (!socket.connected()) {
            Log.w(TAG, "makeMove - Socket not connected, attempting to reconnect...");
            socket.connect();
            // Wait a bit and retry
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            Log.d(TAG, "makeMove - AFTER SOCKET RECONNECT: connected=" + socket.connected());
            
            if (!socket.connected()) {
                Log.e(TAG, "makeMove - FAILED: socket connection failed after retry");
                return;
            }
        }
        
        // Check if we have valid room state - if missing, this indicates a critical bug
        if (currentRoomCode == null || playerId == null) {
            Log.e(TAG, "makeMove - CRITICAL: Missing room state! roomCode=" + currentRoomCode + ", playerId=" + playerId);
            Log.e(TAG, "makeMove - This indicates room state was lost during socket operations");
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            data.put("playerId", playerId);
            data.put("move", move);
            
            Log.d(TAG, "makeMove - EMITTING: " + data);
            socket.emit("make-move", data);
            Log.d(TAG, "makeMove - SUCCESS: move emitted successfully");
        } catch (JSONException e) {
            Log.e(TAG, "makeMove - ERROR: JSON creation failed", e);
        }
        Log.d(TAG, "=== MAKE MOVE END ===");
    }
    
    /**
     * Sync game state
     */
    public void syncGameState(JSONObject gameState) {
        if (socket == null || !socket.connected()) {
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            data.put("gameState", gameState);
            
            socket.emit("sync-game-state", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error syncing game state", e);
        }
    }
    
    /**
     * Send chat message
     */
    public void sendMessage(String message) {
        if (socket == null || !socket.connected()) {
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            data.put("playerId", playerId);
            data.put("message", message);
            
            socket.emit("send-message", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending message", e);
        }
    }
    
    /**
     * End game
     */
    public void endGame(String winner, JSONObject scores) {
        if (socket == null || !socket.connected()) {
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            data.put("winner", winner);
            data.put("scores", scores);
            
            socket.emit("game-over", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error ending game", e);
        }
    }
    
    /**
     * Request rematch
     */
    public void requestRematch() {
        if (socket == null || !socket.connected()) {
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            data.put("playerId", playerId);
            
            socket.emit("request-rematch", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error requesting rematch", e);
        }
    }
    
    /**
     * Accept rematch
     */
    public void acceptRematch() {
        if (socket == null || !socket.connected()) {
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            
            socket.emit("accept-rematch", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error accepting rematch", e);
        }
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
        // Note: Don't clear room info unless explicitly resetting
        // This allows reconnection with same room/player data
    }
    
    /**
     * Reset all connection and room state
     */
    public void reset() {
        Log.d(TAG, "=== RESET CALLED ===");
        Log.d(TAG, "reset() - BEFORE: roomCode=" + currentRoomCode + ", playerId=" + playerId + ", isHost=" + isHost);
        Log.d(TAG, "reset() - Stack trace:", new Exception("Reset called from"));
        disconnect();
        currentRoomCode = null;
        playerId = null;
        playerColor = null;
        isHost = false;
        Log.d(TAG, "reset() - AFTER: roomCode=" + currentRoomCode + ", playerId=" + playerId + ", isHost=" + isHost);
        Log.d(TAG, "=== RESET END ===");
    }
    
    /**
     * Setup socket event listeners
     */
    private void setupSocketListeners() {
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "=== SOCKET CONNECTED ===");
                Log.d(TAG, "Connected to server successfully");
                Log.d(TAG, "Socket ID: " + socket.id());
                if (eventListener != null) {
                    eventListener.onConnected();
                }
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "=== SOCKET DISCONNECTED ===");
                Log.d(TAG, "Disconnected from server");
                if (eventListener != null) {
                    eventListener.onDisconnected();
                }
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "=== SOCKET CONNECTION ERROR ===");
                if (args.length > 0) {
                    Log.e(TAG, "Connection error: " + args[0]);
                }
                if (eventListener != null) {
                    eventListener.onError("Connection failed: " + (args.length > 0 ? args[0] : "Unknown error"));
                }
            }
        });
        
        socket.on("room-joined", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject room = data.getJSONObject("room");
                    playerColor = data.getString("yourColor");
                    
                    if (eventListener != null) {
                        eventListener.onRoomJoined(currentRoomCode, playerColor);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing room-joined", e);
                }
            }
        });
        
        socket.on("player-joined", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject player = data.getJSONObject("player");
                    String playerId = player.getString("id");
                    String playerColor = player.getString("color");
                    
                    if (eventListener != null) {
                        eventListener.onPlayerJoined(playerId, playerColor);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing player-joined", e);
                }
            }
        });
        
        socket.on("game-started", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (eventListener != null) {
                    eventListener.onGameStarted();
                }
            }
        });
        
        socket.on("move-made", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "=== MOVE-MADE EVENT RECEIVED ===");
                Log.d(TAG, "move-made - args length: " + args.length);
                if (args.length > 0) {
                    Log.d(TAG, "move-made - args[0]: " + args[0]);
                }
                try {
                    JSONObject data = (JSONObject) args[0];
                    String playerId = data.getString("playerId");
                    JSONObject move = data.getJSONObject("move");
                    
                    Log.d(TAG, "move-made - parsed successfully - playerId: " + playerId + ", move: " + move);
                    
                    if (eventListener != null) {
                        Log.d(TAG, "move-made - calling eventListener.onMoveMade()");
                        eventListener.onMoveMade(playerId, move);
                        Log.d(TAG, "move-made - eventListener.onMoveMade() completed");
                    } else {
                        Log.w(TAG, "move-made - eventListener is null!");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing move-made", e);
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in move-made listener", e);
                }
                Log.d(TAG, "=== MOVE-MADE EVENT END ===");
            }
        });
        
        socket.on("game-state-updated", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject gameState = data.getJSONObject("gameState");
                    
                    if (eventListener != null) {
                        eventListener.onGameStateUpdated(gameState);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing game-state-updated", e);
                }
            }
        });
        
        socket.on("message-received", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String playerId = data.getString("playerId");
                    String message = data.getString("message");
                    
                    if (eventListener != null) {
                        eventListener.onMessageReceived(playerId, message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing message-received", e);
                }
            }
        });
        
        socket.on("player-disconnected", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String playerId = data.getString("playerId");
                    
                    if (eventListener != null) {
                        eventListener.onPlayerDisconnected(playerId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing player-disconnected", e);
                }
            }
        });
        
        socket.on("game-ended", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String winner = data.getString("winner");
                    JSONObject scores = data.getJSONObject("scores");
                    
                    if (eventListener != null) {
                        eventListener.onGameEnded(winner, scores);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing game-ended", e);
                }
            }
        });
        
        socket.on("rematch-requested", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String playerId = data.getString("playerId");
                    
                    if (eventListener != null) {
                        eventListener.onRematchRequested(playerId);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing rematch-requested", e);
                }
            }
        });
        
        socket.on("game-restarted", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (eventListener != null) {
                    eventListener.onGameRestarted();
                }
            }
        });
        
        socket.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String message = data.getString("message");
                    
                    if (eventListener != null) {
                        eventListener.onError(message);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing error message", e);
                }
            }
        });
    }
    
    // Setters
    public void setRoomInfo(String roomCode, String playerId, boolean isHost) {
        Log.d(TAG, "=== setRoomInfo START ===");
        Log.d(TAG, "setRoomInfo - INSTANCE: " + this.hashCode());
        Log.d(TAG, "setRoomInfo - BEFORE: currentRoomCode=" + this.currentRoomCode + ", playerId=" + this.playerId + ", isHost=" + this.isHost);
        Log.d(TAG, "setRoomInfo - SETTING: roomCode=" + roomCode + ", playerId=" + playerId + ", isHost=" + isHost);
        this.currentRoomCode = roomCode;
        this.playerId = playerId;
        this.isHost = isHost;
        Log.d(TAG, "setRoomInfo - AFTER: currentRoomCode=" + this.currentRoomCode + ", playerId=" + this.playerId + ", isHost=" + this.isHost);
        Log.d(TAG, "=== setRoomInfo END ===");
    }
    
    // Getters with debugging
    public String getCurrentRoomCode() { 
        Log.d(TAG, "getCurrentRoomCode() called - returning: " + currentRoomCode + " (instance: " + this.hashCode() + ")");
        return currentRoomCode; 
    }
    public String getPlayerId() { 
        Log.d(TAG, "getPlayerId() called - returning: " + playerId + " (instance: " + this.hashCode() + ")");
        return playerId; 
    }
    public String getPlayerColor() { return playerColor; }
    public boolean isHost() { return isHost; }
    public boolean isConnected() { return socket != null && socket.connected(); }
}