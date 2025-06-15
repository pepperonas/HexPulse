const express = require('express');
const http = require('http');
const socketIO = require('socket.io');
const cors = require('cors');
const { nanoid } = require('nanoid');

const app = express();
const server = http.createServer(app);
const io = socketIO(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// Middleware
app.use(cors());
app.use(express.json());

// Store active game rooms
const gameRooms = new Map();

// Room class to manage game state
class GameRoom {
    constructor(roomCode, hostId) {
        this.roomCode = roomCode;
        this.hostId = hostId;
        this.guestId = null;
        this.players = new Map();
        this.gameState = {
            currentPlayer: 'BLACK',
            scores: { BLACK: 0, WHITE: 0 },
            board: this.initializeBoard(),
            selectedMarbles: [],
            gameStarted: false,
            winner: null
        };
        this.createdAt = Date.now();
        this.lastActivity = Date.now();
    }

    initializeBoard() {
        // Initialize empty board - client will handle the actual setup
        return {};
    }

    addPlayer(playerId, socketId, isHost = false) {
        const player = {
            id: playerId,
            socketId: socketId,
            color: isHost ? 'BLACK' : 'WHITE',
            connected: true
        };
        this.players.set(playerId, player);
        this.lastActivity = Date.now();
        return player;
    }

    removePlayer(playerId) {
        const player = this.players.get(playerId);
        if (player) {
            player.connected = false;
        }
    }

    getPlayerBySocketId(socketId) {
        for (const [id, player] of this.players) {
            if (player.socketId === socketId) {
                return player;
            }
        }
        return null;
    }

    isFull() {
        return this.players.size >= 2;
    }

    isEmpty() {
        let connectedCount = 0;
        for (const player of this.players.values()) {
            if (player.connected) connectedCount++;
        }
        return connectedCount === 0;
    }

    updateGameState(update) {
        Object.assign(this.gameState, update);
        this.lastActivity = Date.now();
    }

    toJSON() {
        return {
            roomCode: this.roomCode,
            players: Array.from(this.players.values()),
            gameState: this.gameState,
            createdAt: this.createdAt
        };
    }
}

// Generate unique room code (6 characters)
function generateRoomCode() {
    let code;
    do {
        code = nanoid(6).toUpperCase();
    } while (gameRooms.has(code));
    return code;
}

// Clean up inactive rooms (older than 2 hours)
setInterval(() => {
    const now = Date.now();
    const twoHours = 2 * 60 * 60 * 1000;
    
    for (const [code, room] of gameRooms) {
        if (room.isEmpty() || (now - room.lastActivity > twoHours)) {
            gameRooms.delete(code);
            console.log(`Room ${code} cleaned up`);
        }
    }
}, 60000); // Check every minute

// REST API endpoints
app.get('/api/hexpulse/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        rooms: gameRooms.size,
        timestamp: new Date().toISOString()
    });
});

// Create new room
app.post('/api/hexpulse/rooms', (req, res) => {
    const roomCode = generateRoomCode();
    const playerId = nanoid();
    const room = new GameRoom(roomCode, playerId);
    
    gameRooms.set(roomCode, room);
    
    res.json({
        roomCode,
        playerId,
        shareLink: `https://mrx3k1.de/hexpulse/join/${roomCode}`
    });
});

// Get room info
app.get('/api/hexpulse/rooms/:roomCode', (req, res) => {
    const { roomCode } = req.params;
    const room = gameRooms.get(roomCode.toUpperCase());
    
    if (!room) {
        return res.status(404).json({ error: 'Room not found' });
    }
    
    res.json({
        roomCode: room.roomCode,
        playerCount: room.players.size,
        isFull: room.isFull(),
        gameStarted: room.gameState.gameStarted
    });
});

// Socket.IO connection handling
io.on('connection', (socket) => {
    console.log('New connection:', socket.id);
    
    // Join room
    socket.on('join-room', (data) => {
        const { roomCode, playerId, isHost } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) {
            socket.emit('error', { message: 'Room not found' });
            return;
        }
        
        if (room.isFull() && !room.players.has(playerId)) {
            socket.emit('error', { message: 'Room is full' });
            return;
        }
        
        // Join socket room
        socket.join(roomCode);
        
        // Add or update player
        let player = room.players.get(playerId);
        if (!player) {
            player = room.addPlayer(playerId, socket.id, isHost);
        } else {
            player.socketId = socket.id;
            player.connected = true;
        }
        
        // Send room state to joining player
        socket.emit('room-joined', {
            room: room.toJSON(),
            yourColor: player.color,
            playerId: player.id
        });
        
        // Notify other players
        socket.to(roomCode).emit('player-joined', {
            player: player,
            room: room.toJSON()
        });
        
        // Start game if room is full
        if (room.isFull() && !room.gameState.gameStarted) {
            room.gameState.gameStarted = true;
            io.to(roomCode).emit('game-started', {
                room: room.toJSON()
            });
        }
    });
    
    // Handle move
    socket.on('make-move', (data) => {
        const { roomCode, playerId, move } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) {
            socket.emit('error', { message: 'Room not found' });
            return;
        }
        
        const player = room.players.get(playerId);
        if (!player) {
            socket.emit('error', { message: 'Player not found' });
            return;
        }
        
        // Validate it's player's turn
        const playerColor = player.color;
        if (room.gameState.currentPlayer !== playerColor) {
            socket.emit('error', { message: 'Not your turn' });
            return;
        }
        
        // Update game state
        room.updateGameState({
            currentPlayer: playerColor === 'BLACK' ? 'WHITE' : 'BLACK',
            lastMove: move
        });
        
        // Broadcast move to all players in room
        io.to(roomCode).emit('move-made', {
            playerId,
            move,
            gameState: room.gameState
        });
    });
    
    // Handle game state sync
    socket.on('sync-game-state', (data) => {
        const { roomCode, gameState } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) {
            socket.emit('error', { message: 'Room not found' });
            return;
        }
        
        room.updateGameState(gameState);
        
        // Broadcast updated state to all players
        socket.to(roomCode).emit('game-state-updated', {
            gameState: room.gameState
        });
    });
    
    // Handle chat messages
    socket.on('send-message', (data) => {
        const { roomCode, playerId, message } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) return;
        
        const player = room.players.get(playerId);
        if (!player) return;
        
        // Broadcast message to all players in room
        io.to(roomCode).emit('message-received', {
            playerId,
            playerColor: player.color,
            message,
            timestamp: Date.now()
        });
    });
    
    // Handle disconnect
    socket.on('disconnect', () => {
        console.log('Disconnected:', socket.id);
        
        // Find player and room
        for (const [roomCode, room] of gameRooms) {
            const player = room.getPlayerBySocketId(socket.id);
            if (player) {
                room.removePlayer(player.id);
                
                // Notify other players
                socket.to(roomCode).emit('player-disconnected', {
                    playerId: player.id,
                    room: room.toJSON()
                });
                
                // Clean up empty room
                if (room.isEmpty()) {
                    gameRooms.delete(roomCode);
                    console.log(`Room ${roomCode} deleted - all players left`);
                }
                
                break;
            }
        }
    });
    
    // Handle game over
    socket.on('game-over', (data) => {
        const { roomCode, winner, scores } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) return;
        
        room.updateGameState({
            winner,
            scores,
            gameOver: true
        });
        
        // Broadcast game over to all players
        io.to(roomCode).emit('game-ended', {
            winner,
            scores,
            room: room.toJSON()
        });
    });
    
    // Handle rematch request
    socket.on('request-rematch', (data) => {
        const { roomCode, playerId } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) return;
        
        // Notify other player
        socket.to(roomCode).emit('rematch-requested', {
            playerId
        });
    });
    
    // Handle rematch acceptance
    socket.on('accept-rematch', (data) => {
        const { roomCode } = data;
        const room = gameRooms.get(roomCode);
        
        if (!room) return;
        
        // Reset game state
        room.gameState = {
            currentPlayer: 'BLACK',
            scores: { BLACK: 0, WHITE: 0 },
            board: room.initializeBoard(),
            selectedMarbles: [],
            gameStarted: true,
            winner: null
        };
        
        // Notify all players
        io.to(roomCode).emit('game-restarted', {
            room: room.toJSON()
        });
    });
});

// Start server
const PORT = process.env.PORT || 5051;
server.listen(PORT, () => {
    console.log(`HexPulse server running on port ${PORT}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('SIGTERM signal received: closing HTTP server');
    server.close(() => {
        console.log('HTTP server closed');
    });
});