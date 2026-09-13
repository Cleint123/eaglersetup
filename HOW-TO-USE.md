# How To Use EaglerXServer Setup

## Requirements

For building the standalone server, use Java 17 or Java 21.

For 24/7 startup, the setup script installs Node.js, npm, and PM2 when they are missing. Installing packages may require root access or passwordless `sudo`.

## Start the Setup Menu

From the project directory, run:

```sh
./setup.sh
```

The same script is also available at:

```sh
./eaglerweb/setup.sh
```

## Menu Options

### 1. Connect EaglerWeb to an Existing Proxy

Use this when you already have a Velocity, BungeeCord, or Bukkit server.

The script asks for the existing proxy directory and the Eagler port. It then installs or builds the Eagler plugins and creates the required configuration files.

Put website files in:

```text
plugins/eaglerweb/web/
```

Restart the existing proxy after setup.

### 2. Create a Localweb Proxy with Velocity

This creates a folder named:

```text
./eagler connection/
```

The script asks for:

```text
Eagler web/proxy port [8081]
Backend server name [lobby]
Backend server IP [127.0.0.1]
Backend port [25565]
```

You must place a real `velocity.jar` in the generated folder before starting this mode.

### 3. Create a Standalone EaglerXServer

This creates the same folder without requiring Velocity or BungeeCord:

```text
./eagler connection/
├── EaglerXServer-Standalone.jar
├── velocity.toml
├── ecosystem.config.js
└── README.txt
```

The standalone JAR is found in this order:

1. A locally built JAR in the Gradle build directory
2. `EaglerXServer-Standalone.jar` beside the setup script
3. `EaglerXServer-Standalone.jar` in the current directory
4. A published GitHub release asset

The generated `velocity.toml` contains the Eagler listener and backend servers:

```toml
bind = "0.0.0.0:8081"
try = ["lobby"]

[servers]
lobby = "127.0.0.1:25565"

[listener]
inject_address = "0.0.0.0:8081"
dual_stack = false
```

Add more backend servers under `[servers]`:

```toml
[servers]
lobby = "127.0.0.1:25565"
survival = "127.0.0.1:25566"
try = ["lobby"]
```

### 4. Edit Server

This opens the generated TOML configuration in your `$EDITOR`. If `$EDITOR` is not set, the script uses `vi`.

### 5. Exit

Closes the setup menu.

## Starting the Server

When setup finishes, it asks:

```text
Would you like to start the server now? [y/N]:
```

Answer `y` to start with PM2 and save the process.

You can also start the standalone server manually:

```sh
cd "eagler connection"
java -jar EaglerXServer-Standalone.jar velocity.toml
```

Or use PM2:

```sh
cd "eagler connection"
pm2 start ecosystem.config.js
pm2 save
```

Check the process:

```sh
pm2 status
pm2 logs eagler-connection
```

To make PM2 start after a reboot:

```sh
pm2 startup
```

Run the command PM2 prints, then run:

```sh
pm2 save
```

## Connecting

Without TLS, use:

```text
ws://YOUR_DOMAIN:8081
```

With TLS enabled and certificates configured, use:

```text
wss://YOUR_DOMAIN
```

Opening the standalone listener in a browser shows `Eagler Connected!` and the address to join with.

For DNS, TLS certificates, and firewall setup, see [DOMAIN-SETUP.md](DOMAIN-SETUP.md).

## Useful PM2 Commands

```sh
pm2 restart eagler-connection
pm2 stop eagler-connection
pm2 delete eagler-connection
pm2 logs eagler-connection
pm2 save
```
