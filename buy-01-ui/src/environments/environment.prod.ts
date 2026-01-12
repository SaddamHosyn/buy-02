// src/environments/environment.prod.ts
// This is the PRODUCTION file for Docker builds
export const environment = {
  production: true,
  // Always use HTTP for API calls since API Gateway only serves HTTP on port 8080
  // Frontend can be accessed via HTTP (4200) or HTTPS (4201)
  apiUrl: `http://${window.location.hostname}:8080/api`,
  apiGatewayUrl: `http://${window.location.hostname}:8080`,
  authUrl: `http://${window.location.hostname}:8080/api/auth`,
  usersUrl: `http://${window.location.hostname}:8080/api/users`,
  productsUrl: `http://${window.location.hostname}:8080/api/products`,
  mediaUrl: `http://${window.location.hostname}:8080/api/media`,
  enableDebugLogging: false,
  buildTimestamp: '2026-01-08T13:00:00Z',
};
