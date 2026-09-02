import { Alert, Button, Card, CardContent, Stack, TextField } from '@mui/material'
import { PageIntro } from '../../components/common/PageIntro'

export function CheckoutPage() {
  return (
    <>
      <PageIntro title="Thanh toán" description="Cung cấp thông tin giao hàng và kiểm tra đơn trước khi xác nhận." />
      <Alert severity="info" sx={{ mb: 2 }}>Đây là khung giao diện checkout. Logic đặt hàng sẽ được bổ sung trong story nghiệp vụ.</Alert>
      <Card><CardContent><Stack spacing={2}><TextField label="Họ và tên" /><TextField label="Số điện thoại" /><TextField label="Địa chỉ giao hàng" multiline minRows={2} /><Button variant="contained" disabled>Xác nhận đặt hàng</Button></Stack></CardContent></Card>
    </>
  )
}
