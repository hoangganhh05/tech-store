import {
  AppBar, Avatar, Box, Button, Container, Divider, Drawer, IconButton, List,
  ListItemButton, ListItemIcon, ListItemText, Stack, Toolbar, Tooltip,
  Typography, useMediaQuery, useTheme,
} from "@mui/material";
import AssessmentOutlinedIcon from "@mui/icons-material/AssessmentOutlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";
import ChevronLeftRoundedIcon from "@mui/icons-material/ChevronLeftRounded";
import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import DashboardOutlinedIcon from "@mui/icons-material/DashboardOutlined";
import DiscountOutlinedIcon from "@mui/icons-material/DiscountOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import LocalOfferOutlinedIcon from "@mui/icons-material/LocalOfferOutlined";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import MenuRoundedIcon from "@mui/icons-material/MenuRounded";
import OpenInNewRoundedIcon from "@mui/icons-material/OpenInNewRounded";
import PeopleOutlineRoundedIcon from "@mui/icons-material/PeopleOutlineRounded";
import RateReviewOutlinedIcon from "@mui/icons-material/RateReviewOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import WarehouseOutlinedIcon from "@mui/icons-material/WarehouseOutlined";
import { useState, type ReactNode } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ROUTES } from "../constants/routes";
import { env } from "../configs/env";
import { DocumentMetadata } from "../components/common/DocumentMetadata";
import { useAuthEvents } from "../hooks/useAuthEvents";
import { useAuth } from "../hooks/useAuth";
import { AdminNotificationBell } from "../modules/admin/components/AdminNotificationBell";

type AdminNavItem = {
  label: string;
  to: string;
  icon: ReactNode;
};

const adminGroups: { label: string; items: AdminNavItem[] }[] = [
  { label: "Tổng quan", items: [
    { label: "Tổng quan", to: ROUTES.admin, icon: <DashboardOutlinedIcon /> },
  ] },
  { label: "Sản phẩm", items: [
    { label: "Sản phẩm", to: ROUTES.adminProducts, icon: <Inventory2OutlinedIcon /> },
    { label: "Danh mục", to: ROUTES.adminCategories, icon: <CategoryOutlinedIcon /> },
    { label: "Thương hiệu", to: ROUTES.adminBrands, icon: <StorefrontOutlinedIcon /> },
    { label: "Tồn kho", to: ROUTES.adminInventory, icon: <WarehouseOutlinedIcon /> },
  ] },
  { label: "Bán hàng", items: [
    { label: "Đơn hàng", to: ROUTES.adminOrders, icon: <ReceiptLongOutlinedIcon /> },
    { label: "Người dùng", to: ROUTES.adminUsers, icon: <PeopleOutlineRoundedIcon /> },
    { label: "Đánh giá", to: ROUTES.adminReviews, icon: <RateReviewOutlinedIcon /> },
  ] },
  { label: "Marketing", items: [
    { label: "Mã giảm giá", to: ROUTES.adminVouchers, icon: <DiscountOutlinedIcon /> },
    { label: "Khuyến mãi", to: ROUTES.adminPromotions, icon: <LocalOfferOutlinedIcon /> },
  ] },
  { label: "Báo cáo", items: [
    { label: "Báo cáo doanh thu", to: ROUTES.adminRevenueReport, icon: <AssessmentOutlinedIcon /> },
    { label: "Báo cáo sản phẩm", to: ROUTES.adminProductInventoryReport, icon: <AssessmentOutlinedIcon /> },
  ] },
];

export function AdminLayout() {
  useAuthEvents();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const { signOut, user } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const compact = collapsed && !isMobile;
  const drawerWidth = compact ? 76 : 248;
  const currentPage = adminGroups.flatMap((group) => group.items).find((item) =>
    item.to === ROUTES.admin ? location.pathname === item.to : location.pathname.startsWith(item.to),
  );
  const brandInitials = env.brand.name.split(/\s+/).filter(Boolean).slice(-2).map((part) => part[0]).join("");

  const handleLogout = async () => {
    setIsLoggingOut(true);
    try {
      await signOut();
      setMobileDrawerOpen(false);
      navigate(ROUTES.home, { replace: true });
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <Box minHeight="100vh" bgcolor="background.default">
      <DocumentMetadata section="Quản trị" />
      <AppBar position="fixed" color="inherit" elevation={0} sx={{
        width: { md: `calc(100% - ${drawerWidth}px)` }, ml: { md: `${drawerWidth}px` },
        borderBottom: "1px solid", borderColor: "divider", bgcolor: "background.paper",
        transition: "width 180ms ease, margin 180ms ease",
      }}>
        <Toolbar sx={{ gap: 1.5, minHeight: { xs: 64, sm: 64 }, px: { xs: 2, md: 3 } }}>
          {isMobile && <IconButton color="inherit" edge="start" aria-label="Mở điều hướng quản trị" onClick={() => setMobileDrawerOpen(true)}><MenuRoundedIcon /></IconButton>}
          <Box flex={1} minWidth={0}>
            <Typography variant="body2" fontWeight={700} noWrap>{currentPage?.label || "Quản trị"}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap sx={{ display: { xs: "none", sm: "block" } }}>{env.brand.name}</Typography>
          </Box>
          <AdminNotificationBell />
          <Divider orientation="vertical" flexItem sx={{ my: 2 }} />
          <Avatar sx={{ width: 34, height: 34, bgcolor: "primary.main", fontSize: 14, fontWeight: 700 }}>{user?.fullName?.charAt(0) || "A"}</Avatar>
          <Box sx={{ display: { xs: "none", sm: "block" } }}>
            <Typography variant="body2" fontWeight={700} lineHeight={1.4}>{user?.fullName}</Typography>
            <Typography variant="caption" color="text.secondary">Quản trị viên</Typography>
          </Box>
        </Toolbar>
      </AppBar>
      <Drawer variant={isMobile ? "temporary" : "permanent"} open={isMobile ? mobileDrawerOpen : true} onClose={() => setMobileDrawerOpen(false)} sx={{
        width: drawerWidth,
        "& .MuiDrawer-paper": { width: drawerWidth, bgcolor: "#062E63", color: "white", borderRight: 0, transition: "width 180ms ease", overflowX: "hidden" },
      }}>
        <Stack direction="row" alignItems="center" gap={1.25} sx={{ minHeight: 64, px: compact ? 1.5 : 2, borderBottom: "1px solid rgba(255,255,255,.12)" }}>
          <Box component={Link} to={ROUTES.admin} aria-label={`${env.brand.name} — Quản trị`} sx={{ width: 34, height: 34, borderRadius: "10px", bgcolor: "#F2B705", color: "#062E63", display: "grid", placeItems: "center", fontSize: 12, fontWeight: 900, textDecoration: "none", flexShrink: 0 }}>{brandInitials}</Box>
          {!compact && <Box minWidth={0} flex={1}><Typography variant="body2" fontWeight={800} noWrap>{env.brand.name}</Typography><Typography variant="caption" sx={{ color: "rgba(255,255,255,.6)" }}>Không gian quản trị</Typography></Box>}
          {isMobile && <IconButton aria-label="Đóng điều hướng quản trị" onClick={() => setMobileDrawerOpen(false)} sx={{ color: "white" }}><CloseRoundedIcon fontSize="small" /></IconButton>}
        </Stack>
        <List component="nav" aria-label="Điều hướng quản trị" sx={{ flex: 1, overflowY: "auto", px: 1, py: 1.5 }}>
          {adminGroups.map((group) => <Box key={group.label} mb={1}>
            {!compact && <Typography sx={{ px: 1.5, pt: 1.25, pb: 0.5, color: "rgba(255,255,255,.5)", fontSize: 10, fontWeight: 700, textTransform: "uppercase", letterSpacing: 1 }}>{group.label}</Typography>}
            {group.items.map((item) => <Tooltip key={item.to} title={compact ? item.label : ""} placement="right">
              <ListItemButton component={NavLink} to={item.to} end={item.to === ROUTES.admin} onClick={() => { if (isMobile) setMobileDrawerOpen(false); }} aria-label={item.label} sx={{
                borderRadius: "8px", minHeight: 42, px: compact ? 2.25 : 1.5, py: 0.75, my: 0.25, color: "rgba(255,255,255,.74)",
                "&:hover": { bgcolor: "rgba(255,255,255,.08)", color: "white" },
                "&.active": { bgcolor: "#0756A8", color: "white", boxShadow: "inset 3px 0 0 #F2B705" },
              }}>
                <ListItemIcon sx={{ color: "inherit", minWidth: compact ? 0 : 34, "& svg": { fontSize: 19 } }}>{item.icon}</ListItemIcon>
                {!compact && <ListItemText primary={item.label} slotProps={{ primary: { fontSize: 13, fontWeight: 500 } }} />}
              </ListItemButton>
            </Tooltip>)}
          </Box>)}
        </List>
        <Stack spacing={0.5} sx={{ px: 1.5, py: 1.5, borderTop: "1px solid rgba(255,255,255,.12)" }}>
          <Tooltip title={compact ? "Về cửa hàng" : ""} placement="right"><Button component={Link} to={ROUTES.home} aria-label="Về cửa hàng" onClick={() => setMobileDrawerOpen(false)} startIcon={!compact && <OpenInNewRoundedIcon fontSize="small" />} sx={{ color: "rgba(255,255,255,.72)", justifyContent: compact ? "center" : "flex-start", minWidth: 0, px: 1.5 }}>{compact ? <OpenInNewRoundedIcon fontSize="small" /> : "Về cửa hàng"}</Button></Tooltip>
          <Tooltip title={compact ? "Đăng xuất" : ""} placement="right"><Button aria-label="Đăng xuất" onClick={handleLogout} disabled={isLoggingOut} startIcon={!compact && <LogoutRoundedIcon fontSize="small" />} sx={{ color: "#FCA5A5", justifyContent: compact ? "center" : "flex-start", minWidth: 0, px: 1.5 }}>{compact ? <LogoutRoundedIcon fontSize="small" /> : isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}</Button></Tooltip>
          {!isMobile && <Button onClick={() => setCollapsed(!collapsed)} aria-label={compact ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"} sx={{ color: "rgba(255,255,255,.5)", minWidth: 0 }}>{compact ? <ChevronRightRoundedIcon /> : <><ChevronLeftRoundedIcon /><Typography variant="caption">Thu gọn</Typography></>}</Button>}
        </Stack>
      </Drawer>
      <Box component="main" ml={{ xs: 0, md: `${drawerWidth}px` }} pt="88px" pb={4} sx={{
        transition: "margin 180ms ease", minWidth: 0,
        "& .MuiTableHead-root .MuiTableCell-root": { bgcolor: "#F7F9FC", color: "text.secondary", fontSize: 11, fontWeight: 700, textTransform: "uppercase", letterSpacing: 0.4, py: 1.5 },
        "& .MuiTableBody-root .MuiTableCell-root": { fontSize: 13, py: 1.5 },
        "& .MuiCard-root": { boxShadow: "none", border: "1px solid", borderColor: "divider" },
        "& .MuiTableContainer-root": { boxShadow: "none" },
      }}><Container maxWidth="xl" sx={{ px: { xs: 2, md: 3 } }}><Outlet /></Container></Box>
    </Box>
  );
}
