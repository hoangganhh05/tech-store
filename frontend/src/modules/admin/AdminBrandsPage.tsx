import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
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
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import RefreshIcon from '@mui/icons-material/Refresh'
import SearchIcon from '@mui/icons-material/Search'
import BrandingWatermarkOutlinedIcon from '@mui/icons-material/BrandingWatermarkOutlined'
import { isAxiosError } from 'axios'
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { PageIntro } from '../../components/common/PageIntro'
import {
  createAdminBrand,
  deleteAdminBrand,
  getAdminBrands,
  updateAdminBrand,
  type Brand,
  type BrandPayload,
} from '../../services/brandService'

export function AdminBrandsPage() {
  const [brands, setBrands] = useState<Brand[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: 'success' | 'error'
    text: string
  } | null>(null)

  // Dialog Thêm / Sửa
  const [formDialogOpen, setFormDialogOpen] = useState(false)
  const [editingBrand, setEditingBrand] = useState<Brand | null>(null)
  const [formData, setFormData] = useState<BrandPayload>({
    name: '',
    logoUrl: '',
    description: '',
  })
  const [formErrors, setFormErrors] = useState<{ name?: string }>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Dialog Xoá
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [brandToDelete, setBrandToDelete] = useState<Brand | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchData = useCallback(async () => {
    setIsLoading(true)
    try {
      const data = await getAdminBrands()
      setBrands(data)
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      setFeedbackMessage({
        type: 'error',
        text: message || 'Không thể tải danh sách thương hiệu. Vui lòng thử lại.',
      })
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const filteredBrands = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase()
    if (!normalized) return brands
    return brands.filter((b) => b.name.toLowerCase().includes(normalized))
  }, [brands, searchTerm])

  const handleOpenAddDialog = () => {
    setEditingBrand(null)
    setFormData({
      name: '',
      logoUrl: '',
      description: '',
    })
    setFormErrors({})
    setFormDialogOpen(true)
  }

  const handleOpenEditDialog = (brand: Brand) => {
    setEditingBrand(brand)
    setFormData({
      name: brand.name,
      logoUrl: brand.logoUrl || '',
      description: brand.description || '',
    })
    setFormErrors({})
    setFormDialogOpen(true)
  }

  const handleCloseFormDialog = () => {
    if (isSubmitting) return
    setFormDialogOpen(false)
    setEditingBrand(null)
    setFormErrors({})
  }

  const handleSubmitForm = async (e: FormEvent) => {
    e.preventDefault()

    const trimmedName = formData.name.trim()
    if (!trimmedName) {
      setFormErrors({ name: 'Tên thương hiệu không được để trống.' })
      return
    }

    setIsSubmitting(true)
    setFormErrors({})

    try {
      const payload: BrandPayload = {
        name: trimmedName,
        logoUrl: formData.logoUrl?.trim() || undefined,
        description: formData.description?.trim() || undefined,
      }

      if (editingBrand) {
        await updateAdminBrand(editingBrand.id, payload)
        setFeedbackMessage({
          type: 'success',
          text: `Đã cập nhật thương hiệu "${trimmedName}" thành công.`,
        })
      } else {
        await createAdminBrand(payload)
        setFeedbackMessage({
          type: 'success',
          text: `Đã tạo thương hiệu mới "${trimmedName}" thành công.`,
        })
      }

      setFormDialogOpen(false)
      await fetchData()
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      setFeedbackMessage({
        type: 'error',
        text:
          message ||
          (editingBrand
            ? 'Không thể cập nhật thương hiệu. Vui lòng kiểm tra lại.'
            : 'Không thể tạo thương hiệu. Vui lòng kiểm tra lại.'),
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleOpenDeleteDialog = (brand: Brand) => {
    setBrandToDelete(brand)
    setDeleteDialogOpen(true)
  }

  const handleCloseDeleteDialog = () => {
    if (isDeleting) return
    setDeleteDialogOpen(false)
    setBrandToDelete(null)
  }

  const handleConfirmDelete = async () => {
    if (!brandToDelete) return
    setIsDeleting(true)

    try {
      await deleteAdminBrand(brandToDelete.id)
      setFeedbackMessage({
        type: 'success',
        text: `Đã xoá thương hiệu "${brandToDelete.name}" thành công.`,
      })
      setDeleteDialogOpen(false)
      setBrandToDelete(null)
      await fetchData()
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      setFeedbackMessage({
        type: 'error',
        text: message || 'Không thể xoá thương hiệu. Vui lòng thử lại.',
      })
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <Stack spacing={3}>
      <PageIntro
        eyebrow="Quản trị"
        title="Quản lý thương hiệu"
        description="Quản lý danh sách thương hiệu của hệ thống cửa hàng công nghệ, thêm mới, sửa và xoá thương hiệu."
        action={
          <Stack direction="row" spacing={1.5}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={fetchData}
              disabled={isLoading}
            >
              Làm mới
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleOpenAddDialog}
            >
              Thêm thương hiệu
            </Button>
          </Stack>
        }
      />

      {feedbackMessage && (
        <Alert
          severity={feedbackMessage.type}
          onClose={() => setFeedbackMessage(null)}
          sx={{ mb: 1 }}
        >
          {feedbackMessage.text}
        </Alert>
      )}

      <Box sx={{ maxWidth: 360 }}>
        <TextField
          placeholder="Tìm kiếm thương hiệu..."
          size="small"
          fullWidth
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
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
      </Box>

      <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider' }}>
        <CardContent sx={{ p: 0 }}>
          {isLoading && brands.length === 0 ? (
            <Box sx={{ p: 6, display: 'flex', justifyContent: 'center' }}>
              <CircularProgress />
            </Box>
          ) : brands.length === 0 ? (
            <Box sx={{ p: 6, textAlign: 'center' }}>
              <BrandingWatermarkOutlinedIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1.5 }} />
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Chưa có thương hiệu nào
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Bắt đầu bằng việc thêm thương hiệu đầu tiên cho hệ thống sản phẩm.
              </Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={handleOpenAddDialog}
              >
                Thêm thương hiệu đầu tiên
              </Button>
            </Box>
          ) : filteredBrands.length === 0 ? (
            <Box sx={{ p: 6, textAlign: 'center' }}>
              <Typography variant="body1" color="text.secondary">
                Không tìm thấy thương hiệu nào phù hợp với &quot;{searchTerm}&quot;.
              </Typography>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0}>
              <Table sx={{ minWidth: 650 }}>
                <TableHead>
                  <TableRow sx={{ bgcolor: 'grey.50' }}>
                    <TableCell sx={{ fontWeight: 600, width: 90 }}>Logo</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Tên thương hiệu</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Mô tả</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Thao tác
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredBrands.map((brand) => (
                    <TableRow key={brand.id} hover>
                      <TableCell>
                        <Avatar
                          src={brand.logoUrl || undefined}
                          alt={brand.name}
                          variant="rounded"
                          sx={{
                            width: 42,
                            height: 42,
                            bgcolor: 'primary.light',
                            color: 'primary.contrastText',
                            fontWeight: 700,
                          }}
                        >
                          {brand.name.charAt(0).toUpperCase()}
                        </Avatar>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600} color="text.primary">
                          {brand.name}
                        </Typography>
                      </TableCell>
                      <TableCell sx={{ maxWidth: 350 }}>
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          noWrap
                          title={brand.description || ''}
                        >
                          {brand.description || '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                          <Tooltip title="Chỉnh sửa thương hiệu">
                            <IconButton
                              size="small"
                              aria-label={`Sửa ${brand.name}`}
                              onClick={() => handleOpenEditDialog(brand)}
                            >
                              <EditOutlinedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Xoá thương hiệu">
                            <IconButton
                              size="small"
                              color="error"
                              aria-label={`Xoá ${brand.name}`}
                              onClick={() => handleOpenDeleteDialog(brand)}
                            >
                              <DeleteOutlineIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Dialog Thêm / Sửa Thương hiệu */}
      <Dialog
        open={formDialogOpen}
        onClose={handleCloseFormDialog}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmitForm}>
          <DialogTitle>
            {editingBrand ? 'Chỉnh sửa thương hiệu' : 'Thêm thương hiệu mới'}
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              <TextField
                label="Tên thương hiệu"
                required
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                error={Boolean(formErrors.name)}
                helperText={formErrors.name || 'Tên thương hiệu là duy nhất trong hệ thống.'}
                disabled={isSubmitting}
                autoFocus
              />

              <TextField
                label="Đường dẫn Logo (URL)"
                fullWidth
                placeholder="https://example.com/brand-logo.png"
                value={formData.logoUrl || ''}
                onChange={(e) => setFormData({ ...formData, logoUrl: e.target.value })}
                disabled={isSubmitting}
              />

              {formData.logoUrl?.trim() && (
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    p: 1.5,
                    bgcolor: 'grey.50',
                    borderRadius: 1,
                    border: '1px dashed',
                    borderColor: 'divider',
                  }}
                >
                  <Avatar
                    src={formData.logoUrl.trim()}
                    alt="Xem trước logo"
                    variant="rounded"
                    sx={{ width: 48, height: 48 }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    Xem trước hiển thị logo thương hiệu
                  </Typography>
                </Box>
              )}

              <TextField
                label="Mô tả thương hiệu"
                fullWidth
                multiline
                rows={3}
                placeholder="Nhập mô tả ngắn gọn về thương hiệu..."
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                disabled={isSubmitting}
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={handleCloseFormDialog} disabled={isSubmitting}>
              Huỷ
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={isSubmitting}
              startIcon={isSubmitting ? <CircularProgress size={16} /> : null}
            >
              {editingBrand ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Dialog Xác nhận Xoá Thương hiệu */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleCloseDeleteDialog}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Xác nhận xoá thương hiệu?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc chắn muốn xoá thương hiệu{' '}
            <strong>{brandToDelete?.name}</strong> không? Hành động này không thể
            hoàn tác.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={handleCloseDeleteDialog} disabled={isDeleting}>
            Huỷ bỏ
          </Button>
          <Button
            onClick={handleConfirmDelete}
            color="error"
            variant="contained"
            disabled={isDeleting}
            startIcon={isDeleting ? <CircularProgress size={16} /> : null}
          >
            Xoá thương hiệu
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
