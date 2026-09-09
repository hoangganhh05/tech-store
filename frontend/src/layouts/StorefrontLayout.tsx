import {
  Alert,
  AppBar,
  Badge,
  Box,
  Button,
  Container,
  IconButton,
  InputAdornment,
  Snackbar,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from "@mui/material";
import ShoppingCartOutlinedIcon from "@mui/icons-material/ShoppingCartOutlined";
import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import ClearRoundedIcon from "@mui/icons-material/ClearRounded";
import { useState, useEffect } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { ROUTES } from "../constants/routes";
import { env } from "../configs/env";
import { useAuthEvents } from "../hooks/useAuthEvents";
import { useAuth } from "../hooks/useAuth";
import { useCart } from "../hooks/useCart";

const navItems = [
  { label: "Trang chủ", to: ROUTES.home },
  { label: "Sản phẩm", to: ROUTES.products },
];

export function StorefrontLayout() {
  useAuthEvents();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, user, signOut } = useAuth();
  const { cartCount, syncNotification, clearSyncNotification } = useCart();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState(
    searchParams.get("q") || "",
  );

  useEffect(() => {
    setSearchKeyword(searchParams.get("q") || "");
  }, [searchParams]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = searchKeyword.trim();
    if (trimmed) {
      navigate(`${ROUTES.products}?q=${encodeURIComponent(trimmed)}`);
    } else {
      navigate(ROUTES.products);
    }
  };

  const handleClearSearch = () => {
    setSearchKeyword("");
    if (searchParams.get("q")) {
      navigate(ROUTES.products);
    }
  };

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await signOut();
    setIsLoggingOut(false);
    navigate(ROUTES.home, { replace: true });
  };

  return (
    <Box minHeight="100vh" display="flex" flexDirection="column">
      <a className="skip-link" href="#main-content">
        Bỏ qua điều hướng
      </a>
      <AppBar
        position="sticky"
        color="inherit"
        sx={{ borderBottom: "1px solid #e4e7eb" }}
      >
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: 3 }}>
            <Typography
              component={Link}
              to={ROUTES.home}
              variant="h6"
              color="primary"
              fontWeight={800}
            >
              {env.appName}
            </Typography>
            <Stack
              component="nav"
              direction="row"
              spacing={0.5}
              flex={1}
              aria-label="Điều hướng chính"
            >
              {navItems.map((item) => (
                <Button
                  key={item.to}
                  component={NavLink}
                  to={item.to}
                  end={item.to === "/"}
                  color="inherit"
                  sx={{
                    "&.active": { color: "primary.main", bgcolor: "#fff1f1" },
                  }}
                >
                  {item.label}
                </Button>
              ))}
            </Stack>

            {/* Header Search Form */}
            <Box
              component="form"
              onSubmit={handleSearchSubmit}
              role="search"
              aria-label="Tìm kiếm sản phẩm"
              sx={{ width: { xs: 150, sm: 220, md: 280 } }}
            >
              <TextField
                size="small"
                fullWidth
                placeholder="Tìm sản phẩm..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                inputProps={{
                  "aria-label": "Từ khoá tìm kiếm",
                  "data-testid": "header-search-input",
                }}
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        {searchKeyword && (
                          <IconButton
                            size="small"
                            onClick={handleClearSearch}
                            aria-label="Xoá từ khoá tìm kiếm"
                            edge="end"
                            sx={{ mr: 0.25 }}
                          >
                            <ClearRoundedIcon fontSize="small" />
                          </IconButton>
                        )}
                        <IconButton
                          type="submit"
                          size="small"
                          aria-label="Tìm kiếm"
                          data-testid="header-search-button"
                          edge="end"
                          color="primary"
                        >
                          <SearchRoundedIcon fontSize="small" />
                        </IconButton>
                      </InputAdornment>
                    ),
                    sx: {
                      borderRadius: 3,
                      bgcolor: "#f8fafc",
                      pr: 1,
                      "&:hover": { bgcolor: "#f1f5f9" },
                    },
                  },
                }}
              />
            </Box>

            {isAuthenticated ? (
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="body2" color="text.secondary" noWrap>
                  Chào, {user?.fullName}
                </Typography>
                <Button component={Link} to={ROUTES.profile} color="inherit">
                  Tài khoản
                </Button>
                <Button component={Link} to={ROUTES.orders} color="inherit">
                  Đơn hàng
                </Button>
                <Button
                  color="inherit"
                  onClick={handleLogout}
                  disabled={isLoggingOut}
                >
                  {isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}
                </Button>
              </Stack>
            ) : (
              <>
                <Button component={Link} to={ROUTES.register} color="inherit">
                  Đăng ký
                </Button>
                <Button component={Link} to={ROUTES.login} color="inherit">
                  Đăng nhập
                </Button>
              </>
            )}
            <IconButton
              component={Link}
              to={ROUTES.cart}
              aria-label="Giỏ hàng"
              data-testid="header-cart-btn"
            >
              <Badge
                badgeContent={cartCount}
                color="primary"
                data-testid="header-cart-badge"
              >
                <ShoppingCartOutlinedIcon />
              </Badge>
            </IconButton>
          </Toolbar>
        </Container>
      </AppBar>
      <Box id="main-content" component="main" flex={1} py={{ xs: 3, md: 5 }}>
        <Container maxWidth="lg">
          <Outlet />
        </Container>
      </Box>
      <Box component="footer" bgcolor="#263238" color="white" py={3}>
        <Container maxWidth="lg">
          <Typography fontWeight={700}>{env.appName}</Typography>
          <Typography variant="body2" color="#cfd8dc">
            Điện thoại và phụ kiện công nghệ.
          </Typography>
        </Container>
      </Box>
      <Snackbar
        open={Boolean(syncNotification)}
        autoHideDuration={4000}
        onClose={clearSyncNotification}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert
          onClose={clearSyncNotification}
          severity="success"
          variant="filled"
          sx={{ width: "100%" }}
          data-testid="sync-cart-toast"
        >
          {syncNotification}
        </Alert>
      </Snackbar>
    </Box>
  );
}
