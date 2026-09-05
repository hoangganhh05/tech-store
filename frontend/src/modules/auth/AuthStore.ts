import { createContext } from 'react'
import type { AuthenticatedUser, LoginResult } from '../../services/authService'

export type AuthContextValue = {
  user: AuthenticatedUser | null
  isAuthenticated: boolean
  signIn: (result: LoginResult) => void
  updateUserProfile: (profile: Pick<AuthenticatedUser, 'fullName' | 'phone'>) => void
  signOut: () => Promise<void>
  clearSession: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
