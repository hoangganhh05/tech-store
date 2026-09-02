import { Button, Card, CardContent, Typography } from '@mui/material'
import { PageIntro } from '../../components/common/PageIntro'

export function AdminProductsPage() {
  return <><PageIntro eyebrow="Quản trị" title="Quản lý sản phẩm" description="Thêm, cập nhật và theo dõi danh mục sản phẩm." action={<Button variant="contained">Thêm sản phẩm</Button>} /><Card><CardContent><Typography color="text.secondary">Chưa có dữ liệu sản phẩm.</Typography></CardContent></Card></>
}
