const express = require('express');
const cors = require('cors');
const http = require('http');
const sockjs = require('sockjs');

const app = express();
const server = http.createServer(app);

// Create SockJS server
const sockjsServer = sockjs.createServer({
  sockjs_url: 'http://cdn.jsdelivr.net/sockjs/1.0.1/sockjs.min.js',
  log: function(severity, message) {
    console.log('SockJS:', severity, message);
  }
});

// Middleware
app.use(cors());
app.use(express.json());

// In-memory storage
let users = [];
let messages = [];
let nextUserId = 1;
let nextMessageId = 1;

// Mock user data
const mockUsers = [
  { id: 1, username: 'alice', password: 'password123', status: 'OFFLINE' },
  { id: 2, username: 'bob', password: 'password123', status: 'OFFLINE' },
  { id: 3, username: 'charlie', password: 'password123', status: 'OFFLINE' }
];

users = [...mockUsers];
nextUserId = 4;

// API Routes
app.post('/api/register', (req, res) => {
  const { username, password } = req.body;
  
  // Check if user exists
  if (users.find(u => u.username === username)) {
    return res.status(400).json({
      success: false,
      message: 'Username already exists'
    });
  }
  
  // Create new user
  const newUser = {
    id: nextUserId++,
    username,
    password,
    status: 'OFFLINE',
    createdAt: new Date(),
    lastSeen: new Date()
  };
  
  users.push(newUser);
  
  res.json({
    success: true,
    message: 'Registration successful',
    user: {
      id: newUser.id,
      username: newUser.username,
      status: newUser.status,
      createdAt: newUser.createdAt,
      lastSeen: newUser.lastSeen
    }
  });
});

app.post('/api/login', (req, res) => {
  const { username, password } = req.body;
  
  const user = users.find(u => u.username === username && u.password === password);
  
  if (!user) {
    return res.status(401).json({
      success: false,
      message: 'Invalid username or password'
    });
  }
  
  // Update user status
  user.status = 'ONLINE';
  user.lastSeen = new Date();
  
  res.json({
    success: true,
    message: 'Login successful',
    user: {
      id: user.id,
      username: user.username,
      status: user.status,
      createdAt: user.createdAt,
      lastSeen: user.lastSeen
    }
  });
});

app.post('/api/logout', (req, res) => {
  res.json({
    success: true,
    message: 'Logout successful'
  });
});

app.get('/api/users', (req, res) => {
  const userList = users.map(u => ({
    id: u.id,
    username: u.username,
    status: u.status,
    createdAt: u.createdAt,
    lastSeen: u.lastSeen
  }));
  
  res.json(userList);
});

app.get('/api/users/online', (req, res) => {
  const onlineUsers = users
    .filter(u => u.status === 'ONLINE')
    .map(u => ({
      id: u.id,
      username: u.username,
      status: u.status,
      createdAt: u.createdAt,
      lastSeen: u.lastSeen
    }));
  
  res.json(onlineUsers);
});

app.put('/api/users/status', (req, res) => {
  res.json({ success: true });
});

app.get('/api/messages/:userId', (req, res) => {
  res.json([]);
});

app.get('/api/messages/group/:groupId', (req, res) => {
  res.json([]);
});

// Store connected clients
const clients = new Set();

// SockJS connection handling
sockjsServer.on('connection', (conn) => {
  console.log('New SockJS connection:', conn.id);
  clients.add(conn);
  
  // Send STOMP CONNECTED frame
  conn.write('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0');
  
  conn.on('data', (message) => {
    try {
      console.log('Received SockJS message:', message);
      
      // Handle STOMP commands
      if (message.startsWith('CONNECT')) {
        console.log('STOMP CONNECT received');
        conn.write('CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0');
      } else if (message.startsWith('SUBSCRIBE')) {
        console.log('STOMP SUBSCRIBE received');
        // Extract subscription ID if present
        const lines = message.split('\n');
        let receiptId = 'sub-0';
        for (const line of lines) {
          if (line.startsWith('id:')) {
            receiptId = line.substring(3);
            break;
          }
        }
        conn.write(`RECEIPT\nreceipt-id:${receiptId}\n\n\0`);
      } else if (message.startsWith('SEND')) {
        console.log('STOMP SEND received');
        // Parse and broadcast message
        try {
          const bodyStart = message.indexOf('\n\n') + 2;
          const body = message.substring(bodyStart).replace('\0', '');
          
          if (body) {
            const messageData = JSON.parse(body);
            console.log('Broadcasting message:', messageData);
            
            // Broadcast to all connected clients
            const broadcastMessage = `MESSAGE\ndestination:/topic/public\ncontent-type:application/json\nmessage-id:${Date.now()}\n\n${body}\0`;
            clients.forEach((client) => {
              if (client !== conn && client.readyState === 1) {
                try {
                  client.write(broadcastMessage);
                } catch (e) {
                  console.error('Error broadcasting to client:', e);
                }
              }
            });
          }
        } catch (e) {
          console.error('Error parsing STOMP message:', e);
        }
      }
    } catch (error) {
      console.error('Error handling SockJS message:', error);
    }
  });
  
  conn.on('close', () => {
    console.log('SockJS connection closed:', conn.id);
    clients.delete(conn);
  });
});

// Install SockJS handler
sockjsServer.installHandlers(server, {prefix: '/ws/chat'});

const PORT = 8082;
server.listen(PORT, () => {
  console.log(`Mock Chat Backend running on http://localhost:${PORT}`);
  console.log(`WebSocket server running on ws://localhost:${PORT}`);
});