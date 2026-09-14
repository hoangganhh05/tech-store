import {
  Alert,
  AppBar,
  Avatar,
  Badge,
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  InputAdornment,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Snackbar,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded'
import ClearRoundedIcon from '@mui/icons-material/ClearRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import FavoriteBorderRoundedIcon from '@mui/icons-material/FavoriteBorderRounded'
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import MailOutlineRoundedIcon from '@mui/icons-material/MailOutlineRounded'
import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded'
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined'
import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { BrandMark } from '../components/common/BrandMark'
import { DocumentMetadata } from '../components/common/DocumentMetadata'
import { env } from '../configs/env'
import { ROUTES } from '../constants/routes'
import { useAuth } from '../hooks/useAuth'
import { useAuthEvents } from '../hooks/useAuthEvents'
import { useCart } from '../hooks/useCart'
import { getStorefrontCategories } from '../services/storefrontService'
import type { Category } from '../services/categoryService'

type NavItem = { label: string; to: string }

const fallbackNavItems: NavItem[] = [
  { label: 'Điện thoại', to: ROUTES.products },
  { label: 'Tai nghe', to: ROUTES.products },
  { label: 'Phụ kiện', to: ROUTES.products },
]

export function StorefrontLayout() {
  useAuthEvents()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const theme = useTheme()
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'))
  const { isAuthenticated, user, signOut } = useAuth()
  const { cartCount, syncNotification, clearSyncNotification } = useCart()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [accountAnchor, setAccountAnchor] = useState<HTMLElement | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState(searchParams.get('q') || '')
  const [categories, setCategories] = useState<Category[]>([])
  const isAdmin = Boolean(user?.roles?.includes('ADMIN'))
  const isHome = location.pathname === ROUTES.home

  useEffect(() => {
    setSearchKeyword(searchParams.get('q') || '')
  }, [searchParams])

  useEffect(() => {
    let active = true
    Promise.resolve(getStorefrontCategories())
      .then((items) => {
        if (active) setCategories((items ?? []).filter((item) => item.isActive && !item.parentId).slice(0, 4))
      })
      .catch(() => {
        // Navigation stays usable if categories are temporarily unavailable.
      })
    return () => {
      active = false
    }
  }, [])

  const navItems: NavItem[] = [
    { label: 'Trang chủ', to: ROUTES.home },
    ...(categories.length
      ? categories.map((category) => ({ label: category.name, to: `${ROUTES.products}?categoryId=${category.id}` }))
      : fallbackNavItems),
    { label: 'Khuyến mãi', to: '/#on-sale-section' },
  ]

  const accountLinks = [
    ...(isAdmin ? [{ label: 'Trang quản trị', to: ROUTES.admin, icon: <AdminPanelSettingsRoundedIcon /> }] : []),
    { label: 'Tài khoản', to: ROUTES.profile, icon: <PersonOutlineRoundedIcon /> },
    { label: 'Đơn hàng', to: ROUTES.orders, icon: <ReceiptLongOutlinedIcon /> },
    ...(!isAdmin ? [{ label: 'Yêu thích', to: ROUTES.wishlist, icon: <FavoriteBorderRoundedIcon /> }] : []),
  ]

  const closeMenus = () => {
    setDrawerOpen(false)
    setAccountAnchor(null)
  }

  const handleSearchSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    const keyword = searchKeyword.trim()
    navigate(keyword ? `${ROUTES.products}?q=${encodeURIComponent(keyword)}` : ROUTES.products)
    closeMenus()
  }

  const handleClearSearch = () => {
    setSearchKeyword('')
    if (searchParams.get('q')) navigate(ROUTES.products)
  }

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await signOut()
      navigate(ROUTES.home, { replace: true })
    } finally {
      setIsLoggingOut(false)
      closeMenus()
    }
  }

  const isCurrent = (item: NavItem) => `${location.pathname}${location.search}${location.hash}` === item.to

  const searchField = (
    <Box component="form" role="search" aria-label="Tìm kiếm sản phẩm" onSubmit={handleSearchSubmit} width="100%">
      <TextField
        fullWidth
        size="small"
        value={searchKeyword}
        placeholder="Tìm iPhone, Samsung, tai nghe..."
        onChange={(event) => setSearchKeyword(event.target.value)}
        slotProps={{
          htmlInput: {
            'aria-label': 'Từ khoá tìm kiếm',
            'data-testid': 'header-search-input',
          },
          input: {
            startAdornment: <InputAdornment position="start"><SearchRoundedIcon sx={{ color: '#8b9fb4', fontSize: 21 }} /></InputAdornment>,
            endAdornment: <InputAdornment position="end">
              {searchKeyword && <IconButton size="small" onClick={handleClearSearch} aria-label="Xoá từ khoá tìm kiếm"><ClearRoundedIcon fontSize="small" /></IconButton>}
              <IconButton type="submit" size="small" data-testid="header-search-button" aria-label="Tìm kiếm" sx={{ width: 36, height: 36, color: 'primary.main' }}><SearchRoundedIcon fontSize="small" /></IconButton>
            </InputAdornment>,
            sx: { height: 44, pr: .5, bgcolor: 'background.default', fontSize: 14 },
          },
        }}
      />
    </Box>
  )

  return (
    <Box minHeight="100vh" minWidth={0} display="flex" flexDirection="column">
      <DocumentMetadata />
      <a className="skip-link" href="#main-content">Bỏ qua điều hướng</a>

      {isDesktop && <Box bgcolor="secondary.main" color="#d8e5f4">
        <Container maxWidth="xl">
          <Stack direction="row" justifyContent="space-between" alignItems="center" gap={2} minHeight={32}>
            <Stack direction="row" alignItems="center" spacing={2} divider={<Typography variant="caption" color="rgba(255,255,255,.35)">•</Typography>}>
              <Typography variant="caption" display="flex" alignItems="center" gap={.75}><LocationOnOutlinedIcon sx={{ fontSize: 14 }} />{env.brand.address}</Typography>
              <Typography variant="caption" whiteSpace="nowrap">{env.brand.contact.phone}</Typography>
            </Stack>
            <Typography variant="caption" whiteSpace="nowrap">Giao hàng toàn quốc · Bảo hành chính hãng</Typography>
          </Stack>
        </Container>
      </Box>}

      <AppBar position="sticky" color="inherit" sx={{ bgcolor: 'rgba(255,255,255,.97)', borderBottom: '1px solid', borderColor: 'divider', backdropFilter: 'blur(12px)', boxShadow: '0 2px 12px rgba(7,86,168,.04)' }}>
        <Container maxWidth="xl">
          <Toolbar disableGutters sx={{ minHeight: { xs: 66, md: 76 }, gap: { xs: 1, md: 2 } }}>
            <Box component={Link} to={ROUTES.home} aria-label={env.brand.name} sx={{ flexShrink: 0 }}><BrandMark /></Box>
            {isDesktop && <Stack component="nav" direction="row" alignItems="center" gap={.15} aria-label="Điều hướng chính" sx={{ flexShrink: 0 }}>
              {navItems.map((item) => <Button key={item.to} component={Link} to={item.to} aria-current={isCurrent(item) ? 'page' : undefined} size="small" sx={{ minWidth: 0, px: 1, fontSize: 12.5, whiteSpace: 'nowrap', color: isCurrent(item) ? 'primary.main' : 'text.secondary', bgcolor: isCurrent(item) ? 'primary.light' : 'transparent' }}>{item.label}</Button>)}
            </Stack>}
            {isDesktop && <Box flex={1} minWidth={170} maxWidth={360} ml="auto">{searchField}</Box>}
            <Stack direction="row" alignItems="center" spacing={{ xs: 0, md: .5 }} ml="auto">
              {isDesktop && !isAdmin && <Tooltip title="Yêu thích"><IconButton component={Link} to={ROUTES.wishlist} aria-label="Danh sách yêu thích" sx={{ minWidth: 44, minHeight: 44 }}><FavoriteBorderRoundedIcon /></IconButton></Tooltip>}
              {!isAdmin && <Tooltip title="Giỏ hàng"><IconButton component={Link} to={ROUTES.cart} aria-label="Giỏ hàng" data-testid="header-cart-btn" sx={{ minWidth: 44, minHeight: 44 }}><Badge badgeContent={cartCount} color="primary" data-testid="header-cart-badge"><ShoppingCartOutlinedIcon /></Badge></IconButton></Tooltip>}
              {isDesktop && <Divider orientation="vertical" flexItem sx={{ my: 1.5, mx: .75 }} />}
              {isDesktop && (isAuthenticated ? <Button color="inherit" onClick={(event) => setAccountAnchor(event.currentTarget)} aria-label="Menu tài khoản" aria-controls={accountAnchor ? 'account-menu' : undefined} aria-haspopup="true" aria-expanded={Boolean(accountAnchor)} sx={{ px: 1, gap: 1 }}>
                <Avatar sx={{ width: 30, height: 30, bgcolor: 'primary.light', color: 'primary.main', fontSize: 13, fontWeight: 700 }}>{user?.fullName?.charAt(0)}</Avatar>
                <Typography component="span" variant="body2" fontWeight={600} noWrap maxWidth={110}>{user?.fullName?.split(' ').at(-1) || 'Tài khoản'}</Typography>
                <ExpandMoreRoundedIcon fontSize="small" />
              </Button> : <Button component={Link} to={ROUTES.login} startIcon={<PersonOutlineRoundedIcon />} sx={{ whiteSpace: 'nowrap' }}>Đăng nhập</Button>)}
              {!isDesktop && <IconButton aria-label="Mở menu" onClick={() => setDrawerOpen(true)} sx={{ minWidth: 44, minHeight: 44 }}><MenuRoundedIcon /></IconButton>}
            </Stack>
          </Toolbar>
          {!isDesktop && <Box pb={1.5}>{searchField}</Box>}
        </Container>

      </AppBar>

      <Menu id="account-menu" anchorEl={accountAnchor} open={Boolean(accountAnchor)} onClose={() => setAccountAnchor(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} transformOrigin={{ vertical: 'top', horizontal: 'right' }} slotProps={{ paper: { sx: { minWidth: 244, mt: 1, border: '1px solid', borderColor: 'divider' } } }}>
        <Box px={2} py={1.25}><Typography variant="body2" fontWeight={700}>{user?.fullName}</Typography><Typography variant="caption" color="text.secondary">{user?.email}</Typography></Box>
        <Divider />
        {accountLinks.map((item) => <MenuItem key={item.to} component={Link} to={item.to} onClick={closeMenus}><ListItemIcon>{item.icon}</ListItemIcon><ListItemText primary={item.label} /></MenuItem>)}
        <Divider />
        <MenuItem disabled={isLoggingOut} onClick={() => void handleLogout()} sx={{ color: 'error.main' }}><ListItemIcon><LogoutRoundedIcon color="error" /></ListItemIcon>{isLoggingOut ? 'Đang thoát...' : 'Đăng xuất'}</MenuItem>
      </Menu>

      <Drawer open={drawerOpen} onClose={closeMenus} slotProps={{ paper: { sx: { width: 'min(88vw, 340px)', borderRadius: 0 } } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" p={2} gap={1}><BrandMark compact /><IconButton aria-label="Đóng menu" onClick={closeMenus}><CloseRoundedIcon /></IconButton></Stack>
        <Divider />
        <List component="nav" aria-label="Điều hướng di động" sx={{ p: 1.5 }}>
          {navItems.map((item) => <ListItemButton key={item.to} component={Link} to={item.to} onClick={closeMenus} selected={isCurrent(item)} sx={{ borderRadius: '10px', mb: .5 }}><ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 600 }} /></ListItemButton>)}
          <Divider sx={{ my: 1.5 }} />
          {isAuthenticated ? <>
            <Typography variant="caption" color="text.secondary" px={2}>Xin chào, {user?.fullName}</Typography>
            {accountLinks.map((item) => <ListItemButton key={item.to} component={Link} to={item.to} onClick={closeMenus}><ListItemIcon>{item.icon}</ListItemIcon><ListItemText primary={item.label} /></ListItemButton>)}
            <ListItemButton disabled={isLoggingOut} onClick={() => void handleLogout()}><ListItemIcon><LogoutRoundedIcon /></ListItemIcon><ListItemText primary={isLoggingOut ? 'Đang đăng xuất...' : 'Đăng xuất'} /></ListItemButton>
          </> : <>
            <ListItemButton component={Link} to={ROUTES.login} onClick={closeMenus}><ListItemIcon><PersonOutlineRoundedIcon /></ListItemIcon><ListItemText primary="Đăng nhập" /></ListItemButton>
            <ListItemButton component={Link} to={ROUTES.register} onClick={closeMenus}><ListItemText primary="Tạo tài khoản" /></ListItemButton>
          </>}
        </List>
      </Drawer>

      <Box id="main-content" component="main" flex={1} minWidth={0} py={isHome ? 0 : { xs: 3, md: 4 }}>
        {isHome ? <Outlet /> : <Container maxWidth="xl"><Outlet /></Container>}
      </Box>

      <Box component="footer" bgcolor="secondary.main" color="white" mt="auto">
        <Container maxWidth="xl" sx={{ pt: { xs: 5, md: 6 }, pb: 2 }}>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', sm: '1fr 1fr', lg: '1.2fr .75fr .85fr 1.2fr' }} gap={{ xs: 4, md: 5 }}>
            <Stack spacing={2} alignItems="flex-start"><BrandMark inverse /><Typography variant="body2" color="#c4d4e9">{env.brand.industry} Khám phá sản phẩm và liên hệ cửa hàng để được tư vấn lựa chọn phù hợp.</Typography>{env.brand.optional.socialUrl && <Button component="a" href={env.brand.optional.socialUrl} sx={{ color: 'warning.main' }}>Kết nối với cửa hàng</Button>}</Stack>
            <Stack spacing={1.25} alignItems="flex-start"><Typography color="warning.main" fontWeight={700} variant="body2" mb={.5}>Khám phá</Typography>{[{ label: 'Tất cả sản phẩm', to: ROUTES.products }, { label: 'Sản phẩm mới', to: `${ROUTES.products}?sortBy=createdAt&sortDir=desc` }, { label: 'Khuyến mãi', to: '/#on-sale-section' }].map((item) => <Typography key={item.to} component={Link} to={item.to} variant="body2" sx={{ color: '#c4d4e9', '&:hover': { color: 'warning.main' } }}>{item.label}</Typography>)}</Stack>
            <Stack spacing={1.25} alignItems="flex-start"><Typography color="warning.main" fontWeight={700} variant="body2" mb={.5}>Hỗ trợ mua hàng</Typography><Typography component={Link} to={ROUTES.orders} variant="body2" color="#c4d4e9">Theo dõi đơn hàng</Typography><Typography component={Link} to={ROUTES.profile} variant="body2" color="#c4d4e9">Tài khoản của bạn</Typography>{env.brand.optional.warrantyPolicyUrl && <Typography component="a" href={env.brand.optional.warrantyPolicyUrl} variant="body2" color="#c4d4e9">Chính sách bảo hành</Typography>}{env.brand.optional.zaloUrl && <Typography component="a" href={env.brand.optional.zaloUrl} variant="body2" color="#c4d4e9">Tư vấn qua Zalo</Typography>}</Stack>
            <Stack spacing={1.5}><Typography color="warning.main" fontWeight={700} variant="body2">Liên hệ cửa hàng</Typography>{env.brand.address && <Stack direction="row" gap={1}><LocationOnOutlinedIcon sx={{ fontSize: 19, color: 'warning.main', mt: .3 }} /><Typography component="address" variant="body2" color="#c4d4e9" sx={{ fontStyle: 'normal' }}>{env.brand.address}</Typography></Stack>}<Stack direction="row" gap={1} alignItems="center"><PhoneOutlinedIcon sx={{ fontSize: 19, color: 'warning.main' }} /><Typography component="a" href={env.brand.contact.phoneHref} variant="body2" color="#c4d4e9">{env.brand.contact.phone}</Typography></Stack><Stack direction="row" gap={1} alignItems="center"><MailOutlineRoundedIcon sx={{ fontSize: 19, color: 'warning.main' }} /><Typography component="a" href={env.brand.contact.emailHref} variant="body2" color="#c4d4e9" sx={{ overflowWrap: 'anywhere' }}>{env.brand.contact.email}</Typography></Stack>{env.brand.optional.openingHours && <Box bgcolor="rgba(255,255,255,.05)" borderRadius="10px" p={1.5}><Typography variant="caption" color="#c4d4e9">Giờ mở cửa</Typography><Typography variant="body2">{env.brand.optional.openingHours}</Typography></Box>}{env.brand.optional.mapUrl && <Typography component="a" href={env.brand.optional.mapUrl} variant="body2" color="warning.main">Xem bản đồ</Typography>}</Stack>
          </Box>
          <Divider sx={{ mt: 5, mb: 2, borderColor: 'rgba(255,255,255,.12)' }} />
          <Typography variant="caption" color="#b7c9e0">© {new Date().getFullYear()} {env.brand.name}. Mọi quyền được bảo lưu.</Typography>
        </Container>
      </Box>
      <Snackbar open={Boolean(syncNotification)} autoHideDuration={4000} onClose={clearSyncNotification} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}><Alert onClose={clearSyncNotification} severity="success" variant="filled" data-testid="sync-cart-toast">{syncNotification}</Alert></Snackbar>
    </Box>
  )
}
