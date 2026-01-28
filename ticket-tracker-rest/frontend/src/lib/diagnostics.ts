export const runDiagnostics = () => {
  const results = {
    environment: {
      isDev: import.meta.env.DEV,
      mode: import.meta.env.MODE,
      apiUrl: import.meta.env.VITE_API_BASE_URL || 'not configured',
      apiTimeout: import.meta.env.VITE_API_TIMEOUT || 'default',
    },
    browser: {
      userAgent: navigator.userAgent,
      language: navigator.language,
      online: navigator.onLine,
    },
    storage: {
      localStorage: typeof localStorage !== 'undefined',
      sessionStorage: typeof sessionStorage !== 'undefined',
    },
    timestamp: new Date().toISOString(),
  };

  console.group('Application Diagnostics');
  console.table(results.environment);
  console.table(results.browser);
  console.table(results.storage);
  console.log('Timestamp:', results.timestamp);
  console.groupEnd();

  return results;
};

export const checkDatabaseConnection = async () => {
  try {
    const apiUrl = import.meta.env.VITE_API_BASE_URL;

    if (!apiUrl) {
      return {
        status: 'disconnected',
        message: 'API URL not configured',
      };
    }

    const response = await fetch(`${apiUrl}/modules`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      return {
        status: 'error',
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
    }

    return {
      status: 'connected',
      message: 'Backend API connection successful',
    };
  } catch (error) {
    return {
      status: 'error',
      message: error instanceof Error ? error.message : 'Unknown error',
      error,
    };
  }
};

export default {
  runDiagnostics,
  checkDatabaseConnection,
};
