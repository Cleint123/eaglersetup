# Domain Setup

This guide configures a domain such as `play.example.com` for the standalone EaglerXServer.

## 1. Point DNS to the Server

Create an `A` record at your DNS provider:

```text
Name: play
Type: A
Value: YOUR_SERVER_IP
```

The domain will be:

```text
play.example.com
```

For IPv6, add an `AAAA` record as well.

## 2. Allow the Port

For the default port:

```sh
sudo ufw allow 8081/tcp
```

If using the standard secure WebSocket port, allow port `443` instead:

```sh
sudo ufw allow 443/tcp
```

## 3. Get a TLS Certificate

Install Certbot, then request a certificate for the domain:

```sh
sudo certbot certonly --standalone -d play.example.com
```

The certificate files are normally created at:

```text
/etc/letsencrypt/live/play.example.com/fullchain.pem
/etc/letsencrypt/live/play.example.com/privkey.pem
```

Copy them into the standalone server folder and use the expected private-key filename:

```sh
cp /etc/letsencrypt/live/play.example.com/fullchain.pem "eagler connection/fullchain.pem"
cp /etc/letsencrypt/live/play.example.com/privkey.pem "eagler connection/privatekey.pem"
```

Keep the private key readable only by the account that runs the server:

```sh
chmod 600 "eagler connection/privatekey.pem"
```

## 4. Enable WSS

In `velocity.toml`, use the standalone listener section:

```toml
[listener]
inject_address = "0.0.0.0:443"
dual_stack = false

[listener.tls_config]
enable_tls = true
require_tls = true
tls_public_chain_file = "fullchain.pem"
tls_private_key_file = "privatekey.pem"
```

The certificate paths are relative to the standalone server folder.

## 5. Configure the Backend

Keep backend servers in the familiar TOML section:

```toml
[servers]
lobby = "127.0.0.1:25565"
survival = "10.0.0.20:25566"
try = ["lobby"]
```

The Eagler address is then:

```text
wss://play.example.com:443
```

When TLS is disabled, use:

```text
ws://play.example.com:8081
```

## 6. Start with PM2

From the generated folder:

```sh
cd "eagler connection"
pm install --global pm2
pm2 start ecosystem.config.js
pm2 save
pm2 startup
```

Run the command printed by `pm2 startup`, then run `pm2 save` again.

Check the server with:

```sh
pm2 status
pm2 logs eagler-connection
```

Opening the public address in a browser displays the standalone connection status page when the listener is reachable.

## Notes

- Port `443` may require root privileges unless a reverse proxy or port-forwarding rule is used.
- DNS changes can take time to propagate.
- Renewed Let's Encrypt certificates must be copied or linked into the server folder before restarting the server.
- If using Cloudflare, enable WebSockets and use a DNS record that points to the server.
