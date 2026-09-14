import { useContext, type ReactNode } from 'react'
import { Avatar, Box, Card, Divider, Stack, Typography } from '@mui/material'
import PersonOutlineIcon from '@mui/icons-material/PersonOutline'
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder'
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import { NavLink } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { AuthContext } from '../../modules/auth/AuthStore'

const sections = [
  { to: ROUTES.profile, label: 'Hồ sơ & bảo mật', icon: PersonOutlineIcon },
  { to: ROUTES.orders, label: 'Đơn hàng của tôi', icon: Inventory2OutlinedIcon },
  { to: ROUTES.wishlist, label: 'Danh sách yêu thích', icon: FavoriteBorderIcon },
  { to: ROUTES.addresses, label: 'Địa chỉ giao hàng', icon: LocationOnOutlinedIcon },
]

export function AccountLayout({ children }: { children: ReactNode }) {
  const auth = useContext(AuthContext)
  const user = auth?.user

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: '248px minmax(0, 1fr)' }, gap: 3, alignItems: 'start' }}>
      <Card component="aside" sx={{ position: { md: 'sticky' }, top: 104, overflow: 'hidden' }}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ p: 2.5 }}>
          <Avatar sx={{ width: 44, height: 44, bgcolor: 'primary.main', fontWeight: 800 }}>{user?.fullName?.trim().charAt(0).toUpperCase() || <PersonOutlineIcon />}</Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle2" fontWeight={700} noWrap>{user?.fullName || 'Tài khoản của tôi'}</Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', overflowWrap: 'anywhere' }}>{user?.email || 'Quản lý thông tin mua sắm'}</Typography>
          </Box>
        </Stack>
        <Divider />
        <Box component="nav" aria-label="Điều hướng tài khoản" sx={{ p: 1, display: 'grid', gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', md: '1fr' }, gap: 0.5 }}>
          {sections.map(({ to, label, icon: Icon }) => (
            <Box key={to} component={NavLink} to={to} sx={{ display: 'flex', alignItems: 'center', gap: 1, p: 1.5, borderRadius: '10px', textDecoration: 'none', color: 'text.secondary', fontSize: 13, fontWeight: 500, transition: 'background-color 160ms', '&:hover': { bgcolor: '#F7F9FC', color: 'primary.main' }, '&.active': { bgcolor: '#EAF1FB', color: 'primary.main', fontWeight: 700 } }}>
              <Icon sx={{ fontSize: 20, flexShrink: 0 }} />
              <Box component="span" sx={{ flex: 1 }}>{label}</Box>
              <ChevronRightIcon sx={{ fontSize: 16, display: { xs: 'none', md: 'block' } }} />
            </Box>
          ))}
        </Box>
      </Card>
      <Box sx={{ minWidth: 0 }}>{children}</Box>
    </Box>
  )
}
