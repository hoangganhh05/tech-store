import { useEffect, useState } from 'react'
import { Alert, Button, CircularProgress, FormControlLabel, Radio, RadioGroup, Stack } from '@mui/material'
import { getPaymentMethods, selectPaymentMethod, type PaymentMethod, type PaymentOption } from '../../services/checkoutService'

type Props = {
  selected: PaymentOption | null
  onContinue: (option: PaymentOption) => void
  onBack: () => void
  onSelectionChange?: (option: PaymentOption) => void
}

export function PaymentMethodStep({ selected, onContinue, onBack, onSelectionChange }: Props) {
  const [methods, setMethods] = useState<PaymentOption[]>([])
  const [method, setMethod] = useState<PaymentMethod | ''>(selected?.paymentMethod ?? '')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    getPaymentMethods().then((options) => {
      if (active) {
        setMethods(options)
        if (options.length === 0) setError('Chưa có phương thức thanh toán. Vui lòng thử lại.')
      }
    }).catch(() => {
      if (active) setError('Không thể tải phương thức thanh toán. Vui lòng thử lại.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [attempt])

  const option = methods.find((item) => item.paymentMethod === method)
  const submit = async () => {
    if (!option || saving) return
    setSaving(true)
    setError(null)
    try {
      const confirmed = await selectPaymentMethod(option.paymentMethod)
      onContinue(confirmed)
    } catch {
      setError('Không thể xác nhận phương thức thanh toán. Vui lòng thử lại.')
    } finally {
      setSaving(false)
    }
  }

  return <Stack spacing={2}>
    {loading && <Stack direction="row" spacing={2} alignItems="center"><CircularProgress size={24} /><span>Đang tải phương thức thanh toán...</span></Stack>}
    {error && <Alert severity="error" action={methods.length === 0 && !loading ? <Button onClick={() => {
      setLoading(true)
      setError(null)
      setAttempt((value) => value + 1)
    }}>Thử lại</Button> : undefined}>{error}</Alert>}
    {!loading && <RadioGroup aria-label="Phương thức thanh toán" value={method} onChange={(event) => {
      setMethod(event.target.value as PaymentMethod)
      const next = methods.find((item) => item.paymentMethod === event.target.value)
      if (next) onSelectionChange?.(next)
      setError(null)
    }}>
      {methods.map((item) => <FormControlLabel key={item.paymentMethod} value={item.paymentMethod}
        control={<Radio />} label={item.label} disabled={saving} sx={{ m: 0, minHeight: 48 }} />)}
    </RadioGroup>}
    {option && <Alert severity="info">{option.instructions}</Alert>}
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between">
      <Button variant="outlined" disabled={saving} onClick={onBack} data-testid="back-to-address-btn" fullWidth sx={{ width: { sm: 'auto' } }}>Quay lại chọn địa chỉ</Button>
      <Button variant="contained" disabled={loading || saving || !option} onClick={submit} fullWidth sx={{ width: { sm: 'auto' } }}>
        {saving ? 'Đang xác nhận...' : 'Tiếp tục xem lại đơn hàng'}
      </Button>
    </Stack>
  </Stack>
}
