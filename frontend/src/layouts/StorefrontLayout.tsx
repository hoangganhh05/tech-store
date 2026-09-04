import { AppBar, Badge, Box, Button, Container, IconButton, Stack, Toolbar, Typography } from '@mui/material'
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { ROUTES } from '../constants/routes'
import { env } from '../configs/env'
import { useAuthEvents } from '../hooks/useAuthEvents'

const navItems = [
  { label: 'Trang chủ', to: ROUTES.home },
  { label: 'Sản phẩm', to: ROUTES.products },
]

export function StorefrontLayout() {
  useAuthEvents()
  return (
    <Box minHeight="100vh" display="flex" flexDirection="column">
      <a className="skip-link" href="#main-content">Bỏ qua điều hướng</a>
      <AppBar position="sticky" color="inherit" sx={{ borderBottom: '1px solid #e4e7eb' }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: 3 }}>
            <Typography component={Link} to={ROUTES.home} variant="h6" color="primary" fontWeight={800}>
              {env.appName}
            </Typography>
            <Stack component="nav" direction="row" spacing={0.5} flex={1} aria-label="Điều hướng chính">
              {navItems.map((item) => (
                <Button
                  key={item.to}
                  component={NavLink}
                  to={item.to}
                  end={item.to === '/'}
                  color="inherit"
                  sx={{ '&.active': { color: 'primary.main', bgcolor: '#fff1f1' } }}
                >
                  {item.label}
                </Button>
              ))}
            </Stack>
            <Button component={Link} to={ROUTES.login} color="inherit">Đăng nhập</Button>
            <IconButton component={Link} to={ROUTES.cart} aria-label="Giỏ hàng">
              <Badge badgeContent={0} color="primary"><ShoppingCartOutlinedIcon /></Badge>
            </IconButton>
          </Toolbar>
        </Container>
      </AppBar>
      <Box id="main-content" component="main" flex={1} py={{ xs: 3, md: 5 }}>
        <Container maxWidth="lg"><Outlet /></Container>
      </Box>
      <Box component="footer" bgcolor="#263238" color="white" py={3}>
        <Container maxWidth="lg">
          <Typography fontWeight={700}>{env.appName}</Typography>
          <Typography variant="body2" color="#cfd8dc">Điện thoại và phụ kiện công nghệ.</Typography>
        </Container>
      </Box>
    </Box>
  )
}
