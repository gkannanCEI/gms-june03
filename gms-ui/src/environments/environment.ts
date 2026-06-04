export const environment = {
  production: false,
  apiBaseUrl: '',
  // auth-mode: 'b2c' or 'local'
  // When 'local', the app shows a login screen and uses HTTP Basic auth
  // When 'b2c', the app uses MSAL/Azure AD B2C redirect flow
  authMode: 'local' as 'b2c' | 'local',
  b2c: {
    clientId: 'ANGULAR_APP_CLIENT_ID',
    authority: 'https://TENANT.b2clogin.com/TENANT.onmicrosoft.com/B2C_1_signin',
    knownAuthorities: ['TENANT.b2clogin.com'],
    redirectUri: 'http://localhost:42001',
    postLogoutRedirectUri: 'http://localhost:42001',
    scopes: ['https://TENANT.onmicrosoft.com/gms-api/access_as_user']
  }
};
