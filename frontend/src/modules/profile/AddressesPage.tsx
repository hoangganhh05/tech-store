import AddIcon from '@mui/icons-material/Add'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import HomeIcon from '@mui/icons-material/Home'
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
  Divider,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material'
import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState } from 'react'
import { deleteMyAddress, getMyAddresses, setDefaultAddress, type Address } from '../../services/userService'
import { AddressFormDialog } from './AddressFormDialog'

export function AddressesPage() {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  // Dialog form (thêm/sửa)
  const [formOpen, setFormOpen] = useState(false)
  const [editingAddress, setEditingAddress] = useState<Address | null>(null)

  // Dialog xác nhận xoá
  const [deleteTarget, setDeleteTarget] = useState<Address | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Đang xử lý đặt mặc định
  const [settingDefaultId, setSettingDefaultId] = useState<number | null>(null)

  const reload = useCallback(() => {
    setIsLoading(true)
    setLoadError('')
    setReloadKey((k) => k + 1)
  }, [])

  useEffect(() => {
    let active = true
    getMyAddresses()
      .then((data) => {
        if (active) setAddresses(data)
      })
      .catch(() => {
        if (active) setLoadError('Không thể tải danh sách địa chỉ. Vui lòng thử lại.')
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => {
      active = false
    }
  }, [reloadKey])

  const handleOpenAdd = () => {
    setEditingAddress(null)
    setFormOpen(true)
    setActionError('')
  }

  const handleOpenEdit = (address: Address) => {
    setEditingAddress(address)
    setFormOpen(true)
    setActionError('')
  }

  const handleFormSaved = (saved: Address) => {
    setFormOpen(false)
    setAddresses((prev) => {
      const exists = prev.some((a) => a.id === saved.id)
      if (exists) {
        return prev.map((a) => (a.id === saved.id ? saved : a))
      }
      // Địa chỉ mới: thêm vào đầu nếu là default, cuối nếu không
      return saved.isDefault ? [saved, ...prev] : [...prev, saved]
    })
  }

  const handleSetDefault = async (address: Address) => {
    if (address.isDefault) return
    setSettingDefaultId(address.id)
    setActionError('')
    try {
      const updated = await setDefaultAddress(address.id)
      setAddresses((prev) =>
        prev.map((a) => ({
          ...a,
          isDefault: a.id === updated.id,
        })),
      )
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error) ? error.response?.data?.message : undefined
      setActionError(message || 'Không thể đặt địa chỉ mặc định lúc này.')
    } finally {
      setSettingDefaultId(null)
    }
  }

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return
    setIsDeleting(true)
    setActionError('')
    try {
      await deleteMyAddress(deleteTarget.id)
      setAddresses((prev) => prev.filter((a) => a.id !== deleteTarget.id))
      setDeleteTarget(null)
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error) ? error.response?.data?.message : undefined
      setActionError(message || 'Không thể xoá địa chỉ lúc này. Vui lòng thử lại.')
      setDeleteTarget(null)
    } finally {
      setIsDeleting(false)
    }
  }

  if (isLoading) {
    return (
      <Stack alignItems="center" py={8} spacing={2}>
        <CircularProgress />
        <Typography>Đang tải danh sách địa chỉ...</Typography>
      </Stack>
    )
  }

  if (loadError) {
    return (
      <Stack alignItems="center" py={8} spacing={2}>
        <Alert severity="error">{loadError}</Alert>
        <Button variant="outlined" onClick={reload}>
          Thử lại
        </Button>
      </Stack>
    )
  }

  return (
    <>
      <Stack spacing={3} sx={{ maxWidth: 720, mx: 'auto' }}>
        {/* Header */}
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Box>
            <Typography component="h1" variant="h2">
              Địa chỉ giao hàng
            </Typography>
            <Typography color="text.secondary" mt={0.5}>
              Quản lý các địa chỉ giao hàng của bạn.
            </Typography>
          </Box>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenAdd}>
            Thêm địa chỉ
          </Button>
        </Stack>

        {/* Action error */}
        {actionError && (
          <Alert severity="error" onClose={() => setActionError('')}>
            {actionError}
          </Alert>
        )}

        {/* Empty state */}
        {addresses.length === 0 && (
          <Card>
            <CardContent>
              <Stack alignItems="center" py={4} spacing={2}>
                <HomeIcon sx={{ fontSize: 56, color: 'text.disabled' }} />
                <Typography color="text.secondary">Bạn chưa có địa chỉ giao hàng nào.</Typography>
                <Button variant="outlined" startIcon={<AddIcon />} onClick={handleOpenAdd}>
                  Thêm địa chỉ đầu tiên
                </Button>
              </Stack>
            </CardContent>
          </Card>
        )}

        {/* Address cards */}
        {addresses.map((address, index) => (
          <Card
            key={address.id}
            variant={address.isDefault ? 'outlined' : 'elevation'}
            sx={address.isDefault ? { borderColor: 'primary.main', borderWidth: 2 } : undefined}
          >
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
                {/* Address info */}
                <Box flex={1}>
                  <Stack direction="row" alignItems="center" spacing={1} mb={0.5}>
                    <Typography variant="subtitle1" fontWeight={600}>
                      {address.recipientName}
                    </Typography>
                    <Typography color="text.secondary">·</Typography>
                    <Typography color="text.secondary">{address.phone}</Typography>
                    {address.isDefault && (
                      <Chip label="Mặc định" size="small" color="primary" variant="outlined" />
                    )}
                  </Stack>
                  <Typography color="text.secondary" variant="body2">
                    {address.streetAddress}, {address.ward}, {address.district}, {address.province}
                  </Typography>
                </Box>

                {/* Actions */}
                <Stack direction="row" spacing={0.5}>
                  <Tooltip title="Chỉnh sửa">
                    <IconButton size="small" onClick={() => handleOpenEdit(address)}>
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Xoá">
                    <span>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => {
                          setActionError('')
                          setDeleteTarget(address)
                        }}
                      >
                        <DeleteOutlineIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </Stack>
              </Stack>

              {!address.isDefault && (
                <>
                  {index !== 0 && <Divider sx={{ my: 1.5 }} />}
                  <Button
                    size="small"
                    variant="text"
                    onClick={() => handleSetDefault(address)}
                    disabled={settingDefaultId === address.id}
                  >
                    {settingDefaultId === address.id ? 'Đang đặt...' : 'Đặt làm mặc định'}
                  </Button>
                </>
              )}
            </CardContent>
          </Card>
        ))}
      </Stack>

      {/* Form dialog */}
      <AddressFormDialog
        open={formOpen}
        editing={editingAddress}
        onClose={() => setFormOpen(false)}
        onSaved={handleFormSaved}
      />

      {/* Delete confirm dialog */}
      <Dialog open={Boolean(deleteTarget)} onClose={isDeleting ? undefined : () => setDeleteTarget(null)}>
        <DialogTitle>Xoá địa chỉ?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc muốn xoá địa chỉ{' '}
            <strong>
              {deleteTarget?.streetAddress}, {deleteTarget?.province}
            </strong>
            ? Hành động này không thể hoàn tác.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)} disabled={isDeleting}>
            Huỷ
          </Button>
          <Button color="error" variant="contained" onClick={handleDeleteConfirm} disabled={isDeleting}>
            {isDeleting ? 'Đang xoá...' : 'Xoá'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
