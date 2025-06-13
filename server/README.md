# HexPulse Server

Node.js Backend für HexPulse Online Multiplayer

## Installation auf Ubuntu 24.04 mit PM2

1. **Dateien hochladen** nach `/var/www/html/games/api/hexpulse/`:
   - `package.json`
   - `server.js`

2. **Abhängigkeiten installieren**:
   ```bash
   cd /var/www/html/games/api/hexpulse
   npm install --production
   ```

3. **PM2 Installation** (falls noch nicht installiert):
   ```bash
   npm install -g pm2
   ```

4. **Server mit PM2 starten**:
   ```bash
   cd /var/www/html/games/api/hexpulse
   pm2 start server.js --name "hexpulse"
   ```

5. **PM2 Autostart konfigurieren**:
   ```bash
   # Process speichern
   pm2 save
   
   # Autostart aktivieren
   pm2 startup
   # Den angezeigten sudo-Befehl ausführen
   ```

6. **Nginx konfigurieren** - Füge folgendes zu deiner Nginx-Config hinzu:
   ```nginx
   # HexPulse API
   location /api/hexpulse/ {
       proxy_pass http://localhost:5051/;
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection 'upgrade';
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_cache_bypass $http_upgrade;
       
       # Timeouts
       proxy_connect_timeout 60s;
       proxy_send_timeout 60s;
       proxy_read_timeout 60s;
       
       # Debug-Logs
       access_log /var/log/nginx/hexpulse-api-access.log;
       error_log /var/log/nginx/hexpulse-api-error.log;
   }

   # HexPulse Socket.IO WebSocket (für Multiplayer)
   location /hexpulse-socket.io/ {
       proxy_pass http://localhost:5051/socket.io/;
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection "upgrade";
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_buffering off;
       proxy_set_header X-NginX-Proxy true;
       proxy_redirect off;
       proxy_cache_bypass $http_upgrade;
       
       # Timeouts
       proxy_connect_timeout 60s;
       proxy_send_timeout 60s;
       proxy_read_timeout 60s;
   }
   ```

7. **Nginx neu laden**:
   ```bash
   sudo nginx -t
   sudo nginx -s reload
   ```

## API Endpoints

- `GET /api/hexpulse/health` - Server Status
- `POST /api/hexpulse/rooms` - Neuen Raum erstellen
- `GET /api/hexpulse/rooms/:roomCode` - Raum-Informationen

## WebSocket Events

**Client → Server:**
- `join-room` - Raum beitreten
- `make-move` - Spielzug machen
- `sync-game-state` - Spielzustand synchronisieren
- `send-message` - Chat-Nachricht senden
- `game-over` - Spiel beenden
- `request-rematch` - Revanche anfordern
- `accept-rematch` - Revanche akzeptieren

**Server → Client:**
- `room-joined` - Raum erfolgreich beigetreten
- `player-joined` - Anderer Spieler beigetreten
- `game-started` - Spiel gestartet
- `move-made` - Zug ausgeführt
- `game-state-updated` - Spielzustand aktualisiert
- `message-received` - Chat-Nachricht empfangen
- `player-disconnected` - Spieler getrennt
- `game-ended` - Spiel beendet
- `rematch-requested` - Revanche angefragt
- `game-restarted` - Spiel neu gestartet

## PM2 Service-Verwaltung

```bash
# Status aller Prozesse anzeigen
pm2 status

# HexPulse Server Status
pm2 show hexpulse

# Server-Logs anzeigen
pm2 logs hexpulse

# Server neu starten
pm2 restart hexpulse

# Server stoppen
pm2 stop hexpulse

# Server aus PM2 entfernen
pm2 delete hexpulse

# Monitoring Dashboard
pm2 monit

# Alle Prozesse neu starten
pm2 restart all

# PM2 Status speichern
pm2 save
```

## Logs

- **PM2 Logs**: `pm2 logs hexpulse`
- **Nginx Access Log**: `/var/log/nginx/hexpulse-api-access.log`
- **Nginx Error Log**: `/var/log/nginx/hexpulse-api-error.log`

## Server testen

```bash
# API-Endpunkt testen
curl https://mrx3k1.de/api/hexpulse/health

# WebSocket-Verbindung testen (mit wscat)
npm install -g wscat
wscat -c wss://mrx3k1.de/hexpulse-socket.io/
```

## Troubleshooting

```bash
# PM2 Prozesse neu laden
pm2 reload all

# PM2 komplett neu starten
pm2 kill
pm2 resurrect

# Nginx Konfiguration testen
sudo nginx -t

# Port 5051 prüfen
sudo netstat -tlnp | grep 5051
```