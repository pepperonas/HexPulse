# PM2 Setup für HexPulse Server

## PM2 Installation (falls noch nicht installiert)
```bash
sudo npm install -g pm2
```

## Server mit PM2 starten
```bash
cd /var/www/html/games/api/hexpulse
pm2 start server.js --name "hexpulse"
```

## Process speichern und Autostart aktivieren
```bash
# Aktuellen PM2 Zustand speichern
pm2 save

# PM2 Autostart beim Systemstart aktivieren
pm2 startup

# Den angezeigten Befehl ausführen (wird von pm2 startup generiert)
# Beispiel: sudo env PATH=$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u username --hp /home/username
```

## Nützliche PM2 Befehle
```bash
# Status aller Prozesse anzeigen
pm2 status

# Logs anzeigen
pm2 logs hexpulse

# Server neu starten
pm2 restart hexpulse

# Server stoppen
pm2 stop hexpulse

# Server aus PM2 entfernen
pm2 delete hexpulse

# Monitoring Dashboard
pm2 monit
```

## Server-Konfiguration mit Ecosystem-Datei (Optional)
Erstelle `ecosystem.config.js`:
```javascript
module.exports = {
  apps: [{
    name: 'hexpulse',
    script: 'server.js',
    cwd: '/var/www/html/games/api/hexpulse',
    instances: 1,
    exec_mode: 'fork',
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 5051
    },
    error_file: '/var/log/hexpulse/error.log',
    out_file: '/var/log/hexpulse/output.log',
    log_file: '/var/log/hexpulse/combined.log',
    time: true
  }]
};
```

Mit Ecosystem-Datei starten:
```bash
pm2 start ecosystem.config.js
```