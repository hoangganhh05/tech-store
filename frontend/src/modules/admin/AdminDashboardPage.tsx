import { Card, CardContent, Grid, Typography } from '@mui/material'
import { PageIntro } from '../../components/common/PageIntro'

export function AdminDashboardPage() {
  return <><PageIntro eyebrow="Quản trị" title="Tổng quan" description="Theo dõi nhanh tình trạng hoạt động của cửa hàng." /><Grid container spacing={2}>{[['Đơn hàng hôm nay', '0'], ['Doanh thu hôm nay', '0 ₫'], ['Sản phẩm sắp hết', '0']].map(([label, value]) => <Grid key={label} size={{ xs: 12, md: 4 }}><Card><CardContent><Typography color="text.secondary">{label}</Typography><Typography variant="h2" mt={1}>{value}</Typography></CardContent></Card></Grid>)}</Grid></>
}
