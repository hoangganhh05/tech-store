const ACCESS_TOKEN_KEY = 'techstore.accessToken'
const REFRESH_TOKEN_KEY = 'techstore.refreshToken'

export const tokenStorage = {
  getAccessToken: () => window.localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => window.localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken?: string) => {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    if (refreshToken) window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
  clear: () => {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
    window.localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}
