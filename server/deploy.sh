#!/bin/bash

# HexPulse Server Deployment Script

echo "🚀 Deploying HexPulse Server..."

# Variables
SERVER_DIR="/var/www/html/games/api/hexpulse"
LOG_DIR="/var/log/hexpulse"
SERVICE_NAME="hexpulse"

# Create directories
echo "📁 Creating directories..."
sudo mkdir -p $SERVER_DIR
sudo mkdir -p $LOG_DIR

# Copy server files
echo "📋 Copying server files..."
sudo cp package.json $SERVER_DIR/
sudo cp server.js $SERVER_DIR/

# Install dependencies
echo "📦 Installing dependencies..."
cd $SERVER_DIR
sudo npm install --production

# Set permissions
echo "🔐 Setting permissions..."
sudo chown -R www-data:www-data $SERVER_DIR
sudo chown -R www-data:www-data $LOG_DIR

# Install systemd service
echo "⚙️ Installing systemd service..."
sudo cp hexpulse.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME

# Start or restart service
echo "🔄 Starting service..."
sudo systemctl restart $SERVICE_NAME

# Check status
echo "✅ Checking service status..."
sudo systemctl status $SERVICE_NAME --no-pager

echo "🎉 Deployment complete!"
echo ""
echo "📝 Next steps:"
echo "1. Update nginx configuration with the provided nginx.conf"
echo "2. Reload nginx: sudo nginx -s reload"
echo "3. Test the API: curl https://mrx3k1.de/api/hexpulse/health"