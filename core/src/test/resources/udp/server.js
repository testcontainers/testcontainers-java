var PORT = process.env.SERVER_PORT;
var MESSAGE = process.env.SERVER_MESSAGE;

var dgram = require('dgram');
var server = dgram.createSocket('udp4');

server.on('listening', function () {
    var address = server.address();
    console.log('UDP Server listening on ' + address.address + ':' + address.port);
});

server.on('message', function (message, remote) {
    server.send(Buffer.from(MESSAGE), remote.port, remote.address, function (error) {
        if (error) {
            client.close();
        } else {
            console.log('Data sent.');
        }
    });
});

server.bind(PORT, '0.0.0.0');
