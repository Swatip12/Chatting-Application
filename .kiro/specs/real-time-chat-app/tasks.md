# Implementation Plan

- [x] 1. Set up project structure and dependencies





  - Create Spring Boot backend project with required dependencies (web, websocket, data-jpa, mysql-connector-j, lombok)
  - Create Angular frontend project with Bootstrap, SockJS, and STOMP dependencies
  - Configure application.properties for MySQL database connection
  - Set up basic project directory structure for both backend and frontend
  - _Requirements: 1.1, 2.1_

- [x] 2. Implement backend data models and database configuration





  - [x] 2.1 Create User entity with JPA annotations


    - Implement User entity with id, username, password, status, timestamps
    - Add proper JPA annotations and validation constraints
    - Define UserStatus enum for online/offline states
    - _Requirements: 1.1, 1.2, 4.1, 4.2_

  - [x] 2.2 Create Message entity with relationships


    - Implement Message entity with sender, receiver, content, timestamp fields
    - Add MessageType enum for CHAT, JOIN, LEAVE message types
    - Configure JPA relationships between User and Message entities
    - Add groupId field for group chat support
    - _Requirements: 3.1, 3.3, 5.1, 6.1_

  - [x] 2.3 Create repository interfaces


    - Implement UserRepository with custom query methods for finding online users
    - Implement MessageRepository with methods for message history retrieval
    - Add query methods for group message history and user conversations
    - _Requirements: 2.1, 4.1, 5.1, 6.1_

- [x] 3. Implement authentication and user management




  - [x] 3.1 Create user registration functionality


    - Implement UserService with user registration logic
    - Add username uniqueness validation and password encryption
    - Create AuthController with POST /api/register endpoint
    - Add proper error handling for duplicate usernames and validation errors
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 3.2 Create user login functionality


    - Implement user authentication logic in UserService
    - Create POST /api/login endpoint in AuthController
    - Add session management and user status update to online
    - Implement proper error handling for invalid credentials
    - _Requirements: 2.1, 2.2, 2.4_

  - [x] 3.3 Create user management endpoints


    - Implement GET /api/users endpoint to retrieve all users
    - Create GET /api/users/online endpoint for online users only
    - Add PUT /api/users/status endpoint for status updates
    - Implement logout functionality with status update to offline
    - _Requirements: 4.1, 4.2, 8.1, 8.2_

- [x] 4. Implement WebSocket configuration and real-time messaging





  - [x] 4.1 Configure WebSocket with STOMP


    - Create WebSocketConfig class with message broker configuration
    - Set up STOMP endpoints and application destination prefixes
    - Configure SockJS fallback support for WebSocket connections
    - Add CORS configuration for cross-origin WebSocket connections
    - _Requirements: 3.1, 3.4_

  - [x] 4.2 Create WebSocket message controllers


    - Implement ChatController with @MessageMapping annotations
    - Create /app/chat.sendMessage endpoint for private messaging
    - Add /app/chat.sendGroupMessage endpoint for group messaging
    - Implement /app/chat.addUser and /app/chat.removeUser for user status
    - _Requirements: 3.1, 3.4, 4.2, 4.3, 6.2_

  - [x] 4.3 Implement message broadcasting logic


    - Create MessageService for message persistence and broadcasting
    - Implement real-time message delivery to specific users and groups
    - Add message history storage to MySQL database
    - Configure message routing for private and group conversations
    - _Requirements: 3.1, 3.2, 3.5, 6.2_

- [x] 5. Create message history and retrieval functionality





  - [x] 5.1 Implement message history endpoints



    - Create GET /api/messages/{userId} endpoint for private message history
    - Add GET /api/messages/group/{groupId} endpoint for group message history
    - Implement pagination for message history (limit to 50 recent messages)
    - Add timestamp-based sorting for chronological message display
    - _Requirements: 5.1, 5.2, 5.4, 5.5_

  - [x] 5.2 Create message persistence service


    - Implement message saving logic in MessageService
    - Add automatic timestamp generation for all messages
    - Create methods for retrieving conversation history between users
    - Implement group message history retrieval with proper filtering
    - _Requirements: 3.2, 5.1, 5.3, 6.5_

- [x] 6. Implement Angular frontend structure and services





  - [x] 6.1 Create Angular project structure and routing


    - Set up Angular project with routing configuration
    - Create components for login, register, and chat-room
    - Configure Bootstrap 5 for responsive UI styling
    - Set up routing guards for authentication protection
    - _Requirements: 7.1, 7.5_



  - [x] 6.2 Create authentication service and components

    - Implement AuthService with HTTP client for REST API calls
    - Create LoginComponent with form validation and error handling
    - Create RegisterComponent with username availability check
    - Add authentication state management and local storage handling
    - _Requirements: 1.5, 2.5, 8.3, 8.4_

  - [x] 6.3 Create WebSocket chat service


    - Implement ChatService using SockJS and STOMP client
    - Add connection management with automatic reconnection logic
    - Create message subscription handlers for real-time updates
    - Implement message sending methods for private and group chats
    - _Requirements: 3.1, 3.4, 4.4, 6.2_

- [x] 7. Implement chat room UI and real-time features





  - [x] 7.1 Create main chat room component


    - Implement ChatRoomComponent with user list and message area layout
    - Add responsive design with left sidebar for users and right area for messages
    - Create message input form with send functionality
    - Implement scrollable message area with proper styling
    - _Requirements: 7.2, 7.3, 7.4_

  - [x] 7.2 Create user list component


    - Implement UserListComponent to display online/offline users
    - Add real-time user status updates through WebSocket subscriptions
    - Create visual indicators for online/offline status
    - Add click handlers to start private conversations with users
    - _Requirements: 4.1, 4.4, 4.5_

  - [x] 7.3 Create message display components


    - Implement MessageComponent for individual message rendering
    - Add chat bubble styling with different colors for sender/receiver
    - Create timestamp formatting and display for each message
    - Implement message type handling for JOIN/LEAVE notifications
    - _Requirements: 7.2, 5.2, 5.3_

- [-] 8. Implement group chat functionality


  - [x] 8.1 Add group chat backend support


    - Extend MessageService to handle group message routing
    - Create group management logic for adding/removing participants
    - Implement group message broadcasting to all members
    - Add group message history persistence and retrieval
    - _Requirements: 6.1, 6.2, 6.4, 6.5_

  - [-] 8.2 Create group chat UI components

    - Add group creation and management interface
    - Implement group member list display in chat interface
    - Create group message sending and receiving functionality
    - Add notifications for group member join/leave events
    - _Requirements: 6.3, 6.4_

- [ ] 9. Add error handling and user experience improvements
  - [ ] 9.1 Implement comprehensive error handling
    - Create global exception handler for backend API errors
    - Add HTTP error interceptor for frontend error handling
    - Implement WebSocket connection error handling with retry logic
    - Add user-friendly error messages and notifications
    - _Requirements: 1.3, 2.4_

  - [ ] 9.2 Add user experience enhancements
    - Implement loading indicators for API calls and message sending
    - Add message delivery status indicators
    - Create notification system for new messages and user status changes
    - Implement proper logout functionality with session cleanup
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 10. Testing and validation
  - [ ] 10.1 Create backend unit tests
    - Write unit tests for UserService registration and authentication logic
    - Create tests for MessageService message handling and persistence
    - Add repository layer tests for data access operations
    - Test WebSocket message controller endpoints
    - _Requirements: All backend requirements_

  - [ ] 10.2 Create frontend unit tests
    - Write component tests for LoginComponent and RegisterComponent
    - Create service tests for AuthService and ChatService
    - Add tests for message display and user list components
    - Test WebSocket connection and message handling logic
    - _Requirements: All frontend requirements_

  - [ ] 10.3 Create integration tests
    - Write end-to-end tests for user registration and login flow
    - Create tests for real-time message sending and receiving
    - Add tests for group chat functionality and user status updates
    - Test WebSocket connection handling and error scenarios
    - _Requirements: All system integration requirements_