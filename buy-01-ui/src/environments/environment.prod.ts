// src/environments/environment.prod.ts
// This is the PRODUCTION file for Docker builds// Updated: 2026-01-08 - New infrastructure IP addressesexport const environment = {
  production: true,
  apiUrl: 'http://13.61.234.232:8080/api',
  apiGatewayUrl: 'http://13.61.234.232:8080',
  authUrl: 'http://13.61.234.232:8080/api/auth',
  usersUrl: 'http://13.61.234.232:8080/api/users',
  productsUrl: 'http://13.61.234.232:8080/api/products',
  mediaUrl: 'http://13.61.234.232:8080/api/media',
  enableDebugLogging: false,
};
