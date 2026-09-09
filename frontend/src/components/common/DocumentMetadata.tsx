import { useEffect } from 'react'
import { env } from '../../configs/env'

type DocumentMetadataProps = {
  section?: string
}

function setMetaAttribute(
  selector: string,
  attribute: 'name' | 'property',
  value: string,
  content: string,
) {
  let meta = document.head.querySelector<HTMLMetaElement>(selector)
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute(attribute, value)
    document.head.append(meta)
  }
  meta.content = content
}

export function DocumentMetadata({ section }: DocumentMetadataProps) {
  useEffect(() => {
    const title = section ? `${section} | ${env.brand.name}` : env.brand.name
    const description = `${env.brand.name} - ${env.brand.industry}`

    document.title = title
    setMetaAttribute('meta[name="description"]', 'name', 'description', description)
    setMetaAttribute('meta[property="og:title"]', 'property', 'og:title', title)
    setMetaAttribute(
      'meta[property="og:description"]',
      'property',
      'og:description',
      description,
    )
    setMetaAttribute(
      'meta[name="twitter:title"]',
      'name',
      'twitter:title',
      title,
    )
    setMetaAttribute(
      'meta[name="twitter:description"]',
      'name',
      'twitter:description',
      description,
    )
  }, [section])

  return null
}
