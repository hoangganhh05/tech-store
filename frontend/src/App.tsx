import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from './modules/auth/AuthContext'
import { appRouter } from './routers/appRouter'

export default function App() {
  return (
    <AuthProvider>
      <RouterProvider router={appRouter} />
    </AuthProvider>
  )
}
