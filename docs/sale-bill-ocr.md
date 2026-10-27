# Private sale-bill upload and OCR price check

This backend slice stores a member's purchase bill as private MySQL `MEDIUMBLOB` data and uses local English Tesseract OCR to compare a proposed sale price with a clearly recognized paid amount. It does not expose a public file URL and it does not claim that OCR authenticates a bill.

## Upload limits

- JPEG, PNG, and PDF files are accepted, up to 5 MB per file.
- PDF files must contain 1 to 10 pages.
- The backend checks the file signature; it does not trust only the filename or browser MIME type.
- Raw OCR text is not persisted. The database stores the uploaded bytes and, after a check, only the extracted paid amount and result status.
- Only the bill owner can run the price check. No download or public retrieval route is provided in this task.

## Local English Tesseract setup

The backend uses Tess4J's local Tesseract engine and Apache PDFBox for PDF page rendering. On Windows, Tess4J bundles the native OCR engine and English language data; it extracts them to a temporary local directory when OCR is first used. No bill is sent to a third-party OCR service.

On Windows, the Microsoft Visual C++ v14 Redistributable is required by Tess4J's native libraries. If BookLoop reports that the native library could not load, install that runtime and restart the backend. See the [Tess4J setup notes](https://github.com/nguyenq/tess4j#dependencies).

An alternate English data folder can be configured with `BOOKLOOP_TESSDATA_PATH` in the ignored `backend/.env.local` file. The folder must contain `eng.traineddata`. This is optional on Windows when the bundled data is available.

On Linux deployments, install the platform's Tesseract native library and configure English language data; deployment setup will be documented after hosting is selected.

## API

Both endpoints require the member bearer token.

### Upload a private bill

`POST /api/sale-bills` with `multipart/form-data` field `file`.

Returns `201` and safe metadata including the opaque `id`, sanitized filename, media type, size, and `UPLOADED` status. It does not return the uploaded bytes.

### Check a sale price

`POST /api/sale-bills/{id}/check?askingPrice=350.00`

The owner-only check returns one of:

- `PRICE_CHECK_PASSED`: the asking price is strictly below the paid amount read by OCR.
- `PRICE_NOT_BELOW_PAID_AMOUNT`: the asking price is equal to or greater than the paid amount.
- `UNREADABLE`: OCR did not find one unique paid total, so no price-check tick is granted.

The parser recognizes labeled totals such as “grand total,” “amount paid,” “net payable,” and “order total.” It requires exactly one unique labeled paid amount. Unlabeled amounts are never guessed to be the amount paid, because they could be an MRP or another figure on the bill.

If English language data or the native OCR runtime is not available, the check returns a service-unavailable response with setup guidance. An OCR pass only checks the numeric comparison; the buyer must still verify the bill's authenticity.

## Sale listing integration

Sale listings may be published without a bill and are labeled unverified. If a bill is included at creation, its OCR price check must pass before the listing is saved. An owner can later add a bill in My Books; only a bill belonging to that owner with a passed result and a detected paid amount above the listing price can set the OCR tick. Sale listing responses include `salePriceVerified`; no bill bytes or public bill URL are returned. The API to attach a passed bill is `PUT /api/books/{id}/sale-verification` with `saleBillDocumentId`.

The OCR tick means only that the detected paid amount was greater than the asking price. It does not verify bill authenticity. Sale interest creation and seller decisions are documented in [sale-interest-workflow.md](sale-interest-workflow.md). Private buyer/seller chat, redacted bill sharing, and manual sold marking remain separate marketplace work.
