import {
  Alert, AppBar, Badge, Box, Button, Container, Divider, Drawer, IconButton,
  InputAdornment, List, ListItemButton, ListItemIcon, ListItemText, Snackbar,
  Stack, TextField, Toolbar, Tooltip, Typography, useMediaQuery, useTheme,
} from "@mui/material";
import AdminPanelSettingsRoundedIcon from "@mui/icons-material/AdminPanelSettingsRounded";
import ClearRoundedIcon from "@mui/icons-material/ClearRounded";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import FavoriteBorderRoundedIcon from "@mui/icons-material/FavoriteBorderRounded";
import LocalShippingOutlinedIcon from "@mui/icons-material/LocalShippingOutlined";
import MenuRoundedIcon from "@mui/icons-material/MenuRounded";
import PersonOutlineRoundedIcon from "@mui/icons-material/PersonOutlineRounded";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import ShoppingCartOutlinedIcon from "@mui/icons-material/ShoppingCartOutlined";
import VerifiedUserOutlinedIcon from "@mui/icons-material/VerifiedUserOutlined";
import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useNavigate, useSearchParams } from "react-router-dom";
import { DocumentMetadata } from "../components/common/DocumentMetadata";
import { env } from "../configs/env";
import { ROUTES } from "../constants/routes";
import { useAuth } from "../hooks/useAuth";
import { useAuthEvents } from "../hooks/useAuthEvents";
import { useCart } from "../hooks/useCart";

const navItems = [
  { label: "Trang chủ", to: ROUTES.home },
  { label: "Sản phẩm", to: ROUTES.products },
];

export function StorefrontLayout() {
  useAuthEvents();
  const navigate = useNavigate();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up("md"));
  const [searchParams] = useSearchParams();
  const { isAuthenticated, user, signOut } = useAuth();
  const { cartCount, syncNotification, clearSyncNotification } = useCart();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState(searchParams.get("q") || "");
  const isAdmin = Boolean(user?.roles?.includes("ADMIN"));

  useEffect(() => setSearchKeyword(searchParams.get("q") || ""), [searchParams]);

  const handleSearchSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const keyword = searchKeyword.trim();
    navigate(keyword ? `${ROUTES.products}?q=${encodeURIComponent(keyword)}` : ROUTES.products);
    setDrawerOpen(false);
  };

  const handleClearSearch = () => {
    setSearchKeyword("");
    if (searchParams.get("q")) navigate(ROUTES.products);
  };

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await signOut();
    setIsLoggingOut(false);
    setDrawerOpen(false);
    navigate(ROUTES.home, { replace: true });
  };

  const searchField = (
    <Box component="form" onSubmit={handleSearchSubmit} role="search" aria-label="Tìm kiếm sản phẩm" width="100%">
      <TextField
        size="small" fullWidth placeholder="Tìm sản phẩm..." value={searchKeyword}
        onChange={(event) => setSearchKeyword(event.target.value)}
        inputProps={{ "aria-label": "Từ khoá tìm kiếm", "data-testid": "header-search-input" }}
        slotProps={{ input: {
          startAdornment: <InputAdornment position="start"><SearchRoundedIcon color="action" /></InputAdornment>,
          endAdornment: <InputAdornment position="end">
            {searchKeyword && <IconButton size="small" onClick={handleClearSearch} aria-label="Xoá từ khoá tìm kiếm"><ClearRoundedIcon fontSize="small" /></IconButton>}
            <Button type="submit" size="small" variant="contained" data-testid="header-search-button" sx={{ minWidth: 44, px: 1.25 }}>Tìm</Button>
          </InputAdornment>,
          sx: { bgcolor: "background.default", pr: 0.5, "& fieldset": { borderColor: "divider" } },
        } }}
      />
    </Box>
  );

  return (
    <Box minHeight="100vh" display="flex" flexDirection="column">
      <DocumentMetadata />
      <a className="skip-link" href="#main-content">Bỏ qua điều hướng</a>

      <Box bgcolor="secondary.main" color="white" sx={{ display: { xs: "none", md: "block" } }}>
        <Container maxWidth="xl">
          <Stack direction="row" justifyContent="space-between" alignItems="center" minHeight={34}>
            <Stack direction="row" spacing={3}>
              <Typography variant="caption" display="flex" alignItems="center" gap={0.75}><LocalShippingOutlinedIcon sx={{ fontSize: 16 }} /> Giao hàng toàn quốc</Typography>
              <Typography variant="caption" display="flex" alignItems="center" gap={0.75}><VerifiedUserOutlinedIcon sx={{ fontSize: 16 }} /> Mua sắm an tâm</Typography>
            </Stack>
            <Typography variant="caption">Hỗ trợ: {env.brand.contact.phone}</Typography>
          </Stack>
        </Container>
      </Box>

      <AppBar position="sticky" color="inherit" sx={{ borderBottom: "1px solid", borderColor: "divider", bgcolor: "rgba(255,255,255,.96)", backdropFilter: "blur(12px)" }}>
        <Container maxWidth="xl">
          <Toolbar disableGutters sx={{ minHeight: { xs: 68, md: 76 }, gap: { xs: 1, md: 3 } }}>
            {!isDesktop && <IconButton aria-label="Mở menu" onClick={() => setDrawerOpen(true)} edge="start"><MenuRoundedIcon /></IconButton>}
            <Typography component={Link} to={ROUTES.home} color="secondary.main" fontWeight={900} sx={{ fontSize: { xs: "1.15rem", sm: "1.35rem", md: "1.55rem" }, whiteSpace: "nowrap", letterSpacing: "-0.04em" }}>
              <Box component="span" color="primary.main">Đăng Tùng</Box> <Box component="span" color="warning.dark">Mobile</Box>
            </Typography>
            {isDesktop && <Box flex={1} maxWidth={620}>{searchField}</Box>}

            <Stack direction="row" alignItems="center" spacing={{ xs: 0, sm: 0.5 }} ml="auto">
              {isDesktop && isAdmin && <Button component={Link} to={ROUTES.admin} variant="contained" color="secondary" startIcon={<AdminPanelSettingsRoundedIcon />}>Trang quản trị</Button>}
              {isDesktop && !isAdmin && isAuthenticated && <>
                <Tooltip title="Yêu thích"><IconButton component={Link} to={ROUTES.wishlist} aria-label="Danh sách yêu thích"><FavoriteBorderRoundedIcon /></IconButton></Tooltip>
                <Tooltip title="Đơn hàng"><IconButton component={Link} to={ROUTES.orders} aria-label="Đơn hàng"><ReceiptLongOutlinedIcon /></IconButton></Tooltip>
                <Button component={Link} to={ROUTES.profile} color="inherit" startIcon={<PersonOutlineRoundedIcon />}>Tài khoản</Button>
              </>}
              {isDesktop && !isAuthenticated && <>
                <Button component={Link} to={ROUTES.login} color="inherit">Đăng nhập</Button>
                <Button component={Link} to={ROUTES.register} variant="outlined">Đăng ký</Button>
              </>}
              {!isAdmin && <Tooltip title="Giỏ hàng"><IconButton component={Link} to={ROUTES.cart} aria-label="Giỏ hàng" data-testid="header-cart-btn"><Badge badgeContent={cartCount} color="primary" data-testid="header-cart-badge"><ShoppingCartOutlinedIcon /></Badge></IconButton></Tooltip>}
            </Stack>
          </Toolbar>
          {!isDesktop && <Box pb={1.5}>{searchField}</Box>}
        </Container>
        {isDesktop && <Box borderTop="1px solid" borderColor="divider">
          <Container maxWidth="xl">
            <Stack component="nav" direction="row" spacing={0.5} aria-label="Điều hướng chính" minHeight={48} alignItems="center">
              {navItems.map((item) => <Button key={item.to} component={NavLink} to={item.to} end={item.to === ROUTES.home} color="inherit" sx={{ height: 48, borderRadius: 0, px: 2, "&.active": { color: "primary.main", borderBottom: "2px solid", borderColor: "primary.main" } }}>{item.label}</Button>)}
              <Box flex={1} />
              {isAuthenticated && <Typography variant="body2" color="text.secondary" noWrap maxWidth={220}>Xin chào, {user?.fullName}</Typography>}
              {isAuthenticated && <Button color="inherit" size="small" onClick={() => void handleLogout()} disabled={isLoggingOut}>{isLoggingOut ? "Đang thoát..." : "Đăng xuất"}</Button>}
            </Stack>
          </Container>
        </Box>}
      </AppBar>

      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} PaperProps={{ sx: { width: "min(86vw, 340px)" } }}>
        <Box p={2} display="flex" alignItems="center" justifyContent="space-between">
          <Typography fontWeight={900} fontSize="1.2rem"><Box component="span" color="primary.main">Đăng Tùng</Box> <Box component="span" color="warning.dark">Mobile</Box></Typography>
          <IconButton onClick={() => setDrawerOpen(false)} aria-label="Đóng menu"><CloseRoundedIcon /></IconButton>
        </Box>
        <Divider />
        <List component="nav" sx={{ p: 1.5 }}>
          {navItems.map((item) => <ListItemButton key={item.to} component={NavLink} to={item.to} end={item.to === ROUTES.home} onClick={() => setDrawerOpen(false)} sx={{ borderRadius: 2, mb: 0.5, "&.active": { bgcolor: "primary.light", color: "primary.dark" } }}><ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 700 }} /></ListItemButton>)}
          <Divider sx={{ my: 1.5 }} />
          {isAdmin ? <ListItemButton component={Link} to={ROUTES.admin} onClick={() => setDrawerOpen(false)} sx={{ borderRadius: 2 }}><ListItemIcon><AdminPanelSettingsRoundedIcon /></ListItemIcon><ListItemText primary="Trang quản trị" /></ListItemButton>
            : isAuthenticated ? <>
              <ListItemButton component={Link} to={ROUTES.profile} onClick={() => setDrawerOpen(false)}><ListItemIcon><PersonOutlineRoundedIcon /></ListItemIcon><ListItemText primary="Tài khoản" /></ListItemButton>
              <ListItemButton component={Link} to={ROUTES.orders} onClick={() => setDrawerOpen(false)}><ListItemIcon><ReceiptLongOutlinedIcon /></ListItemIcon><ListItemText primary="Đơn hàng" /></ListItemButton>
              <ListItemButton component={Link} to={ROUTES.wishlist} onClick={() => setDrawerOpen(false)}><ListItemIcon><FavoriteBorderRoundedIcon /></ListItemIcon><ListItemText primary="Yêu thích" /></ListItemButton>
            </> : <>
              <ListItemButton component={Link} to={ROUTES.login} onClick={() => setDrawerOpen(false)}><ListItemText primary="Đăng nhập" /></ListItemButton>
              <ListItemButton component={Link} to={ROUTES.register} onClick={() => setDrawerOpen(false)}><ListItemText primary="Tạo tài khoản" /></ListItemButton>
            </>}
          {isAuthenticated && <ListItemButton onClick={() => void handleLogout()} disabled={isLoggingOut}><ListItemText primary={isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"} /></ListItemButton>}
        </List>
      </Drawer>

      <Box id="main-content" component="main" flex={1} py={{ xs: 2.5, md: 4 }}><Container maxWidth="xl"><Outlet /></Container></Box>
      <Box component="footer" bgcolor="secondary.main" color="white" py={{ xs: 4, md: 5 }} mt="auto">
        <Container maxWidth="xl">
          <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={3}>
            <Box maxWidth={460}><Typography component="p" variant="h6" fontWeight={900} mb={1} color="warning.main">{env.brand.name}</Typography><Typography variant="body2" color="#cbd5e1" lineHeight={1.7}>{env.brand.industry} Sản phẩm rõ nguồn gốc, tư vấn tận tâm và hỗ trợ nhanh chóng.</Typography></Box>
            <Stack spacing={0.75} minWidth={{ md: 300 }}><Typography fontWeight={800}>Liên hệ cửa hàng</Typography>{env.brand.address && <Typography component="address" variant="body2" color="#cbd5e1" sx={{ fontStyle: "normal" }}>{env.brand.address}</Typography>}<Typography component="a" href={env.brand.contact.phoneHref} variant="body2" color="#cbd5e1">{env.brand.contact.phone}</Typography><Typography component="a" href={env.brand.contact.emailHref} variant="body2" color="#cbd5e1">{env.brand.contact.email}</Typography></Stack>
          </Stack>
          <Divider sx={{ my: 3, borderColor: "rgba(255,255,255,.12)" }} /><Typography variant="caption" color="#94a3b8">© {new Date().getFullYear()} {env.brand.name}. Mọi quyền được bảo lưu.</Typography>
        </Container>
      </Box>
      <Snackbar open={Boolean(syncNotification)} autoHideDuration={4000} onClose={clearSyncNotification} anchorOrigin={{ vertical: "top", horizontal: "center" }}><Alert onClose={clearSyncNotification} severity="success" variant="filled" data-testid="sync-cart-toast">{syncNotification}</Alert></Snackbar>
    </Box>
  );
}
