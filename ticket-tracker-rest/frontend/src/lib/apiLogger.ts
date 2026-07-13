const LOGGING_ENABLED = import.meta.env.VITE_ENABLE_LOGGING === 'true';

function redactSensitiveData(data: any): any {
  if (typeof data !== 'object' || data === null) {
    return data;
  }

  const redacted = { ...data };
  const sensitiveKeys = ['password', 'token', 'secret', 'authorization'];

  Object.keys(redacted).forEach((key) => {
    if (sensitiveKeys.some((sensitive) => key.toLowerCase().includes(sensitive))) {
      redacted[key] = '[REDACTED]';
    } else if (typeof redacted[key] === 'object') {
      redacted[key] = redactSensitiveData(redacted[key]);
    }
  });

  return redacted;
}

export function logApiRequest(method: string, url: string, body?: any): void {
  if (!LOGGING_ENABLED) return;

  console.groupCollapsed(`🔵 API ${method} ${url}`);
  console.log('URL:', url);
  console.log('Method:', method);
  if (body) {
    console.log('Body:', redactSensitiveData(body));
  }
  console.log('Timestamp:', new Date().toISOString());
  console.groupEnd();
}

export function logApiResponse(status: number, data: any): void {
  if (!LOGGING_ENABLED) return;

  console.groupCollapsed(`🟢 API Response ${status}`);
  console.log('Status:', status);
  console.log('Data:', redactSensitiveData(data));
  console.log('Timestamp:', new Date().toISOString());
  console.groupEnd();
}

export function logApiError(error: any): void {
  if (!LOGGING_ENABLED) return;

  console.groupCollapsed(`🔴 API Error ${error.code || 'UNKNOWN'}`);
  console.log('Code:', error.code);
  console.log('Message:', error.message);
  if (error.details) {
    console.log('Details:', error.details);
  }
  console.log('Timestamp:', new Date().toISOString());
  console.groupEnd();
}
