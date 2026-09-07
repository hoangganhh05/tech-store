const SESSION_ID_KEY = "techstore.sessionId";

export function getSessionId(): string | null {
  try {
    return window.localStorage.getItem(SESSION_ID_KEY);
  } catch {
    return null;
  }
}

export function getOrCreateSessionId(): string {
  try {
    let sessionId = window.localStorage.getItem(SESSION_ID_KEY);
    if (!sessionId) {
      sessionId =
        "guest_" +
        Math.random().toString(36).substring(2, 15) +
        Date.now().toString(36);
      window.localStorage.setItem(SESSION_ID_KEY, sessionId);
    }
    return sessionId;
  } catch {
    return "guest_fallback_session";
  }
}

export function rotateSessionId(): string {
  try {
    const newSessionId =
      "guest_" +
      Math.random().toString(36).substring(2, 15) +
      Date.now().toString(36);
    window.localStorage.setItem(SESSION_ID_KEY, newSessionId);
    return newSessionId;
  } catch {
    return "guest_fallback_session";
  }
}

export function clearSessionId(): void {
  try {
    window.localStorage.removeItem(SESSION_ID_KEY);
  } catch {
    // Ignore error in non-browser environments
  }
}

