import NotificationsIcon from "@mui/icons-material/Notifications";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import {
  Badge,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Popover,
  Stack,
  Typography,
} from "@mui/material";
import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAdminNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  type AdminNotification,
} from "../../../services/notificationService";

export function AdminNotificationBell() {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [notifications, setNotifications] = useState<AdminNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);

  const open = Boolean(anchorEl);

  const fetchNotifications = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getAdminNotifications(0, 10);
      setNotifications(data.items || []);
      setUnreadCount(data.unreadCount || 0);
    } catch {
      // Bỏ qua lỗi polling ngầm
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchNotifications();
    // Poll mỗi 30 giây để cập nhật số lượng thông báo mới
    const interval = setInterval(() => {
      void fetchNotifications();
    }, 30000);
    return () => clearInterval(interval);
  }, [fetchNotifications]);

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
    void fetchNotifications();
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleItemClick = async (item: AdminNotification) => {
    if (!item.isRead) {
      try {
        await markNotificationAsRead(item.id);
        setNotifications((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n)),
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch {
        // Tiếp tục điều hướng kể cả khi API mark as read lỗi
      }
    }
    handleClose();
    if (item.orderId) {
      navigate(`/admin/orders/${item.orderId}`);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      setMarkingAll(true);
      await markAllNotificationsAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch {
      // Lỗi đánh dấu tất cả
    } finally {
      setMarkingAll(false);
    }
  };

  const formatTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return (
        date.toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }) +
        " " +
        date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" })
      );
    } catch {
      return isoString;
    }
  };

  return (
    <>
      <IconButton
        color="inherit"
        onClick={handleClick}
        aria-label="Thông báo quản trị"
        sx={{ mr: 1 }}
      >
        <Badge badgeContent={unreadCount} color="error" max={99}>
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
        transformOrigin={{ vertical: "top", horizontal: "right" }}
        PaperProps={{
          sx: {
            width: 360,
            maxHeight: 480,
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
          },
        }}
      >
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
          sx={{ px: 2, py: 1.5 }}
        >
          <Typography variant="subtitle1" fontWeight={700}>
            Thông báo {unreadCount > 0 && `(${unreadCount})`}
          </Typography>
          {unreadCount > 0 && (
            <Button
              size="small"
              startIcon={<DoneAllIcon fontSize="small" />}
              onClick={handleMarkAllRead}
              disabled={markingAll}
              sx={{ fontSize: "0.75rem", textTransform: "none" }}
            >
              Đọc tất cả
            </Button>
          )}
        </Stack>

        <Divider />

        <Box sx={{ overflowY: "auto", flex: 1 }}>
          {loading && notifications.length === 0 ? (
            <Stack alignItems="center" justifyContent="center" sx={{ py: 4 }}>
              <CircularProgress size={28} />
            </Stack>
          ) : notifications.length === 0 ? (
            <Box sx={{ py: 4, px: 2, textAlign: "center" }}>
              <Typography variant="body2" color="text.secondary">
                Không có thông báo nào.
              </Typography>
            </Box>
          ) : (
            <List disablePadding>
              {notifications.map((item) => (
                <ListItemButton
                  key={item.id}
                  onClick={() => void handleItemClick(item)}
                  sx={{
                    px: 2,
                    py: 1.25,
                    borderBottom: "1px solid",
                    borderColor: "divider",
                    bgcolor: item.isRead ? "transparent" : "action.hover",
                    "&:hover": {
                      bgcolor: "action.selected",
                    },
                  }}
                >
                  <ListItemText
                    secondaryTypographyProps={{ component: "div" }}
                    primary={
                      <Stack direction="row" alignItems="center" spacing={1}>
                        {!item.isRead && (
                          <Box
                            sx={{
                              width: 8,
                              height: 8,
                              borderRadius: "50%",
                              bgcolor: "primary.main",
                              flexShrink: 0,
                            }}
                          />
                        )}
                        <Typography
                          variant="body2"
                          fontWeight={item.isRead ? 400 : 700}
                          noWrap
                          sx={{ flex: 1 }}
                        >
                          {item.title}
                        </Typography>
                      </Stack>
                    }
                    secondary={
                      <Stack
                        spacing={0.5}
                        sx={{ mt: 0.5, pl: item.isRead ? 0 : 2 }}
                      >
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{
                            display: "-webkit-box",
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: "vertical",
                            overflow: "hidden",
                          }}
                        >
                          {item.message}
                        </Typography>
                        <Typography variant="caption" color="text.disabled">
                          {formatTime(item.createdAt)}
                        </Typography>
                      </Stack>
                    }
                  />
                </ListItemButton>
              ))}
            </List>
          )}
        </Box>
      </Popover>
    </>
  );
}
