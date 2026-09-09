import {
  AppBar,
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import MenuRoundedIcon from "@mui/icons-material/MenuRounded";
import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { ROUTES } from "../constants/routes";
import { env } from "../configs/env";
import { DocumentMetadata } from "../components/common/DocumentMetadata";
import { useAuthEvents } from "../hooks/useAuthEvents";
import { useAuth } from "../hooks/useAuth";
import { AdminNotificationBell } from "../modules/admin/components/AdminNotificationBell";

const drawerWidth = 240;
const adminItems = [
  { label: "Tổng quan", to: ROUTES.admin },
  { label: "Người dùng", to: ROUTES.adminUsers },
  { label: "Danh mục", to: ROUTES.adminCategories },
  { label: "Thương hiệu sản phẩm", to: ROUTES.adminBrands },
  { label: "Sản phẩm", to: ROUTES.adminProducts },
  { label: "Tồn kho", to: ROUTES.adminInventory },
  { label: "Đơn hàng", to: ROUTES.adminOrders },
];

export function AdminLayout() {
  useAuthEvents();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const { signOut } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    await signOut();
    setIsLoggingOut(false);
    setMobileDrawerOpen(false);
    navigate(ROUTES.home, { replace: true });
  };

  return (
    <Box minHeight="100vh" bgcolor="background.default">
      <DocumentMetadata section="Quản trị" />
      <AppBar
        position="fixed"
        sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}
      >
        <Toolbar sx={{ gap: { xs: 0.5, sm: 1 } }}>
          {isMobile && (
            <IconButton
              color="inherit"
              edge="start"
              aria-label="Mở điều hướng quản trị"
              onClick={() => setMobileDrawerOpen(true)}
            >
              <MenuRoundedIcon />
            </IconButton>
          )}
          <Typography
            variant="h6"
            fontWeight={800}
            flex={1}
            minWidth={0}
            sx={{
              fontSize: { xs: "1rem", sm: "1.25rem" },
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {env.brand.name}
            <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>
              {" "}— Quản trị
            </Box>
          </Typography>
          <AdminNotificationBell />
          <Button
            component={Link}
            to={ROUTES.home}
            color="inherit"
            sx={{ display: { xs: "none", md: "inline-flex" } }}
          >
            Về cửa hàng
          </Button>
          <Button
            color="inherit"
            onClick={handleLogout}
            disabled={isLoggingOut}
            sx={{ display: { xs: "none", md: "inline-flex" } }}
          >
            {isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}
          </Button>
        </Toolbar>
      </AppBar>
      <Drawer
        variant={isMobile ? "temporary" : "permanent"}
        open={isMobile ? mobileDrawerOpen : true}
        onClose={() => setMobileDrawerOpen(false)}
        sx={{
          width: drawerWidth,
          "& .MuiDrawer-paper": { width: drawerWidth, pt: isMobile ? 1 : 8 },
        }}
      >
        <List component="nav" aria-label="Điều hướng quản trị">
          {adminItems.map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              end={item.to === ROUTES.admin}
              onClick={() => {
                if (isMobile) setMobileDrawerOpen(false);
              }}
              sx={{ "&.active": { bgcolor: "#ffebee", color: "primary.main" } }}
            >
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
          {isMobile && (
            <>
              <Divider sx={{ my: 1 }} />
              <ListItemButton
                component={Link}
                to={ROUTES.home}
                onClick={() => setMobileDrawerOpen(false)}
              >
                <ListItemText primary="Về cửa hàng" />
              </ListItemButton>
              <ListItemButton onClick={handleLogout} disabled={isLoggingOut}>
                <ListItemText
                  primary={isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}
                />
              </ListItemButton>
            </>
          )}
        </List>
      </Drawer>
      <Box
        component="main"
        ml={{ xs: 0, md: `${drawerWidth}px` }}
        pt={{ xs: 10, md: 11 }}
        pb={5}
      >
        <Container maxWidth="lg">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
}
