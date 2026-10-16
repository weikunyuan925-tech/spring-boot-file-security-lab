# Security Checklist

## Authentication

- [ ] Is password stored securely?
- [ ] Does login endpoint have rate limit?
- [ ] Is token generated securely?
- [ ] Does token expire?

## Authorization

- [ ] Does the API check current user identity?
- [ ] Can one user access another user's data?

## File Upload

- [ ] Is file type validated on server side?
- [ ] Is file size limited?
- [ ] Are dangerous file extensions blocked?
- [ ] Are uploaded files renamed?

## Configuration

- [ ] Are database credentials hardcoded?
- [ ] Are debug endpoints exposed?
- [ ] Does error response leak stack trace?

## Dependency

- [ ] Are dependencies scanned for CVE?
- [ ] Are vulnerable versions upgraded?