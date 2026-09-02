import { Box, Button, Card, CardContent, Grid, Stack, Typography } from '@mui/material'
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined'
import SecurityOutlinedIcon from '@mui/icons-material/SecurityOutlined'
import SupportAgentOutlinedIcon from '@mui/icons-material/SupportAgentOutlined'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { ProductPlaceholder } from '../../components/common/ProductPlaceholder'

export function HomePage() {
  return (
    <Stack spacing={5}>
      <Box bgcolor="#fff1f1" border="1px solid #ffcdd2" borderRadius={3} p={{ xs: 3, md: 6 }}>
        <Typography color="primary" fontWeight={700} mb={1}>TechStore chính hãng</Typography>
        <Typography component="h1" variant="h1" maxWidth={620}>Thiết bị công nghệ phù hợp cho nhu cầu mỗi ngày</Typography>
        <Typography color="text.secondary" mt={2} mb={3} maxWidth={600}>Khám phá điện thoại và phụ kiện với thông tin rõ ràng, giao hàng thuận tiện.</Typography>
        <Button component={Link} to={ROUTES.products} variant="contained" size="large">Xem sản phẩm</Button>
      </Box>
      <Box>
        <Typography component="h2" variant="h2" mb={2}>Sản phẩm nổi bật</Typography>
        <Grid container spacing={2}>
          {['Điện thoại mới', 'Phụ kiện thiết yếu', 'Sản phẩm bán chạy'].map((name) => (
            <Grid key={name} size={{ xs: 12, sm: 4 }}>
              <Card><ProductPlaceholder /><CardContent><Typography fontWeight={700}>{name}</Typography><Typography color="primary" mt={1}>Xem chi tiết</Typography></CardContent></Card>
            </Grid>
          ))}
        </Grid>
      </Box>
      <Grid container spacing={2}>
        {[
          [<LocalShippingOutlinedIcon />, 'Giao hàng thuận tiện', 'Theo dõi trạng thái đơn hàng rõ ràng.'],
          [<SecurityOutlinedIcon />, 'Sản phẩm chính hãng', 'Thông tin sản phẩm minh bạch.'],
          [<SupportAgentOutlinedIcon />, 'Hỗ trợ khi cần', 'Dễ dàng liên hệ với cửa hàng.'],
        ].map(([icon, title, text]) => (
          <Grid key={String(title)} size={{ xs: 12, md: 4 }}><Card><CardContent><Box color="primary.main">{icon}</Box><Typography variant="h3" mt={1}>{title}</Typography><Typography color="text.secondary" mt={1}>{text}</Typography></CardContent></Card></Grid>
        ))}
      </Grid>
    </Stack>
  )
}
