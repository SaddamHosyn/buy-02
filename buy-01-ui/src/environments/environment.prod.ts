// src/environments/environment.prod.ts
// This is the PRODUCTION file for Docker builds
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080/api',
  apiGatewayUrl: 'http://localhost:8080',
  authUrl: 'http://localhost:8080/api/auth',
  usersUrl: 'http://localhost:8080/api/users',
  productsUrl: 'http://localhost:8080/api/products',
  mediaUrl: 'http://localhost:8080/api/media',
  enableDebugLogging: false
};
