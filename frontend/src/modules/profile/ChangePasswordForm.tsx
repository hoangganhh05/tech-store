import { Alert, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material'
import { isAxiosError } from 'axios'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuth } from '../../hooks/useAuth'
import { changeMyPassword, type ChangePasswordPayload } from '../../services/userService'

type ChangePasswordField = keyof ChangePasswordPayload
type FieldErrors = Partial<Record<ChangePasswordField, string>>

const initialValues: ChangePasswordPayload = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
}

export function ChangePasswordForm() {
  const navigate = useNavigate()
  const { clearSession } = useAuth()
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (field: ChangePasswordField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setSubmitError('')
  }

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    if (!values.currentPassword) errors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.'
    if (values.newPassword.length < 8 || values.newPassword.length > 72) {
      errors.newPassword = 'Mật khẩu mới phải có từ 8 đến 72 ký tự.'
    } else if (values.newPassword === values.currentPassword) {
      errors.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại.'
    }
    if (!values.confirmPassword) errors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới.'
    else if (values.newPassword !== values.confirmPassword) errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
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
      await changeMyPassword(values)
      clearSession()
      navigate(ROUTES.login, {
        replace: true,
        state: { passwordChangedMessage: 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới.' },
      })
    } catch (error: unknown) {
      const response = isAxiosError<{ code?: string; message?: string }>(error) ? error.response?.data : undefined
      if (response?.code === 'INVALID_CURRENT_PASSWORD') {
        setFieldErrors({ currentPassword: response.message || 'Mật khẩu hiện tại không đúng.' })
      } else if (response?.code === 'PASSWORD_MUST_BE_DIFFERENT') {
        setFieldErrors({ newPassword: response.message || 'Mật khẩu mới phải khác mật khẩu hiện tại.' })
      } else {
        setSubmitError(response?.message || 'Không thể đổi mật khẩu lúc này. Vui lòng thử lại sau.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card sx={{ maxWidth: 680, width: '100%', mx: 'auto' }}>
      <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
        <Typography component="h2" variant="h4" mb={1}>Đổi mật khẩu</Typography>
        <Typography color="text.secondary" mb={3}>Sau khi đổi mật khẩu, bạn cần đăng nhập lại.</Typography>
        <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
          {submitError && <Alert severity="error">{submitError}</Alert>}
          <TextField label="Mật khẩu hiện tại" type="password" value={values.currentPassword} onChange={handleChange('currentPassword')} error={Boolean(fieldErrors.currentPassword)} helperText={fieldErrors.currentPassword} required autoComplete="current-password" disabled={isSubmitting} />
          <TextField label="Mật khẩu mới" type="password" value={values.newPassword} onChange={handleChange('newPassword')} error={Boolean(fieldErrors.newPassword)} helperText={fieldErrors.newPassword || 'Từ 8 đến 72 ký tự.'} required autoComplete="new-password" disabled={isSubmitting} />
          <TextField label="Xác nhận mật khẩu mới" type="password" value={values.confirmPassword} onChange={handleChange('confirmPassword')} error={Boolean(fieldErrors.confirmPassword)} helperText={fieldErrors.confirmPassword} required autoComplete="new-password" disabled={isSubmitting} />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} aria-busy={isSubmitting}>
            {isSubmitting ? 'Đang đổi mật khẩu...' : 'Đổi mật khẩu'}
          </Button>
        </Stack>
      </CardContent>
    </Card>
  )
}
