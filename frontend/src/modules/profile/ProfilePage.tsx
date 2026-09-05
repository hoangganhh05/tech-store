import { Alert, Button, Card, CardContent, CircularProgress, Stack, TextField, Typography } from '@mui/material'
import { isAxiosError } from 'axios'
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { getMyProfile, updateMyProfile, type UpdateProfilePayload } from '../../services/userService'

type ProfileField = keyof UpdateProfilePayload
type FieldErrors = Partial<Record<ProfileField, string>>

const phonePattern = /^[0-9+() .-]{7,20}$/
const emptyValues: UpdateProfilePayload = { fullName: '', phone: '', dateOfBirth: null }

export function ProfilePage() {
  const { updateUserProfile } = useAuth()
  const [email, setEmail] = useState('')
  const [values, setValues] = useState<UpdateProfilePayload>(emptyValues)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [loadError, setLoadError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    getMyProfile()
      .then((profile) => {
        if (!active) return
        setEmail(profile.email)
        setValues({ fullName: profile.fullName, phone: profile.phone, dateOfBirth: profile.dateOfBirth })
      })
      .catch(() => {
        if (active) setLoadError('Không thể tải thông tin cá nhân. Vui lòng thử lại.')
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => { active = false }
  }, [reloadKey])

  const retryLoad = () => {
    setIsLoading(true)
    setLoadError('')
    setReloadKey((key) => key + 1)
  }

  const handleChange = (field: ProfileField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value || (field === 'dateOfBirth' ? null : '') }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
    setSuccessMessage('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (!values.fullName.trim()) errors.fullName = 'Vui lòng nhập họ tên.'
    else if (values.fullName.trim().length > 150) errors.fullName = 'Họ tên không được vượt quá 150 ký tự.'
    if (!values.phone.trim()) errors.phone = 'Vui lòng nhập số điện thoại.'
    else if (!phonePattern.test(values.phone.trim())) errors.phone = 'Số điện thoại không đúng định dạng.'
    if (values.dateOfBirth && values.dateOfBirth >= new Date().toISOString().slice(0, 10)) {
      errors.dateOfBirth = 'Ngày sinh phải là một ngày trong quá khứ.'
    }
    return errors
  }

  const parseServerErrors = (message: string): FieldErrors => {
    const errors: FieldErrors = {}
    message.split(';').forEach((part) => {
      const separator = part.indexOf(':')
      if (separator < 0) return
      const field = part.slice(0, separator).trim() as ProfileField
      if (field in emptyValues) errors[field] = part.slice(separator + 1).trim()
    })
    return errors
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const errors = validate()
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setIsSubmitting(true)
    setSubmitError('')
    setSuccessMessage('')
    try {
      const updated = await updateMyProfile({
        fullName: values.fullName.trim(),
        phone: values.phone.trim(),
        dateOfBirth: values.dateOfBirth || null,
      })
      setValues({ fullName: updated.fullName, phone: updated.phone, dateOfBirth: updated.dateOfBirth })
      updateUserProfile({ fullName: updated.fullName, phone: updated.phone })
      setSuccessMessage('Cập nhật thông tin cá nhân thành công.')
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error) ? error.response?.data?.message : undefined
      const serverErrors = message ? parseServerErrors(message) : {}
      if (Object.keys(serverErrors).length > 0) setFieldErrors(serverErrors)
      else setSubmitError(message || 'Không thể cập nhật thông tin lúc này. Vui lòng thử lại sau.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <Stack alignItems="center" py={8} spacing={2}><CircularProgress /><Typography>Đang tải thông tin cá nhân...</Typography></Stack>
  }

  if (loadError) {
    return <Stack alignItems="center" py={8} spacing={2}><Alert severity="error">{loadError}</Alert><Button variant="outlined" onClick={retryLoad}>Thử lại</Button></Stack>
  }

  return (
    <Card sx={{ maxWidth: 680, mx: 'auto' }}>
      <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
        <Typography component="h1" variant="h2" mb={1}>Thông tin cá nhân</Typography>
        <Typography color="text.secondary" mb={3}>Xem và cập nhật thông tin tài khoản của bạn.</Typography>
        <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
          {successMessage && <Alert severity="success">{successMessage}</Alert>}
          {submitError && <Alert severity="error">{submitError}</Alert>}
          <TextField label="Email" type="email" value={email} disabled helperText="Email không thể thay đổi trực tiếp." />
          <TextField label="Họ tên" value={values.fullName} onChange={handleChange('fullName')} error={Boolean(fieldErrors.fullName)} helperText={fieldErrors.fullName} required autoComplete="name" />
          <TextField label="Số điện thoại" value={values.phone} onChange={handleChange('phone')} error={Boolean(fieldErrors.phone)} helperText={fieldErrors.phone} required autoComplete="tel" />
          <TextField label="Ngày sinh" type="date" value={values.dateOfBirth ?? ''} onChange={handleChange('dateOfBirth')} error={Boolean(fieldErrors.dateOfBirth)} helperText={fieldErrors.dateOfBirth || 'Không bắt buộc.'} slotProps={{ inputLabel: { shrink: true } }} />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
            {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
          </Button>
        </Stack>
      </CardContent>
    </Card>
  )
}
