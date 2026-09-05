import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../hooks/useAuth'

interface RequireAuthProps {
  children: ReactNode
  requiredRoles?: string[]
}

export function RequireAuth({ children, requiredRoles }: RequireAuthProps) {
  const location = useLocation()
  const { user, isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    const from = `${location.pathname}${location.search}${location.hash}`
    const isAccessingAdmin = location.pathname.startsWith('/admin')
    return (
      <Navigate
        to={isAccessingAdmin ? ROUTES.adminLogin : ROUTES.login}
        replace
        state={{ from }}
      />
    )
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = user?.roles ?? []
    const hasRequiredRole = requiredRoles.some((role) => userRoles.includes(role))
    if (!hasRequiredRole) {
      return <Navigate to={ROUTES.forbidden} replace />
    }
  }

  return <>{children}</>
}