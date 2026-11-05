#!/bin/sh
# Installs the wallpi command: builds the server (Maven) and the launcher (zb),
# copies both with the frontend into WALLPI_HOME and links `wallpi` into BIN_DIR.
set -e

WALLPI_HOME=${WALLPI_HOME:-$HOME/.local/share/wallpi}
BIN_DIR=${BIN_DIR:-$HOME/bin}
ZB=${ZB:-$(command -v zb.sh || echo "$HOME/bin/zb.sh")}

cd "$(dirname "$0")"

(cd wallpi && mvn -q -B package -DskipTests)
(cd cli && "$ZB")

if [ -x "$BIN_DIR/wallpi" ]; then
    "$BIN_DIR/wallpi" stop || true
fi

mkdir -p "$WALLPI_HOME" "$BIN_DIR"
rm -rf "$WALLPI_HOME/server" "$WALLPI_HOME/app"
cp -R wallpi/target/quarkus-app "$WALLPI_HOME/server"
cp -R app/src "$WALLPI_HOME/app"
cp cli/zbo/app.jar "$WALLPI_HOME/wallpi.jar"

cat > "$BIN_DIR/wallpi" <<EOF
#!/bin/sh
export WALLPI_HOME=\${WALLPI_HOME:-$WALLPI_HOME}
exec java -jar "\$WALLPI_HOME/wallpi.jar" "\$@"
EOF
chmod +x "$BIN_DIR/wallpi"

echo "installed wallpi into $WALLPI_HOME, run: wallpi"
