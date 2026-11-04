#!/bin/sh
# Stops every wallpi instance listening on port 9447.
# Other programs on that port are reported, never killed.

PORT=9447
GRACE_SECONDS=10

pids=$(lsof -t -nP -iTCP:$PORT -sTCP:LISTEN)
if [ -z "$pids" ]; then
    echo "wallpi is not running on port $PORT"
    exit 0
fi

stopped=0
for pid in $pids; do
    command=$(ps -o command= -p "$pid")
    case "$command" in
        *quarkus-run.jar*|*quarkus:dev*|*wallpi*)
            echo "stopping wallpi (pid $pid)"
            kill "$pid"
            stopped=1
            ;;
        *)
            echo "skipping pid $pid on port $PORT, not wallpi: $command"
            ;;
    esac
done
[ $stopped -eq 0 ] && exit 1

waited=0
while lsof -t -nP -iTCP:$PORT -sTCP:LISTEN > /dev/null && [ $waited -lt $GRACE_SECONDS ]; do
    sleep 1
    waited=$((waited + 1))
done

remaining=$(lsof -t -nP -iTCP:$PORT -sTCP:LISTEN)
for pid in $remaining; do
    case "$(ps -o command= -p "$pid")" in
        *quarkus-run.jar*|*quarkus:dev*|*wallpi*)
            echo "wallpi (pid $pid) ignored the stop request, killing it"
            kill -9 "$pid"
            ;;
    esac
done
