import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { AUTH_FORBIDDEN_EVENT, AUTH_UNAUTHORIZED_EVENT } from '../services/httpClient'
import { ROUTES } from '../constants/routes'

export function useAuthEvents() {
  const navigate = useNavigate()

  useEffect(() => {
    const handleUnauthorized = () => navigate(ROUTES.login, { replace: true })
    const handleForbidden = () => navigate(ROUTES.home, { replace: true })

    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized)
    window.addEventListener(AUTH_FORBIDDEN_EVENT, handleForbidden)
    return () => {
      window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized)
      window.removeEventListener(AUTH_FORBIDDEN_EVENT, handleForbidden)
    }
  }, [navigate])
}
