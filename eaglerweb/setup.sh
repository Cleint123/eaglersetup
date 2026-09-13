#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SERVER_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
DEFAULT_SETTINGS="$SCRIPT_DIR/src/main/resources/net/lax1dude/eaglercraft/backend/eaglerweb/base/default_settings.json"

die() {
	printf '%s\n' "Error: $*" >&2
	exit 1
}

pause() {
	printf '%s' 'Press Enter to continue...'
	read -r _
}

run_as_root() {
	if [ "$(id -u)" -eq 0 ]; then
		"$@"
	elif command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
		sudo "$@"
	else
		die 'Installing Node.js requires root access or passwordless sudo.'
	fi
}

install_node_pm2() {
	if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
		printf '%s\n' 'Node.js is not installed. Installing Node.js and npm...'
		run_as_root apt-get update
		run_as_root apt-get install -y nodejs npm
	fi
	if ! command -v pm2 >/dev/null 2>&1; then
		printf '%s\n' 'PM2 is not installed. Installing PM2...'
		run_as_root npm install --global pm2
	fi
}

ask_start_server() {
	printf '%s' 'Would you like to start the server now? [y/N]: '
	read -r START_SERVER
	case "$START_SERVER" in
		y|Y|yes|YES)
		if [ "${SERVER_MODE:-standalone}" = "localweb" ] && [ ! -f "$PROXY_DIR/velocity.jar" ]; then
			printf '%s\n' 'Cannot start localweb yet: put velocity.jar in the server folder first.'
			return
		fi
		(cd "$PROXY_DIR" && pm2 start ecosystem.config.js && pm2 save)
		printf '%s\n' 'Server started with PM2 and saved for restart.'
		;;
		*) printf '%s\n' 'Server was not started.' ;;
	esac
}

ask_proxy_dir() {
	printf 'Proxy directory [%s]: ' "$PWD"
	read -r PROXY_DIR
	PROXY_DIR=${PROXY_DIR:-$PWD}
	mkdir -p "$PROXY_DIR" || die "Could not create proxy directory: $PROXY_DIR"
	PROXY_DIR=$(CDPATH= cd -- "$PROXY_DIR" && pwd)
}

ask_port() {
	printf 'Eagler web/proxy port [8081]: '
	read -r PORT
	PORT=${PORT:-8081}
	case "$PORT" in
		*[!0-9]*|'') die 'Port must be a number between 1 and 65535.' ;;
	esac
	[ "$PORT" -ge 1 ] && [ "$PORT" -le 65535 ] || die 'Port must be between 1 and 65535.'
}

ask_backend() {
	printf 'Backend server name [lobby]: '
	read -r BACKEND_NAME
	BACKEND_NAME=${BACKEND_NAME:-lobby}
	case "$BACKEND_NAME" in
		*[!A-Za-z0-9_-]*|'') die 'Backend name may only contain letters, numbers, _ and -.' ;;
	esac
	printf 'Backend server IP [127.0.0.1]: '
	read -r BACKEND_HOST
	BACKEND_HOST=${BACKEND_HOST:-127.0.0.1}
	printf 'Backend port [25565]: '
	read -r BACKEND_PORT
	BACKEND_PORT=${BACKEND_PORT:-25565}
	case "$BACKEND_PORT" in
		*[!0-9]*|'') die 'Backend port must be a number between 1 and 65535.' ;;
	esac
	[ "$BACKEND_PORT" -ge 1 ] && [ "$BACKEND_PORT" -le 65535 ] || die 'Backend port must be between 1 and 65535.'
}

install_jars() {
	mkdir -p "$PROXY_DIR/plugins"
	if [ ! -f "$SCRIPT_DIR/build/libs/EaglerWeb.jar" ]; then
		printf '%s\n' 'EaglerWeb.jar was not built. Building EaglerWeb and EaglerXServer...'
		(cd "$SERVER_DIR" && sh gradlew :eaglerweb:shadowJar :core:shadowJar) || die 'Gradle build failed.'
	fi
	if [ -f "$SCRIPT_DIR/build/libs/EaglerWeb.jar" ]; then
		cp "$SCRIPT_DIR/build/libs/EaglerWeb.jar" "$PROXY_DIR/plugins/EaglerWeb.jar"
	else
		printf '%s\n' 'Warning: EaglerWeb.jar was not found.'
	fi
	if [ -f "$SERVER_DIR/core/build/libs/EaglerXServer.jar" ]; then
		cp "$SERVER_DIR/core/build/libs/EaglerXServer.jar" "$PROXY_DIR/plugins/EaglerXServer.jar"
	else
		printf '%s\n' 'Warning: EaglerXServer.jar was not found.'
	fi
}

write_eaglerweb_config() {
	mkdir -p "$PROXY_DIR/plugins/eaglerweb/web"
	if [ ! -f "$PROXY_DIR/plugins/eaglerweb/settings.json" ]; then
		cp "$DEFAULT_SETTINGS" "$PROXY_DIR/plugins/eaglerweb/settings.json"
	fi
}

write_listener_config() {
	LISTENER_CONFIG="$PROXY_DIR/plugins/EaglerXServer/listeners.cfg"
	mkdir -p "$(dirname -- "$LISTENER_CONFIG")"
	if [ ! -f "$LISTENER_CONFIG" ]; then
		cat > "$LISTENER_CONFIG" <<EOF
listener_list {
  listener_name = "listener0"
  inject_address = "0.0.0.0:$PORT"
  velocity_clone_listener = false
}
EOF
	elif grep -q '^[[:space:]]*inject_address[[:space:]]*=' "$LISTENER_CONFIG"; then
		sed "s|^[[:space:]]*inject_address[[:space:]]*=.*|  inject_address = \"0.0.0.0:$PORT\"|" "$LISTENER_CONFIG" > "$LISTENER_CONFIG.tmp"
		mv "$LISTENER_CONFIG.tmp" "$LISTENER_CONFIG"
	else
		printf '\nlistener_list {\n  listener_name = "listener0"\n  inject_address = "0.0.0.0:%s"\n}\n' "$PORT" >> "$LISTENER_CONFIG"
	fi
}

write_velocity_config() {
	cat > "$PROXY_DIR/velocity.toml" <<EOF
config-version = "2.7"
bind = "0.0.0.0:$PORT"
motd = "$BACKEND_NAME Eaglercraft Server"
show-max-players = 100
online-mode = false
player-info-forwarding-mode = "none"

[servers]
$BACKEND_NAME = "$BACKEND_HOST:$BACKEND_PORT"
try = ["$BACKEND_NAME"]

[forced-hosts]

[advanced]
compression-threshold = 256
compression-level = -1
login-ratelimit = 3000
connection-timeout = 5000
read-timeout = 30000
tcp-fast-open = false
show-ping-requests = false
announce-forge = false
haproxy-protocol = false
tcp-no-delay = true
failover-on-unexpected-server-disconnect = true
send-flush = false
log-command-executions = false
log-player-connections = true

[query]
enabled = false
port = 25565
map = "Eaglercraft"
show-plugins = false

[settings]
server_name = "$BACKEND_NAME Eaglercraft Server"

[listeners]
[[listeners.listener_list]]
listener_name = "listener0"
inject_address = "0.0.0.0:$PORT"
dual_stack = true
EOF
}

write_eagler_metadata() {
	cat > "$PROXY_DIR/eagler.toml" <<EOF
# Eagler connection settings. Edit this file with the Edit server menu.
server_name = "$BACKEND_NAME Eaglercraft Server"
web_port = $PORT
velocity_config = "velocity.toml"
velocity_jar = "velocity.jar"
pm2_name = "eagler-connection"

[backend_servers]
$BACKEND_NAME = "$BACKEND_HOST:$BACKEND_PORT"
EOF
}

write_pm2_config() {
	cat > "$PROXY_DIR/ecosystem.config.js" <<'EOF'
module.exports = {
  apps: [{
    name: "eagler-connection",
    cwd: __dirname,
    script: "java",
    args: "-Xms512M -Xmx2G -jar velocity.jar",
    interpreter: "none",
    autorestart: true,
    restart_delay: 3000,
    time: true
  }]
};
EOF
}

write_edit_script() {
	cat > "$PROXY_DIR/edit-server.sh" <<'EOF'
#!/bin/sh
set -eu
EDITOR=${EDITOR:-vi}
"$EDITOR" "$(dirname -- "$0")/eagler.toml" "$(dirname -- "$0")/velocity.toml" "$(dirname -- "$0")/plugins/EaglerXServer/listeners.cfg"
EOF
	chmod +x "$PROXY_DIR/edit-server.sh"
}

write_local_readme() {
	cat > "$PROXY_DIR/README.txt" <<EOF
Eagler Connection: $BACKEND_NAME Eaglercraft Server

1. Put velocity.jar in this folder.
2. Put your backend server online at $BACKEND_HOST:$BACKEND_PORT.
3. Start the 24/7 proxy with: pm2 start ecosystem.config.js
4. Save it for reboot with: pm2 save
5. Add it to startup with: pm2 startup

The public Eaglercraft address uses port $PORT.
Edit server settings with: ./edit-server.sh
EOF
}

write_standalone_pm2_config() {
	cat > "$PROXY_DIR/ecosystem.config.js" <<'EOF'
module.exports = {
  apps: [{
    name: "eagler-connection",
    cwd: __dirname,
    script: "java",
	args: "-Xms512M -Xmx2G -jar EaglerXServer-Standalone.jar velocity.toml",
    interpreter: "none",
    autorestart: true,
    restart_delay: 3000,
    time: true
  }]
};
EOF
}

setup_standalone() {
	SERVER_MODE=standalone
	PROXY_DIR="$PWD/eagler connection"
	mkdir -p "$PROXY_DIR" || die "Could not create standalone folder: $PROXY_DIR"
	PROXY_DIR=$(CDPATH= cd -- "$PROXY_DIR" && pwd)
	ask_port
	ask_backend

	STANDALONE_JAR="$SERVER_DIR/core/core-platform-standalone/build/libs/EaglerXServer-Standalone.jar"
	if [ ! -f "$STANDALONE_JAR" ] && [ -f "$SCRIPT_DIR/EaglerXServer-Standalone.jar" ]; then
		STANDALONE_JAR="$SCRIPT_DIR/EaglerXServer-Standalone.jar"
	fi
	if [ ! -f "$STANDALONE_JAR" ] && [ -f "$PWD/EaglerXServer-Standalone.jar" ]; then
		STANDALONE_JAR="$PWD/EaglerXServer-Standalone.jar"
	fi
	if [ ! -f "$STANDALONE_JAR" ] && [ -f "$SERVER_DIR/gradlew" ]; then
		printf '%s\n' 'Standalone JAR was not built. Building it now...'
		(cd "$SERVER_DIR" && sh gradlew :core:core-platform-standalone:shadowJar) || die 'Standalone Gradle build failed.'
	fi
	if [ ! -f "$STANDALONE_JAR" ]; then
		STANDALONE_JAR="$SERVER_DIR/core/core-platform-standalone/build/libs/EaglerXServer-Standalone.jar"
	fi
	if [ ! -f "$STANDALONE_JAR" ] && command -v curl >/dev/null 2>&1; then
		STANDALONE_JAR="$PROXY_DIR/EaglerXServer-Standalone.jar"
		STANDALONE_JAR_URL=${STANDALONE_JAR_URL:-https://github.com/lax1dude/eaglerxserver/releases/latest/download/EaglerXServer-Standalone.jar}
		printf '%s\n' "Downloading standalone JAR from $STANDALONE_JAR_URL..."
		curl -fL --retry 2 "$STANDALONE_JAR_URL" -o "$STANDALONE_JAR" || rm -f "$STANDALONE_JAR"
	fi
	[ -f "$STANDALONE_JAR" ] || die "Standalone JAR not found: $STANDALONE_JAR"
	if [ "$STANDALONE_JAR" != "$PROXY_DIR/EaglerXServer-Standalone.jar" ]; then
		cp "$STANDALONE_JAR" "$PROXY_DIR/EaglerXServer-Standalone.jar"
	fi
	write_velocity_config
	sed -i 's/^dual_stack = true$/dual_stack = false/' "$PROXY_DIR/velocity.toml"
	cat >> "$PROXY_DIR/velocity.toml" <<EOF

[listener]
inject_address = "0.0.0.0:$PORT"
dual_stack = false
EOF
	write_standalone_pm2_config
	install_node_pm2
	cat > "$PROXY_DIR/README.txt" <<EOF
Standalone EaglerXServer

Start directly with: java -jar EaglerXServer-Standalone.jar velocity.toml
Start 24/7 with: pm2 start ecosystem.config.js && pm2 save

The Eagler listener uses port $PORT.
The configured backend is $BACKEND_NAME at $BACKEND_HOST:$BACKEND_PORT.
Edit velocity.toml to add more backend servers.
EOF
	printf '%s\n' '' "Standalone EaglerXServer created in $PROXY_DIR."
	printf '%s\n' 'Edit velocity.toml to add backend servers, then run the README command.'
	ask_start_server
	pause
}

update_base_proxy() {
	if [ -f "$PROXY_DIR/velocity.toml" ]; then
		sed -i.bak "0,/^[[:space:]]*bind[[:space:]]*=/{s|^[[:space:]]*bind[[:space:]]*=.*|bind = \"0.0.0.0:$PORT\"|}" "$PROXY_DIR/velocity.toml"
		printf '%s\n' "Updated Velocity listener to 0.0.0.0:$PORT (backup: velocity.toml.bak)."
	elif [ -f "$PROXY_DIR/config.yml" ]; then
		sed -i.bak "0,/^[[:space:]]*host:[[:space:]]*/{s|^[[:space:]]*host:.*|  host: 0.0.0.0:$PORT|}" "$PROXY_DIR/config.yml"
		printf '%s\n' "Updated Bungee listener to 0.0.0.0:$PORT (backup: config.yml.bak)."
	else
		printf '%s\n' "No base proxy config found; set its listener to 0.0.0.0:$PORT manually."
	fi
}

setup_existing_proxy() {
	ask_proxy_dir
	ask_port
	install_jars
	write_eaglerweb_config
	write_listener_config
	update_base_proxy
	printf '%s\n' '' "EaglerWeb files are ready in $PROXY_DIR."
	printf '%s\n' 'Put website files in plugins/eaglerweb/web, then restart the existing proxy.'
	pause
}

setup_localweb() {
	SERVER_MODE=localweb
	PROXY_DIR="$PWD/eagler connection"
	mkdir -p "$PROXY_DIR" || die "Could not create localweb folder: $PROXY_DIR"
	PROXY_DIR=$(CDPATH= cd -- "$PROXY_DIR" && pwd)
	ask_port
	ask_backend
	install_jars
	write_eaglerweb_config
	write_listener_config
	write_velocity_config
	write_eagler_metadata
	write_pm2_config
	write_edit_script
	write_local_readme
	install_node_pm2
	printf '%s\n' '' "Localweb proxy created in $PROXY_DIR."
	printf '%s\n' 'Put a Velocity proxy jar at velocity.jar before starting it.'
	printf '%s\n' 'Start it 24/7 with: pm2 start ecosystem.config.js && pm2 save'
	ask_start_server
	pause
}

edit_server() {
	ask_proxy_dir
	[ -f "$PROXY_DIR/velocity.toml" ] || die "Velocity config not found: $PROXY_DIR/velocity.toml"
	EDITOR=${EDITOR:-vi}
	if [ -f "$PROXY_DIR/eagler.toml" ]; then
		"$EDITOR" "$PROXY_DIR/eagler.toml" "$PROXY_DIR/velocity.toml" "$PROXY_DIR/plugins/EaglerXServer/listeners.cfg"
	else
		"$EDITOR" "$PROXY_DIR/velocity.toml"
	fi
}

while :; do
	printf '%s\n' '' 'Eagler connection setup' '1) Connect EaglerWeb to an existing proxy' '2) Create a localweb proxy with Velocity' '3) Create a standalone EaglerXServer' '4) Edit server' '5) Exit'
	printf '%s' 'Choose an option: '
	read -r OPTION
	case "$OPTION" in
		1) setup_existing_proxy ;;
		2) setup_localweb ;;
		3) setup_standalone ;;
		4) edit_server ;;
		5) exit 0 ;;
		*) printf '%s\n' 'Please choose 1, 2, 3, 4, or 5.' ;;
	esac
done