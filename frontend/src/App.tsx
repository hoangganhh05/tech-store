import { RouterProvider } from 'react-router-dom'
import { appRouter } from './routers/appRouter'

export default function App() {
  return <RouterProvider router={appRouter} />
}
