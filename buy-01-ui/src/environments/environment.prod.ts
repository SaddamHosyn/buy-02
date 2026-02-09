// src/environments/environment.prod.ts
// This is the PRODUCTION file for Docker builds
//
// NOTE: We use a helper function so that `window.location.hostname` is
// evaluated at **runtime** in the browser, not at Angular build-time.
// Static template-literal initialisers are compiled to constants and
// `window` may not exist during the build, which previously caused
// ERR_NAME_NOT_RESOLVED errors.

function apiBase(): string {
  const host = window.location.hostname;
  return `https://${host}:8443/api`;
}

function gatewayBase(): string {
  const host = window.location.hostname;
  return `https://${host}:8443`;
}

export const environment = {
  production: true,
  get apiUrl()        { return apiBase(); },
  get apiGatewayUrl() { return gatewayBase(); },
  get authUrl()       { return `${apiBase()}/auth`; },
  get usersUrl()      { return `${apiBase()}/users`; },
  get productsUrl()   { return `${apiBase()}/products`; },
  get ordersUrl()     { return `${apiBase()}/orders`; },
  get cartUrl()       { return `${apiBase()}/cart`; },
  get mediaUrl()      { return `${apiBase()}/media`; },
  get profileUrl()    { return `${apiBase()}/profile`; },
  enableDebugLogging: false,
  buildTimestamp: '2026-01-08T13:00:00Z',
};
