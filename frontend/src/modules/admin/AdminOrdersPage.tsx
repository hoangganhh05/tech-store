import { Card, CardContent, Typography } from '@mui/material'
import { PageIntro } from '../../components/common/PageIntro'

export function AdminOrdersPage() {
  return <><PageIntro eyebrow="Quản trị" title="Quản lý đơn hàng" description="Theo dõi và cập nhật trạng thái xử lý đơn hàng." /><Card><CardContent><Typography color="text.secondary">Chưa có dữ liệu đơn hàng.</Typography></CardContent></Card></>
}
