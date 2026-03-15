var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    document.getElementById('status').className = 'badge bg-success';
    document.getElementById('status').textContent = 'Connected';

    stompClient.subscribe('/topic/messages', function(message) {
        var msg = JSON.parse(message.body);
        var tbody = document.getElementById('log-body');
        var row = tbody.insertRow(0);
        row.insertCell(0).textContent = msg.sentAt || new Date().toISOString();
        row.insertCell(1).textContent = msg.chatId;
        row.insertCell(2).textContent = msg.senderName || 'Unknown';
        row.insertCell(3).textContent = msg.text;
    });
}, function(error) {
    document.getElementById('status').className = 'badge bg-danger';
    document.getElementById('status').textContent = 'Error: ' + error;
});
