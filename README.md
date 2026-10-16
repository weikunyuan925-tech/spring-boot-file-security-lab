# Spring Boot File Security Lab

This project is a Java application security learning project.

It is based on Spring Boot, MySQL, and MyBatis-Plus.

The goal is to learn common Java web vulnerabilities and how to fix them.

## Features

- User registration
- User login
- Simple token authentication
- File upload

## Tech Stack

- Java 17
- Spring Boot
- MyBatis-Plus
- MySQL
- Maven
- Postman

## Security Notes

This project intentionally contains some insecure implementations for learning purposes.

These issues will be audited and fixed step by step.

Known issues:

- Password is stored in plain text
- Login endpoint has no rate limit
- File upload validation is weak
- Token is stored in memory
- Some sensitive configuration may be hardcoded