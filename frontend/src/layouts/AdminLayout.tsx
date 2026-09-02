import { AppBar, Box, Button, Container, Drawer, List, ListItemButton, ListItemText, Toolbar, Typography } from '@mui/material'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { ROUTES } from '../constants/routes'
import { useAuthEvents } from '../hooks/useAuthEvents'

const drawerWidth = 240
const adminItems = [
  { label: 'Tổng quan', to: ROUTES.admin },
  { label: 'Sản phẩm', to: ROUTES.adminProducts },
  { label: 'Đơn hàng', to: ROUTES.adminOrders },
]

export function AdminLayout() {
  useAuthEvents()
  return (
    <Box minHeight="100vh" bgcolor="background.default">
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" fontWeight={800} flex={1}>TechStore Admin</Typography>
          <Button component={Link} to={ROUTES.home} color="inherit">Về cửa hàng</Button>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{ width: drawerWidth, '& .MuiDrawer-paper': { width: drawerWidth, pt: 8 } }}
      >
        <List component="nav" aria-label="Điều hướng quản trị">
          {adminItems.map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              end={item.to === ROUTES.admin}
              sx={{ '&.active': { bgcolor: '#ffebee', color: 'primary.main' } }}
            >
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box component="main" ml={`${drawerWidth}px`} pt={11} pb={5}>
        <Container maxWidth="lg"><Outlet /></Container>
      </Box>
    </Box>
  )
}
