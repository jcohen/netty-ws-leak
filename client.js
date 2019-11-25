const WebSocket = require('ws');
const argv = require('yargs').argv

const host = argv.host || "localhost";
const port = argv.port || "8080";

const connections = argv.connections || 1;
const connectDelayMillis = argv.connectDelayMillis || 5;

function addConnection(i) {
    console.log(`Adding connection: ${i}.`);

    const ws = new WebSocket(`ws://${host}:${port}`);

    ws.on('connecting', function connecting() {
        console.debug("Opening connection %d...", i);
    });

    ws.on('open', function open() {
        console.info("Connection %d established!", i);
    });

    ws.on('message', function incoming(data) {
        console.info(`Received message on conn ${i}: ${data}`);
    });

    ws.on('error', function error(data) {
        console.error(`Error on connection ${i}: error: ${data}.`);
    });

    ws.on('close', function closed(data) {
        console.warn(`Disconnect on connection ${i}: ${data}`);
    });

    const next = i + 1;
    if (next < connections) {
        setTimeout(() => {
            addConnection(next);
        }, connectDelayMillis);
    }
}

addConnection(0);
