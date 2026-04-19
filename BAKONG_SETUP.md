# Bakong / KHQR integration (how this project works)

This project uses the **KHQR JavaScript SDK** on the **frontend** to generate:
- KHQR string (`qr`)
- MD5 hash (`md5`)

Then the backend uses **Bakong Open API** to check payment status by `md5`:

- `POST {baseUrl}/v1/check_transaction_by_md5` with header `Authorization: Bearer <token>`.

References:
- Bakong Open API list includes `/v1/check_transaction_by_md5`. (Bakong Open API Document)
- QR integration flow recommends generating KHQR via SDK, then checking transaction status by MD5. (Bakong QR Pay Integration document)
- KHQR JS SDK usage: `new BakongKHQR().generateMerchant(...)` returns `{ qr, md5 }`. (KHQR SDK Document)

## Env var

Set your token:

- Windows (PowerShell): `$env:BAKONG_API_TOKEN="YOUR_TOKEN"`
- Linux/Mac: `export BAKONG_API_TOKEN="YOUR_TOKEN"`

## Backend endpoints

- `GET /api/bakong/merchant` → merchant info for KHQR generation
- `POST /api/bakong/check-md5` body: `{ "md5": "..." }` → checks status from Bakong
