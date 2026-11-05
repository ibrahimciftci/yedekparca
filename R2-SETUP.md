# Cloudflare R2 configuration

Product photos are uploaded to the backend as multipart files. The backend validates JPG, PNG, and WebP signatures/content and a 5 MiB per-file limit before writing the object. Only random `offers/<uuid>.<ext>` object keys are stored. The browser never receives R2 credentials or signed storage URLs.

Set these environment variables before starting the backend:

- `R2_ENDPOINT`: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`
- `R2_REGION`: `auto`
- `R2_BUCKET`: bucket name
- `R2_ACCESS_KEY_ID`: access key scoped to this bucket
- `R2_SECRET_ACCESS_KEY`: matching secret
- `R2_PUBLIC_BASE_URL`: HTTPS custom domain serving this bucket, with no trailing slash
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL connection
- `APP_JWT_SECRET`: Base64 encoded signing secret (at least 256 bits)

In Cloudflare, create an R2 bucket, create an S3 API token limited to that bucket with object read/write/delete access, and connect a dedicated custom domain for public product images. Keep the S3 endpoint and keys private. The application uploads through its authenticated API, so R2 bucket CORS is not needed for browser PUT requests. Do not use the development `r2.dev` domain for production.

The app stores the public object URL alongside the key so customers can load images. Existing `products.image_url` values remain available as the legacy image fallback. A live R2 integration was not verified without bucket credentials.
