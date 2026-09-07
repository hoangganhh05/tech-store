const SESSION_ID_KEY = "techstore.sessionId";

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
