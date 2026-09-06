import {
  Alert,
  Avatar,
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
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
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
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined'
import SubdirectoryArrowRightIcon from '@mui/icons-material/SubdirectoryArrowRight'
import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { PageIntro } from '../../components/common/PageIntro'
import {
  createAdminCategory,
  deleteAdminCategory,
  getAdminCategories,
  getAdminCategoryTree,
  updateAdminCategory,
  type Category,
  type CategoryPayload,
  type CategoryTree,
} from '../../services/categoryService'

type FlattenedTreeCategory = {
  id: number
  name: string
  description?: string | null
  parentId?: number | null
  imageUrl?: string | null
  level: number
  childrenCount: number
}

function flattenTree(nodes: CategoryTree[], level = 0): FlattenedTreeCategory[] {
  const result: FlattenedTreeCategory[] = []
  for (const node of nodes) {
    result.push({
      id: node.id,
      name: node.name,
      description: node.description,
      parentId: node.parentId,
      imageUrl: node.imageUrl,
      level,
      childrenCount: node.children ? node.children.length : 0,
    })
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, level + 1))
    }
  }
  return result
}

function getDescendantIds(nodeId: number, flatList: FlattenedTreeCategory[]): Set<number> {
  const descendantIds = new Set<number>()
  const queue = [nodeId]

  while (queue.length > 0) {
    const currentId = queue.shift()!
    const directChildren = flatList.filter((c) => c.parentId === currentId)
    for (const child of directChildren) {
      if (!descendantIds.has(child.id)) {
        descendantIds.add(child.id)
        queue.push(child.id)
      }
    }
  }

  return descendantIds
}

export function AdminCategoriesPage() {
  const [flatCategories, setFlatCategories] = useState<Category[]>([])
  const [treeCategories, setTreeCategories] = useState<FlattenedTreeCategory[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: 'success' | 'error'
    text: string
  } | null>(null)

  // Dialog State
  const [formDialogOpen, setFormDialogOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [formData, setFormData] = useState<CategoryPayload>({
    name: '',
    description: '',
    parentId: null,
    imageUrl: '',
  })
  const [formErrors, setFormErrors] = useState<{ name?: string }>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Confirm Dialog
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [categoryToDelete, setCategoryToDelete] = useState<FlattenedTreeCategory | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchData = useCallback(async () => {
    setIsLoading(true)
    try {
      const [allList, treeList] = await Promise.all([
        getAdminCategories(),
        getAdminCategoryTree(),
      ])
      setFlatCategories(allList)
      setTreeCategories(flattenTree(treeList))
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      setFeedbackMessage({
        type: 'error',
        text: message || 'Không thể tải danh sách danh mục. Vui lòng thử lại.',
      })
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleOpenAddDialog = () => {
    setEditingCategory(null)
    setFormData({
      name: '',
      description: '',
      parentId: null,
      imageUrl: '',
    })
    setFormErrors({})
    setFormDialogOpen(true)
  }

  const handleOpenEditDialog = (item: FlattenedTreeCategory) => {
    const rawCat = flatCategories.find((c) => c.id === item.id)
    setEditingCategory(rawCat || null)
    setFormData({
      name: item.name,
      description: item.description || '',
      parentId: item.parentId ?? null,
      imageUrl: item.imageUrl || '',
    })
    setFormErrors({})
    setFormDialogOpen(true)
  }

  const handleCloseFormDialog = () => {
    if (isSubmitting) return
    setFormDialogOpen(false)
    setEditingCategory(null)
    setFormErrors({})
  }

  const handleSubmitForm = async (e: FormEvent) => {
    e.preventDefault()

    const trimmedName = formData.name.trim()
    if (!trimmedName) {
      setFormErrors({ name: 'Tên danh mục không được để trống.' })
      return
    }

    setIsSubmitting(true)
    setFormErrors({})

    try {
      const payload: CategoryPayload = {
        name: trimmedName,
        description: formData.description?.trim() || undefined,
        parentId: formData.parentId ?? null,
        imageUrl: formData.imageUrl?.trim() || undefined,
      }

      if (editingCategory) {
        await updateAdminCategory(editingCategory.id, payload)
        setFeedbackMessage({
          type: 'success',
          text: `Đã cập nhật danh mục "${trimmedName}" thành công.`,
        })
      } else {
        await createAdminCategory(payload)
        setFeedbackMessage({
          type: 'success',
          text: `Đã tạo danh mục mới "${trimmedName}" thành công.`,
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
          (editingCategory
            ? 'Không thể cập nhật danh mục. Vui lòng kiểm tra lại.'
            : 'Không thể tạo danh mục. Vui lòng kiểm tra lại.'),
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleOpenDeleteDialog = (item: FlattenedTreeCategory) => {
    setCategoryToDelete(item)
    setDeleteDialogOpen(true)
  }

  const handleCloseDeleteDialog = () => {
    if (isDeleting) return
    setDeleteDialogOpen(false)
    setCategoryToDelete(null)
  }

  const handleConfirmDelete = async () => {
    if (!categoryToDelete) return
    setIsDeleting(true)

    try {
      await deleteAdminCategory(categoryToDelete.id)
      setFeedbackMessage({
        type: 'success',
        text: `Đã xoá danh mục "${categoryToDelete.name}" thành công.`,
      })
      setDeleteDialogOpen(false)
      setCategoryToDelete(null)
      await fetchData()
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined
      setFeedbackMessage({
        type: 'error',
        text: message || 'Không thể xoá danh mục. Vui lòng thử lại.',
      })
    } finally {
      setIsDeleting(false)
    }
  }

  // Chặn chọn chính nó hoặc con cháu làm danh mục cha khi đang chỉnh sửa
  const excludedParentIds = editingCategory
    ? new Set([editingCategory.id, ...Array.from(getDescendantIds(editingCategory.id, treeCategories))])
    : new Set<number>()

  const selectableParents = flatCategories.filter(
    (c) => !excludedParentIds.has(c.id),
  )

  return (
    <Stack spacing={3}>
      <PageIntro
        eyebrow="Quản trị"
        title="Quản lý danh mục sản phẩm"
        description="Quản lý phân cấp danh mục sản phẩm đa cấp (cha - con), thêm mới, cập nhật và xoá danh mục."
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
              Thêm danh mục
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

      <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider' }}>
        <CardContent sx={{ p: 0 }}>
          {isLoading && treeCategories.length === 0 ? (
            <Box sx={{ p: 6, display: 'flex', justifyContent: 'center' }}>
              <CircularProgress />
            </Box>
          ) : treeCategories.length === 0 ? (
            <Box sx={{ p: 6, textAlign: 'center' }}>
              <FolderOutlinedIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1.5 }} />
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Chưa có danh mục nào
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Bắt đầu bằng việc thêm danh mục gốc cấp cao nhất cho hệ thống sản phẩm.
              </Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={handleOpenAddDialog}
              >
                Thêm danh mục đầu tiên
              </Button>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0}>
              <Table sx={{ minWidth: 650 }}>
                <TableHead>
                  <TableRow sx={{ bgcolor: 'grey.50' }}>
                    <TableCell sx={{ fontWeight: 600 }}>Tên danh mục</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Mô tả</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Cấp độ
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="center">
                      Danh mục con
                    </TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Thao tác
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {treeCategories.map((item) => {
                    const isRoot = item.level === 0
                    const hasChildren = item.childrenCount > 0

                    return (
                      <TableRow key={item.id} hover>
                        <TableCell>
                          <Box
                            sx={{
                              display: 'flex',
                              alignItems: 'center',
                              pl: item.level * 3.5,
                            }}
                          >
                            {item.level > 0 && (
                              <SubdirectoryArrowRightIcon
                                fontSize="small"
                                sx={{ color: 'text.secondary', mr: 1 }}
                              />
                            )}
                            {item.imageUrl ? (
                              <Avatar
                                src={item.imageUrl}
                                alt={item.name}
                                variant="rounded"
                                sx={{ width: 32, height: 32, mr: 1.5 }}
                              />
                            ) : (
                              <FolderOutlinedIcon
                                fontSize="small"
                                sx={{
                                  color: isRoot ? 'primary.main' : 'text.secondary',
                                  mr: 1.5,
                                }}
                              />
                            )}
                            <Typography
                              variant="body2"
                              sx={{
                                fontWeight: isRoot ? 600 : 500,
                                color: 'text.primary',
                              }}
                            >
                              {item.name}
                            </Typography>
                          </Box>
                        </TableCell>
                        <TableCell sx={{ maxWidth: 300 }}>
                          <Typography
                            variant="body2"
                            color="text.secondary"
                            noWrap
                            title={item.description || ''}
                          >
                            {item.description || '—'}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          {isRoot ? (
                            <Chip
                              label="Gốc"
                              size="small"
                              color="primary"
                              variant="filled"
                              sx={{ fontWeight: 600, fontSize: '0.75rem' }}
                            />
                          ) : (
                            <Chip
                              label={`Cấp ${item.level}`}
                              size="small"
                              variant="outlined"
                              sx={{ fontSize: '0.75rem' }}
                            />
                          )}
                        </TableCell>
                        <TableCell align="center">
                          {hasChildren ? (
                            <Chip
                              label={`${item.childrenCount} con`}
                              size="small"
                              color="secondary"
                              variant="outlined"
                              sx={{ fontSize: '0.75rem' }}
                            />
                          ) : (
                            <Typography variant="caption" color="text.secondary">
                              0
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell align="right">
                          <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                            <Tooltip title="Chỉnh sửa danh mục">
                              <IconButton
                                size="small"
                                aria-label={`Sửa ${item.name}`}
                                onClick={() => handleOpenEditDialog(item)}
                              >
                                <EditOutlinedIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip
                              title={
                                hasChildren
                                  ? 'Không thể xoá danh mục đang có danh mục con'
                                  : 'Xoá danh mục'
                              }
                            >
                              <span>
                                <IconButton
                                  size="small"
                                  color="error"
                                  aria-label={`Xoá ${item.name}`}
                                  disabled={hasChildren}
                                  onClick={() => handleOpenDeleteDialog(item)}
                                >
                                  <DeleteOutlineIcon fontSize="small" />
                                </IconButton>
                              </span>
                            </Tooltip>
                          </Stack>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Dialog Thêm / Sửa Danh mục */}
      <Dialog
        open={formDialogOpen}
        onClose={handleCloseFormDialog}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmitForm}>
          <DialogTitle>
            {editingCategory ? 'Chỉnh sửa danh mục sản phẩm' : 'Thêm danh mục sản phẩm mới'}
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              <TextField
                label="Tên danh mục"
                required
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                error={Boolean(formErrors.name)}
                helperText={formErrors.name || 'Tên danh mục không được trùng trong cùng một cấp.'}
                disabled={isSubmitting}
                autoFocus
              />

              <FormControl fullWidth disabled={isSubmitting}>
                <InputLabel id="parent-category-select-label">Danh mục cha (Tuỳ chọn)</InputLabel>
                <Select<number | string>
                  labelId="parent-category-select-label"
                  label="Danh mục cha (Tuỳ chọn)"
                  value={formData.parentId ?? ''}
                  onChange={(e) => {
                    const val = e.target.value
                    setFormData({ ...formData, parentId: val === '' ? null : Number(val) })
                  }}
                >
                  <MenuItem value="">
                    <em>Không có (Danh mục gốc cấp cao nhất)</em>
                  </MenuItem>
                  {selectableParents.map((cat) => (
                    <MenuItem key={cat.id} value={cat.id}>
                      {cat.parentName ? `${cat.parentName} ➔ ${cat.name}` : cat.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <TextField
                label="Đường dẫn ảnh đại diện (URL)"
                fullWidth
                placeholder="https://example.com/category-image.png"
                value={formData.imageUrl || ''}
                onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                disabled={isSubmitting}
              />

              <TextField
                label="Mô tả danh mục"
                fullWidth
                multiline
                rows={3}
                placeholder="Nhập mô tả ngắn gọn về danh mục..."
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
              {editingCategory ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Dialog Xác nhận Xoá Danh mục */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleCloseDeleteDialog}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Xác nhận xoá danh mục?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc chắn muốn xoá danh mục{' '}
            <strong>{categoryToDelete?.name}</strong> không? Hành động này không thể
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
            Xoá danh mục
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
