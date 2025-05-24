# Chatting Application Backend

A real-time chat application backend built with Spring Boot, WebSocket, and JWT authentication.

## Features

- **Real-time messaging** using WebSocket connections
- **JWT-based authentication** for secure user sessions
- **File upload support** for images, videos, documents, and audio
- **Chat room management** with user permissions
- **Message status tracking** (sent, delivered, read)
- **RESTful API** for client integration
- **Comprehensive testing** with unit and integration tests

## Technology Stack

- **Java 17**
- **Spring Boot 3.x**
- **Spring Security** with JWT
- **Spring WebSocket** for real-time communication
- **Spring Data JPA** for database operations
- **H2 Database** (development) / MySQL (production)
- **Maven** for dependency management
- **JUnit 5** for testing

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Git

### Installation

1. Clone the repository:
```bash
git clone https://github.com/hamza-damra/Chatting-Application-Back-End.git
cd Chatting-Application-Back-End
```

2. Build the project:
```bash
./mvnw clean install
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### Configuration

The application supports multiple profiles:

- **Development**: `application-dev.yml`
- **Production**: `application-prod.yml`

To run with a specific profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Documentation

### Authentication Endpoints

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/refresh` - Refresh JWT token

### Chat Endpoints

- `GET /api/chatrooms` - Get user's chat rooms
- `POST /api/chatrooms` - Create a new chat room
- `GET /api/chatrooms/{id}/messages` - Get chat room messages
- `POST /api/messages` - Send a message

### File Upload Endpoints

- `POST /api/files/upload` - Upload files
- `GET /api/files/{fileId}` - Download files

### WebSocket Endpoints

- `/ws/chat` - WebSocket connection for real-time messaging
- `/ws/file` - WebSocket connection for file uploads

## Testing

Run the test suite:
```bash
./mvnw test
```

For detailed testing guides, see:
- [Testing Guide](TESTING-GUIDE.md)
- [WebSocket Testing Guide](WebSocket-Testing-Guide.md)
- [Postman Testing Guide](docs/postman_testing_guide.md)

## File Upload

The application supports file uploads with the following features:

- **Multiple file types**: Images, videos, documents, audio
- **Chunked upload** for large files
- **File size limits** configurable per file type
- **Secure file storage** with unique file naming

For detailed file upload documentation, see [File Upload Guide](docs/file_upload_guide.md).

## Security

- **JWT Authentication** with configurable expiration
- **Role-based access control** (USER, ADMIN)
- **CORS configuration** for cross-origin requests
- **Input validation** and sanitization
- **Secure file upload** with type validation

## Database Schema

The application uses the following main entities:

- **User**: User account information
- **ChatRoom**: Chat room details and participants
- **Message**: Chat messages with metadata
- **FileMetadata**: File upload information

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

- **Author**: Hamza Damra
- **GitHub**: [@hamza-damra](https://github.com/hamza-damra)
- **Repository**: [Chatting-Application-Back-End](https://github.com/hamza-damra/Chatting-Application-Back-End)

## Related Projects

- [Chatting Application Frontend](https://github.com/hamza-damra/Chatting-Application-Front-End) - Flutter mobile client
