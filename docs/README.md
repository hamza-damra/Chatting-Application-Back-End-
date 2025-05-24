# Chat Application API Testing Documentation

Welcome to the Chat Application API testing documentation. This repository contains guides and resources to help you test the Chat Application backend using Postman and other tools.

## Table of Contents

1. [Getting Started](#getting-started)
2. [API Documentation](#api-documentation)
3. [Postman Testing](#postman-testing)
4. [WebSocket Testing](#websocket-testing)
5. [Troubleshooting](#troubleshooting)

## Getting Started

Before you begin testing, make sure:

1. The Chat Application backend is running on `http://localhost:8080`
2. MySQL database is properly configured and running
3. You have Postman installed (or another API testing tool)

## API Documentation

The Chat Application provides a RESTful API for managing users, chat rooms, and messages:

- [API Endpoints Reference](api_endpoints_reference.md) - Complete list of all available endpoints

## Postman Testing

We've prepared comprehensive guides for testing with Postman:

- [Postman Testing Guide](postman_testing_guide.md) - Step-by-step instructions for testing each endpoint
- [Postman Collection Setup](postman_collection_setup.md) - How to set up a structured Postman collection

## WebSocket Testing

For real-time messaging features:

- [WebSocket Testing Guide](websocket_testing_guide.md) - How to test WebSocket connections and STOMP messaging

## Troubleshooting

### Common Issues

#### Authentication Problems

- **Issue**: 401 Unauthorized errors
- **Solution**: Ensure your JWT token is valid and included in the Authorization header

#### Database Connection Issues

- **Issue**: Application fails to start with database errors
- **Solution**: Verify MySQL is running and the connection details in application.yml are correct

#### CORS Issues

- **Issue**: Browser-based clients receive CORS errors
- **Solution**: Ensure the client origin is included in the CORS allowed origins configuration

#### WebSocket Connection Problems

- **Issue**: Unable to establish WebSocket connection
- **Solution**: Check that the WebSocket endpoint is correct and the server is running

### Getting Help

If you encounter issues not covered in this documentation, please:

1. Check the application logs for detailed error messages
2. Review the Spring Boot documentation for configuration issues
3. Search for similar issues in the Spring Framework or related technologies

## Default Test User

For testing purposes, the application is initialized with an admin user:

- **Username**: admin
- **Password**: admin
- **Roles**: ADMIN, USER

You can use these credentials to obtain a JWT token for testing protected endpoints.
