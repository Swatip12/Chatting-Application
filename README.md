# Real-time Chat Application

A full-stack real-time chatting application built with Spring Boot backend, Angular frontend, and MySQL database.

## Project Structure

```
├── backend/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/chatapp/
│   │   │   │   ├── config/         # Configuration classes
│   │   │   │   ├── controller/     # REST and WebSocket controllers
│   │   │   │   ├── dto/           # Data Transfer Objects
│   │   │   │   ├── entity/        # JPA entities
│   │   │   │   ├── repository/    # Data repositories
│   │   │   │   └── service/       # Business logic services
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
├── frontend/                # Angular frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/        # Angular components
│   │   │   ├── services/          # Angular services
│   │   │   ├── models/            # TypeScript interfaces
│   │   │   └── guards/            # Route guards
│   │   ├── assets/
│   │   └── styles.css
│   ├── angular.json
│   ├── package.json
│   └── tsconfig.json
└── README.md
```

## Technologies Used

### Backend
- Spring Boot 2.7.14
- Spring WebSocket with STOMP
- Spring Data JPA
- MySQL 8.0+
- Lombok

### Frontend
- Angular 15+
- Bootstrap 5
- SockJS client
- STOMP.js
- RxJS

## Prerequisites

- Java 11 or higher
- Node.js 16 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

## Getting Started

### Database Setup
1. Install MySQL and create a database named `chatapp_db`
2. Update database credentials in `backend/src/main/resources/application.properties`

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install dependencies and run the application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
3. The backend will start on `http://localhost:8080`

### Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm start
   ```
4. The frontend will start on `http://localhost:4200`

## Features

- User registration and authentication
- Real-time messaging with WebSocket
- Online/offline user status
- Message history persistence
- Group chat functionality
- Responsive UI design

## API Endpoints

### Authentication
- `POST /api/register` - User registration
- `POST /api/login` - User login
- `POST /api/logout` - User logout

### Users
- `GET /api/users` - Get all users
- `GET /api/users/online` - Get online users
- `PUT /api/users/status` - Update user status

### Messages
- `GET /api/messages/{userId}` - Get message history
- `GET /api/messages/group/{groupId}` - Get group message history

### WebSocket Endpoints
- `/ws/chat` - WebSocket connection endpoint
- `/app/chat.sendMessage` - Send private message
- `/app/chat.sendGroupMessage` - Send group message
- `/app/chat.addUser` - User joins chat
- `/app/chat.removeUser` - User leaves chat