export const isApiConfigured = (): boolean => {
  const url = import.meta.env.VITE_API_BASE_URL;
  return !!(url && url !== 'http://localhost:8080/api');
};

export const getEnvironmentMode = (): 'production' | 'development' => {
  return import.meta.env.MODE === 'production' ? 'production' : 'development';
};

export const getAuthMode = (): 'jwt' => {
  return 'jwt';
};

export const getEnvironmentConfig = () => {
  const mode = getEnvironmentMode();
  return {
    mode,
    authMode: 'jwt',
    apiUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
    apiTimeout: parseInt(import.meta.env.VITE_API_TIMEOUT || '30000', 10),
    isProduction: mode === 'production',
    isDevelopment: mode === 'development',
    enableLogging: import.meta.env.VITE_ENABLE_LOGGING === 'true',
  };
};
