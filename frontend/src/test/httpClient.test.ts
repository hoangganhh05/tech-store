import AxiosMockAdapter from 'axios-mock-adapter'
import { AUTH_UNAUTHORIZED_EVENT, httpClient } from '../services/httpClient'
import { logoutAccount } from '../services/authService'
import { tokenStorage } from '../utils/tokenStorage'

describe('httpClient', () => {
  const mock = new AxiosMockAdapter(httpClient)

  afterEach(() => {
    mock.reset()
    tokenStorage.clear()
  })

  it('attaches the access token to requests', async () => {
    tokenStorage.setTokens('access-token')
    mock.onGet('/profile').reply((config) => [200, { authorization: config.headers?.Authorization }])
    const response = await httpClient.get<{ authorization: string }>('/profile')
    expect(response.data.authorization).toBe('Bearer access-token')
  })

  it('clears tokens and emits an event for 401 responses', async () => {
    tokenStorage.setTokens('expired-token', 'refresh-token')
    const eventHandler = vi.fn()
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, eventHandler)
    mock.onGet('/private').reply(401)

    await expect(httpClient.get('/private')).rejects.toBeTruthy()
    expect(tokenStorage.getAccessToken()).toBeNull()
    expect(eventHandler).toHaveBeenCalledOnce()
    window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, eventHandler)
  })

  it('does not redirect away from the login form for invalid credentials', async () => {
    const eventHandler = vi.fn()
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, eventHandler)
    mock.onPost('/auth/login').reply(401, { message: 'Email hoặc mật khẩu không đúng' })

    await expect(httpClient.post('/auth/login', { email: 'customer@example.com', password: 'wrong-password' })).rejects.toBeTruthy()
    expect(eventHandler).not.toHaveBeenCalled()
    window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, eventHandler)
  })

  it('leaves logout failures to the sign-out flow so it can clear the full client session', async () => {
    tokenStorage.setTokens('expired-token', 'refresh-token')
    const eventHandler = vi.fn()
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, eventHandler)
    mock.onPost('/auth/logout').reply(401)

    await expect(httpClient.post('/auth/logout', { refreshToken: 'refresh-token' })).rejects.toBeTruthy()
    expect(tokenStorage.getAccessToken()).toBe('expired-token')
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token')
    expect(eventHandler).not.toHaveBeenCalled()
    window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, eventHandler)
  })

  it('posts the refresh token to the logout endpoint', async () => {
    mock.onPost('/auth/logout').reply(204)

    await logoutAccount({ refreshToken: 'refresh-token' })

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0].data).toBe(JSON.stringify({ refreshToken: 'refresh-token' }))
  })
})
