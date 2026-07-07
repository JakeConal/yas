#!/bin/bash
set -e

echo "Setting up local access through WSL2 Nginx..."

# Ensure Nginx is installed in WSL
if ! command -v nginx &> /dev/null; then
    echo "Installing Nginx in WSL..."
    sudo apt-get update
    sudo apt-get install -y nginx
fi

MINIKUBE_IP=$(minikube ip)
echo "Minikube IP is: $MINIKUBE_IP"

# Create Nginx config to forward requests to Minikube Ingress
NGINX_CONF_PATH="/etc/nginx/sites-available/yas-local.conf"

sudo bash -c "cat > $NGINX_CONF_PATH" <<EOF
server {
    listen 80;
    server_name ~^(.*)\.yas\.local\.com\$;

    location / {
        proxy_pass http://$MINIKUBE_IP;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

# Enable the site and restart Nginx
sudo ln -sf /etc/nginx/sites-available/yas-local.conf /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo systemctl restart nginx || sudo service nginx restart

echo "Nginx configured successfully."
