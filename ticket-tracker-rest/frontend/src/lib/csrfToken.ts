const CSRF_TOKEN_KEY = 'csrf_token';
const CSRF_TOKEN_TIMESTAMP_KEY = 'csrf_token_timestamp';
const TOKEN_MAX_AGE = 30 * 60 * 1000;

let inMemoryCsrfToken: string | null = null;

export const getCsrfToken = (): string | null => {
  if (inMemoryCsrfToken) {
    return inMemoryCsrfToken;
  }

  try {
    const token = localStorage.getItem(CSRF_TOKEN_KEY);
    const timestamp = localStorage.getItem(CSRF_TOKEN_TIMESTAMP_KEY);

    if (!token || !timestamp) {
      return null;
    }

    const tokenAge = Date.now() - parseInt(timestamp, 10);
    if (tokenAge > TOKEN_MAX_AGE) {
      clearCsrfToken();
      return null;
    }

    inMemoryCsrfToken = token;
    return token;
  } catch (error) {
    console.warn('Error reading CSRF token from localStorage:', error);
    return null;
  }
};

export const setCsrfToken = (token: string): void => {
  inMemoryCsrfToken = token;
  try {
    localStorage.setItem(CSRF_TOKEN_KEY, token);
    localStorage.setItem(CSRF_TOKEN_TIMESTAMP_KEY, Date.now().toString());
  } catch (error) {
    console.warn('Error storing CSRF token in localStorage:', error);
  }
};

export const clearCsrfToken = (): void => {
  inMemoryCsrfToken = null;
  try {
    localStorage.removeItem(CSRF_TOKEN_KEY);
    localStorage.removeItem(CSRF_TOKEN_TIMESTAMP_KEY);
  } catch (error) {
    console.warn('Error clearing CSRF token from localStorage:', error);
  }
};

export const hasCsrfToken = (): boolean => {
  return getCsrfToken() !== null;
};
