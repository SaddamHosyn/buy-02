// src/environments/environment.ts
// This is the DEFAULT file for local development
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8082',
  apiGatewayUrl: 'http://localhost:8080',
  authUrl: 'http://localhost:8081/auth',
  usersUrl: 'http://localhost:8081/users',
  productsUrl: 'http://localhost:8082/products',
  ordersUrl: 'http://localhost:8084/orders',
  cartUrl: 'http://localhost:8084/cart',
  mediaUrl: 'http://localhost:8083/media',
  enableDebugLogging: true
};
