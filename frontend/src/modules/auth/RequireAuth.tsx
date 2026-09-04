import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../hooks/useAuth'

export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation()
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    const from = `${location.pathname}${location.search}${location.hash}`
    return <Navigate to={ROUTES.login} replace state={{ from }} />
  }

  return <>{children}</>
}
