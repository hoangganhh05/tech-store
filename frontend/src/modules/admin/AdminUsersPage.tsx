import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  InputAdornment,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import RefreshIcon from "@mui/icons-material/Refresh";
import { isAxiosError } from "axios";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { PageIntro } from "../../components/common/PageIntro";
import { useAuth } from "../../hooks/useAuth";
import {
  getAdminUsers,
  updateAdminUserStatus,
  type PageResponse,
} from "../../services/adminUserService";
import type { AuthenticatedUser } from "../../services/authService";

export function AdminUsersPage() {
  const { user: currentAdmin } = useAuth();

  const [users, setUsers] = useState<AuthenticatedUser[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeKeyword, setActiveKeyword] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const [targetUser, setTargetUser] = useState<AuthenticatedUser | null>(null);
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);

  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const response: PageResponse<AuthenticatedUser> = await getAdminUsers({
        keyword: activeKeyword,
        page,
        size: rowsPerPage,
      });
      setUsers(response.items);
      setTotalElements(response.totalElements);
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedbackMessage({
        type: "error",
        text: message || "Không thể tải danh sách tài khoản. Vui lòng thử lại.",
      });
    } finally {
      setIsLoading(false);
    }
  }, [activeKeyword, page, rowsPerPage]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleSearchSubmit = (event: FormEvent) => {
    event.preventDefault();
    setPage(0);
    setActiveKeyword(searchTerm.trim());
  };

  const handleOpenConfirmDialog = (user: AuthenticatedUser) => {
    setTargetUser(user);
    setConfirmDialogOpen(true);
  };

  const handleCloseConfirmDialog = () => {
    if (!isUpdating) {
      setConfirmDialogOpen(false);
      setTargetUser(null);
    }
  };

  const handleToggleStatus = async () => {
    if (!targetUser) return;
    const nextStatus = targetUser.status === "LOCKED" ? "ACTIVE" : "LOCKED";

    setIsUpdating(true);
    try {
      await updateAdminUserStatus(targetUser.id, nextStatus);
      setFeedbackMessage({
        type: "success",
        text:
          nextStatus === "ACTIVE"
            ? `Mở khoá tài khoản "${targetUser.email}" thành công.`
            : `Khoá tài khoản "${targetUser.email}" thành công.`,
      });
      handleCloseConfirmDialog();
      await fetchUsers();
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setFeedbackMessage({
        type: "error",
        text: message || "Thao tác không thành công. Vui lòng thử lại.",
      });
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <Box>
      <PageIntro
        eyebrow="Quản trị tài khoản"
        title="Danh sách người dùng"
        description="Quản lý thông tin, phân quyền và kiểm soát trạng thái khoá/mở khoá tài khoản trên hệ thống."
      />

      {feedbackMessage && (
        <Alert
          severity={feedbackMessage.type}
          onClose={() => setFeedbackMessage(null)}
          sx={{ mb: 3 }}
        >
          {feedbackMessage.text}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ p: 2.5 }}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={2}
            justifyContent="space-between"
            alignItems={{ xs: "stretch", sm: "center" }}
            component="form"
            onSubmit={handleSearchSubmit}
          >
            <TextField
              size="small"
              placeholder="Tìm theo họ tên hoặc email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              sx={{ minWidth: { sm: 320, md: 400 } }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" color="action" />
                    </InputAdornment>
                  ),
                },
              }}
            />
            <Stack direction="row" spacing={1}>
              <Button
                type="submit"
                variant="contained"
                startIcon={<SearchIcon />}
              >
                Tìm kiếm
              </Button>
              <Button
                variant="outlined"
                color="secondary"
                onClick={() => {
                  setSearchTerm("");
                  setActiveKeyword("");
                  setPage(0);
                }}
                disabled={!searchTerm && !activeKeyword}
              >
                Đặt lại
              </Button>
              <IconButton
                onClick={fetchUsers}
                disabled={isLoading}
                title="Làm mới dữ liệu"
              >
                <RefreshIcon />
              </IconButton>
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      <Paper sx={{ width: "100%", overflow: "hidden" }}>
        <TableContainer sx={{ minHeight: 300 }}>
          <Table stickyHeader aria-label="Bảng danh sách người dùng">
            <TableHead>
              <TableRow>
                <TableCell width={60}>
                  <strong>ID</strong>
                </TableCell>
                <TableCell>
                  <strong>Họ tên</strong>
                </TableCell>
                <TableCell>
                  <strong>Email</strong>
                </TableCell>
                <TableCell>
                  <strong>Số điện thoại</strong>
                </TableCell>
                <TableCell>
                  <strong>Vai trò</strong>
                </TableCell>
                <TableCell>
                  <strong>Trạng thái</strong>
                </TableCell>
                <TableCell align="right">
                  <strong>Thao tác</strong>
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={36} />
                    <Typography variant="body2" color="text.secondary" mt={1}>
                      Đang tải danh sách người dùng...
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : users.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <Typography variant="body1" color="text.secondary">
                      Không tìm thấy người dùng nào phù hợp.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                users.map((user) => {
                  const isSelf = currentAdmin?.id === user.id;
                  const isLocked = user.status === "LOCKED";

                  return (
                    <TableRow key={user.id} hover>
                      <TableCell>{user.id}</TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600}>
                          {user.fullName}
                        </Typography>
                      </TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>{user.phone || "—"}</TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={0.5}>
                          {user.roles.map((role) => (
                            <Chip
                              key={role}
                              label={
                                role === "ADMIN"
                                  ? "Quản trị viên"
                                  : "Khách hàng"
                              }
                              size="small"
                              color={role === "ADMIN" ? "primary" : "default"}
                              variant={role === "ADMIN" ? "filled" : "outlined"}
                            />
                          ))}
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={isLocked ? "Đã khoá" : "Hoạt động"}
                          size="small"
                          color={isLocked ? "error" : "success"}
                          variant={isLocked ? "outlined" : "filled"}
                        />
                      </TableCell>
                      <TableCell align="right">
                        {isSelf ? (
                          <Tooltip title="Không thể tự khoá tài khoản của chính mình">
                            <span>
                              <Button
                                size="small"
                                color="error"
                                variant="outlined"
                                disabled
                                startIcon={<LockOutlinedIcon />}
                              >
                                Khoá
                              </Button>
                            </span>
                          </Tooltip>
                        ) : isLocked ? (
                          <Button
                            size="small"
                            color="success"
                            variant="outlined"
                            startIcon={<LockOpenOutlinedIcon />}
                            onClick={() => handleOpenConfirmDialog(user)}
                          >
                            Mở khoá
                          </Button>
                        ) : (
                          <Button
                            size="small"
                            color="error"
                            variant="outlined"
                            startIcon={<LockOutlinedIcon />}
                            onClick={() => handleOpenConfirmDialog(user)}
                          >
                            Khoá
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={totalElements}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          labelRowsPerPage="Số hàng mỗi trang:"
          labelDisplayedRows={({ from, to, count }) =>
            `${from}–${to} trên ${count}`
          }
        />
      </Paper>

      {/* Confirmation Dialog */}
      <Dialog
        open={confirmDialogOpen}
        onClose={handleCloseConfirmDialog}
        aria-labelledby="confirm-dialog-title"
      >
        <DialogTitle id="confirm-dialog-title">
          {targetUser?.status === "LOCKED"
            ? "Mở khoá tài khoản người dùng?"
            : "Khoá tài khoản người dùng?"}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {targetUser?.status === "LOCKED"
              ? `Bạn có chắc chắn muốn mở khoá tài khoản "${targetUser?.email}"? Người dùng này sẽ có thể đăng nhập lại bình thường.`
              : `Bạn có chắc chắn muốn khoá tài khoản "${targetUser?.email}"? Người dùng này sẽ bị đăng xuất ngay lập tức và không thể tiếp tục đăng nhập.`}
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={handleCloseConfirmDialog} disabled={isUpdating}>
            Huỷ bỏ
          </Button>
          <Button
            onClick={handleToggleStatus}
            variant="contained"
            color={targetUser?.status === "LOCKED" ? "success" : "error"}
            disabled={isUpdating}
            autoFocus
          >
            {isUpdating
              ? "Đang xử lý..."
              : targetUser?.status === "LOCKED"
                ? "Mở khoá"
                : "Khoá tài khoản"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
