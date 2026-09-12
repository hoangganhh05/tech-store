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
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import DashboardOutlinedIcon from "@mui/icons-material/DashboardOutlined";
import DiscountOutlinedIcon from "@mui/icons-material/DiscountOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import MenuRoundedIcon from "@mui/icons-material/MenuRounded";
import PeopleOutlineRoundedIcon from "@mui/icons-material/PeopleOutlineRounded";
import RateReviewOutlinedIcon from "@mui/icons-material/RateReviewOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import WarehouseOutlinedIcon from "@mui/icons-material/WarehouseOutlined";
import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { ROUTES } from "../constants/routes";
import { env } from "../configs/env";
import { DocumentMetadata } from "../components/common/DocumentMetadata";
import { useAuthEvents } from "../hooks/useAuthEvents";
import { useAuth } from "../hooks/useAuth";
import { AdminNotificationBell } from "../modules/admin/components/AdminNotificationBell";

const drawerWidth = 276;
const adminItems = [
  { label: "Tổng quan", to: ROUTES.admin, icon: <DashboardOutlinedIcon /> },
  { label: "Người dùng", to: ROUTES.adminUsers, icon: <PeopleOutlineRoundedIcon /> },
  { label: "Danh mục", to: ROUTES.adminCategories, icon: <CategoryOutlinedIcon /> },
  { label: "Thương hiệu", to: ROUTES.adminBrands, icon: <StorefrontOutlinedIcon /> },
  { label: "Sản phẩm", to: ROUTES.adminProducts, icon: <Inventory2OutlinedIcon /> },
  { label: "Tồn kho", to: ROUTES.adminInventory, icon: <WarehouseOutlinedIcon /> },
  { label: "Đơn hàng", to: ROUTES.adminOrders, icon: <ReceiptLongOutlinedIcon /> },
  { label: "Đánh giá", to: ROUTES.adminReviews, icon: <RateReviewOutlinedIcon /> },
  { label: "Mã giảm giá", to: ROUTES.adminVouchers, icon: <DiscountOutlinedIcon /> },
  { label: "Khuyến mãi", to: ROUTES.adminPromotions, icon: <LocalOfferOutlinedIcon /> },
  { label: "Báo cáo doanh thu", to: ROUTES.adminRevenueReport, icon: <AssessmentOutlinedIcon /> },
  { label: "Báo cáo sản phẩm", to: ROUTES.adminProductInventoryReport, icon: <AssessmentOutlinedIcon /> },
];

export function AdminLayout() {
  useAuthEvents();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const { signOut, user } = useAuth();
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
        color="inherit"
        sx={{ zIndex: (theme) => theme.zIndex.drawer + 1, borderBottom: "1px solid", borderColor: "divider" }}
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
            <Box component="span" color="primary.main">{env.brand.name}</Box>
            <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>
              {" "}— Quản trị
            </Box>
          </Typography>
          <AdminNotificationBell />
          <Box sx={{ display: { xs: "none", sm: "block" }, textAlign: "right", mr: 1 }}>
            <Typography variant="body2" fontWeight={700} lineHeight={1.2}>{user?.fullName}</Typography>
            <Typography variant="caption" color="text.secondary">Quản trị viên</Typography>
          </Box>
          <Button
            component={Link}
            to={ROUTES.home}
            color="secondary"
            sx={{ display: { xs: "none", md: "inline-flex" } }}
          >
            Về cửa hàng
          </Button>
          <Button
            color="secondary"
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
          "& .MuiDrawer-paper": {
            width: drawerWidth,
            pt: isMobile ? 1 : 9,
            bgcolor: "secondary.main",
            color: "#cbd5e1",
            borderRight: 0,
          },
        }}
      >
        <Box px={2.5} pt={2} pb={1}>
          <Typography variant="overline" color="#64748b" fontWeight={800} letterSpacing={1.2}>Không gian quản trị</Typography>
        </Box>
        <List component="nav" aria-label="Điều hướng quản trị" sx={{ px: 1.5 }}>
          {adminItems.map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              end={item.to === ROUTES.admin}
              onClick={() => {
                if (isMobile) setMobileDrawerOpen(false);
              }}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                minHeight: 44,
                "&:hover": { bgcolor: "rgba(255,255,255,.07)", color: "white" },
                "&.active": { bgcolor: "primary.main", color: "white" },
                "&.active .MuiListItemIcon-root": { color: "white" },
              }}
            >
              <ListItemIcon sx={{ color: "#94a3b8", minWidth: 40 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
          {isMobile && (
            <>
              <Divider sx={{ my: 1, borderColor: "rgba(255,255,255,.12)" }} />
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
        pt={{ xs: 10, md: 12 }}
        pb={5}
      >
        <Container maxWidth="xl">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
}
