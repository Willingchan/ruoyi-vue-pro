# Agent API Service - Documentation

## 1. Introduction

This document provides a detailed specification for the RESTful API that allows an AI agent to interact with the core business data of the RuoYi-Vue-Pro project.

### 1.1. Base URL
All API endpoints are prefixed with:
`/api/v1`

### 1.2. Authentication
All requests to this API should be authenticated. The server expects an API Key to be provided in the `Authorization` header.

**Header:**
`Authorization: Bearer <YOUR_API_KEY>`

### 1.3. General Conventions

*   **Request/Response Format**: All data is sent and received as JSON.
*   **Pagination**: List endpoints (`GET /resources`) use `page` and `size` query parameters.
*   **Error Handling**: The API uses standard HTTP status codes to indicate success or failure. Error responses will include a JSON body with `detail` and `error_code` fields.

---

## 2. User Management
**Resource:** `/users`

### 2.1. Get User List
- **Method**: `GET`
- **Path**: `/users`
- **Description**: Retrieves a paginated list of users. Supports filtering by username and department ID.
- **Query Parameters**:
  - `page: int` (optional, default: 1) - The page number.
  - `size: int` (optional, default: 10) - The number of items per page.
  - `username: str` (optional) - Filter by username (exact match).
  - `dept_id: int` (optional) - Filter by department ID.
- **Success Response**: `200 OK`
  ```json
  {
    "total": 1,
    "items": [
      {
        "id": 1,
        "username": "admin",
        "nickname": "Admin",
        "email": "admin@example.com",
        "dept_id": 103,
        "status": 0,
        "create_time": "2023-01-01T12:00:00"
      }
    ]
  }
  ```

### 2.2. Get User Details
- **Method**: `GET`
- **Path**: `/users/{user_id}`
- **Description**: Retrieves details for a specific user.
- **Path Parameters**:
  - `user_id: int` (required) - The ID of the user.
- **Success Response**: `200 OK`
  ```json
  {
    "id": 1,
    "username": "admin",
    "nickname": "Admin",
    "email": "admin@example.com",
    "dept_id": 103,
    "status": 0,
    "create_time": "2023-01-01T12:00:00"
  }
  ```
- **Error Response**: `404 Not Found` if the user does not exist.

### 2.3. Create User
- **Method**: `POST`
- **Path**: `/users`
- **Description**: Creates a new user.
- **Request Body**:
  ```json
  {
    "username": "newUser",
    "password": "complex_password_123",
    "nickname": "New User",
    "email": "new.user@example.com",
    "dept_id": 105
  }
  ```
- **Success Response**: `201 Created` (returns the created user object)
- **Error Response**: `400 Bad Request` if validation fails.

### 2.4. Update User
- **Method**: `PUT`
- **Path**: `/users/{user_id}`
- **Description**: Updates an existing user's information.
- **Request Body**:
  ```json
  {
    "nickname": "Updated Nickname",
    "email": "updated.user@example.com",
    "status": 1
  }
  ```
- **Success Response**: `200 OK` (returns the updated user object)
- **Error Response**: `404 Not Found`.

### 2.5. Delete User
- **Method**: `DELETE`
- **Path**: `/users/{user_id}`
- **Description**: Deletes a user.
- **Success Response**: `204 No Content`
- **Error Response**: `404 Not Found`.

---

## 3. Role Management
**Resource:** `/roles`
*(The structure for GET list, GET by ID, POST, PUT, DELETE follows the same pattern as User Management.)*

---

## 4. Department Management
**Resource:** `/depts`
*(The structure for GET list, GET by ID, POST, PUT, DELETE follows the same pattern as User Management. The GET list endpoint should return a tree structure if possible.)*

---

## 5. Dictionary Data Management
**Resource:** `/dict-data`

### 5.1. Get Dictionary Data List
- **Method**: `GET`
- **Path**: `/dict-data`
- **Description**: Retrieves a list of dictionary data items, typically filtered by dictionary type.
- **Query Parameters**:
  - `dict_type: str` (required) - The type of the dictionary to retrieve (e.g., "sys_user_sex").
- **Success Response**: `200 OK`
  ```json
  {
    "total": 2,
    "items": [
      {
        "id": 1,
        "dict_type": "sys_user_sex",
        "label": "Male",
        "value": "0",
        "sort": 1
      },
      {
        "id": 2,
        "dict_type": "sys_user_sex",
        "label": "Female",
        "value": "1",
        "sort": 2
      }
    ]
  }
  ```
*(Other endpoints for POST, PUT, DELETE on `/dict-data/{data_id}` follow the standard pattern.)*
