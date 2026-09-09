const removeTrailingSlash = (value: string) => value.replace(/\/$/, '')
const optionalText = (value: string | undefined) => value?.trim() ?? ''

export const env = Object.freeze({
  brand: Object.freeze({
    name: import.meta.env.VITE_APP_NAME || 'Đăng Tùng Mobile',
    industry:
      import.meta.env.VITE_BRAND_INDUSTRY || 'Kinh doanh phụ kiện điện thoại.',
    address:
      import.meta.env.VITE_BRAND_ADDRESS ||
      'Xóm Lê Lợi, Thôn Cập Thượng, Phường Nam Đồng, TP Hải Phòng',
    contact: Object.freeze({
      phone: import.meta.env.VITE_BRAND_PHONE || '0867116863',
      phoneHref: import.meta.env.VITE_BRAND_PHONE_HREF || 'tel:+84867116863',
      email: import.meta.env.VITE_BRAND_EMAIL || 'hoanghd064@gmail.com',
      emailHref:
        import.meta.env.VITE_BRAND_EMAIL_HREF ||
        'mailto:hoanghd064@gmail.com',
    }),
    // Keep unknown public details blank. UI must only render them when supplied.
    optional: Object.freeze({
      openingHours: optionalText(import.meta.env.VITE_BRAND_OPENING_HOURS),
      zaloUrl: optionalText(import.meta.env.VITE_BRAND_ZALO_URL),
      socialUrl: optionalText(import.meta.env.VITE_BRAND_SOCIAL_URL),
      mapUrl: optionalText(import.meta.env.VITE_BRAND_MAP_URL),
      warrantyPolicyUrl: optionalText(
        import.meta.env.VITE_BRAND_WARRANTY_POLICY_URL,
      ),
    }),
  }),
  apiBaseUrl: removeTrailingSlash(
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  ),
})
