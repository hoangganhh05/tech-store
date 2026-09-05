import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material'
import { isAxiosError } from 'axios'
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { addMyAddress, updateMyAddress, type Address, type AddressPayload } from '../../services/userService'

type AddressField = keyof AddressPayload
type FieldErrors = Partial<Record<AddressField, string>>

const empty: AddressPayload = {
  recipientName: '',
  phone: '',
  province: '',
  district: '',
  ward: '',
  streetAddress: '',
}

const phonePattern = /^[0-9+() .\-]{7,20}$/

interface Props {
  open: boolean
  editing: Address | null
  onClose: () => void
  onSaved: (address: Address) => void
}

export function AddressFormDialog({ open, editing, onClose, onSaved }: Props) {
  const [values, setValues] = useState<AddressPayload>(empty)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Nạp dữ liệu khi mở dialog chỉnh sửa
  useEffect(() => {
    if (open) {
      if (editing) {
        setValues({
          recipientName: editing.recipientName,
          phone: editing.phone,
          province: editing.province,
          district: editing.district,
          ward: editing.ward,
          streetAddress: editing.streetAddress,
        })
      } else {
        setValues(empty)
      }
      setFieldErrors({})
      setSubmitError('')
    }
  }, [open, editing])

  const handleChange = (field: AddressField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (!values.recipientName.trim()) errors.recipientName = 'Vui lòng nhập tên người nhận.'
    else if (values.recipientName.trim().length > 150) errors.recipientName = 'Tên không được vượt quá 150 ký tự.'
    if (!values.phone.trim()) errors.phone = 'Vui lòng nhập số điện thoại.'
    else if (!phonePattern.test(values.phone.trim())) errors.phone = 'Số điện thoại không đúng định dạng.'
    if (!values.province.trim()) errors.province = 'Vui lòng nhập tỉnh/thành phố.'
    if (!values.district.trim()) errors.district = 'Vui lòng nhập quận/huyện.'
    if (!values.ward.trim()) errors.ward = 'Vui lòng nhập phường/xã.'
    if (!values.streetAddress.trim()) errors.streetAddress = 'Vui lòng nhập địa chỉ chi tiết.'
    return errors
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const errors = validate()
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setIsSubmitting(true)
    setSubmitError('')
    try {
      const payload: AddressPayload = {
        recipientName: values.recipientName.trim(),
        phone: values.phone.trim(),
        province: values.province.trim(),
        district: values.district.trim(),
        ward: values.ward.trim(),
        streetAddress: values.streetAddress.trim(),
      }
      const saved = editing
        ? await updateMyAddress(editing.id, payload)
        : await addMyAddress(payload)
      onSaved(saved)
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error) ? error.response?.data?.message : undefined
      setSubmitError(message || 'Không thể lưu địa chỉ lúc này. Vui lòng thử lại sau.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onClose={isSubmitting ? undefined : onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới'}</DialogTitle>
      <Stack component="form" onSubmit={handleSubmit} noValidate>
        <DialogContent>
          <Stack spacing={2}>
            {submitError && <Alert severity="error">{submitError}</Alert>}
            <TextField
              label="Người nhận"
              value={values.recipientName}
              onChange={handleChange('recipientName')}
              error={Boolean(fieldErrors.recipientName)}
              helperText={fieldErrors.recipientName}
              required
              disabled={isSubmitting}
              autoComplete="name"
            />
            <TextField
              label="Số điện thoại"
              value={values.phone}
              onChange={handleChange('phone')}
              error={Boolean(fieldErrors.phone)}
              helperText={fieldErrors.phone}
              required
              disabled={isSubmitting}
              autoComplete="tel"
            />
            <TextField
              label="Tỉnh/Thành phố"
              value={values.province}
              onChange={handleChange('province')}
              error={Boolean(fieldErrors.province)}
              helperText={fieldErrors.province}
              required
              disabled={isSubmitting}
            />
            <TextField
              label="Quận/Huyện"
              value={values.district}
              onChange={handleChange('district')}
              error={Boolean(fieldErrors.district)}
              helperText={fieldErrors.district}
              required
              disabled={isSubmitting}
            />
            <TextField
              label="Phường/Xã"
              value={values.ward}
              onChange={handleChange('ward')}
              error={Boolean(fieldErrors.ward)}
              helperText={fieldErrors.ward}
              required
              disabled={isSubmitting}
            />
            <TextField
              label="Địa chỉ chi tiết"
              value={values.streetAddress}
              onChange={handleChange('streetAddress')}
              error={Boolean(fieldErrors.streetAddress)}
              helperText={fieldErrors.streetAddress}
              required
              disabled={isSubmitting}
              multiline
              rows={2}
              placeholder="Số nhà, tên đường, toà nhà..."
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} disabled={isSubmitting}>
            Huỷ
          </Button>
          <Button type="submit" variant="contained" disabled={isSubmitting} aria-busy={isSubmitting}>
            {isSubmitting ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Thêm địa chỉ'}
          </Button>
        </DialogActions>
      </Stack>
    </Dialog>
  )
}
