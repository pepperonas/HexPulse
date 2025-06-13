package io.celox.hexpulse.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.*;

/**
 * Game client for online multiplayer functionality
 */
public class GameClient {
    private static final String TAG = "GameClient";
    private static final String BASE_URL = "https://mrx3k1.de";
    private static final String API_PATH = "/api/hexpulse";
    
    private static GameClient instance;
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
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        gson = new Gson();
    }
    
    public static GameClient getInstance() {
        if (instance == null) {
            instance = new GameClient();
        }
        return instance;
    }
    
    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Create a new game room
     */
    public void createRoom(Callback callback) {
        Request request = new Request.Builder()
            .url(BASE_URL + API_PATH + "/rooms")
            .post(RequestBody.create("{}", MediaType.parse("application/json")))
            .build();
            
        httpClient.newCall(request).enqueue(callback);
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
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket"};
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1000;
            
            socket = IO.socket(URI.create(BASE_URL), options);
            setupSocketListeners();
            socket.connect();
            
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
        if (socket == null || !socket.connected()) {
            if (eventListener != null) {
                eventListener.onError("Not connected to server");
            }
            return;
        }
        
        this.currentRoomCode = roomCode;
        this.playerId = playerId;
        this.isHost = isHost;
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", roomCode);
            data.put("playerId", playerId);
            data.put("isHost", isHost);
            
            socket.emit("join-room", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error joining room", e);
        }
    }
    
    /**
     * Make a move
     */
    public void makeMove(JSONObject move) {
        if (socket == null || !socket.connected()) {
            return;
        }
        
        try {
            JSONObject data = new JSONObject();
            data.put("roomCode", currentRoomCode);
            data.put("playerId", playerId);
            data.put("move", move);
            
            socket.emit("make-move", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error making move", e);
        }
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
        currentRoomCode = null;
        playerId = null;
        playerColor = null;
        isHost = false;
    }
    
    /**
     * Setup socket event listeners
     */
    private void setupSocketListeners() {
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Connected to server");
                if (eventListener != null) {
                    eventListener.onConnected();
                }
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Disconnected from server");
                if (eventListener != null) {
                    eventListener.onDisconnected();
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
                try {
                    JSONObject data = (JSONObject) args[0];
                    String playerId = data.getString("playerId");
                    JSONObject move = data.getJSONObject("move");
                    
                    if (eventListener != null) {
                        eventListener.onMoveMade(playerId, move);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing move-made", e);
                }
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
    
    // Getters
    public String getCurrentRoomCode() { return currentRoomCode; }
    public String getPlayerId() { return playerId; }
    public String getPlayerColor() { return playerColor; }
    public boolean isHost() { return isHost; }
    public boolean isConnected() { return socket != null && socket.connected(); }
}