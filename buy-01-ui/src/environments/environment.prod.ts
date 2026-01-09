// src/environments/environment.prod.ts
// This is the PRODUCTION file for Docker builds
export const environment = {
  production: true,
  // Automatically detects if it's on AWS or Localhost
  apiUrl: `http://${window.location.hostname}:8080/api`,
  apiGatewayUrl: `http://${window.location.hostname}:8080`,
  authUrl: `http://${window.location.hostname}:8080/api/auth`,
  usersUrl: `http://${window.location.hostname}:8080/api/users`,
  productsUrl: `http://${window.location.hostname}:8080/api/products`,
  mediaUrl: `http://${window.location.hostname}:8080/api/media`,
  enableDebugLogging: false,
  buildTimestamp: '2026-01-08T13:00:00Z',
};
