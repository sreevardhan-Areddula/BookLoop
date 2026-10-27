# BookLoop UI assets

## Community hero image

`frontend/public/bookloop-community.png` is the generated community illustration shown in the Browse books introduction. Vite serves files from `public` at the site root, so the page references it as `/bookloop-community.png`.

The artwork is decorative and does not contain text. Its alternative text describes the scene for users who cannot see the image. The image is cropped to fit the responsive intro panel; on narrow screens it moves below the heading.

`frontend/public/bookloop-background.png` is a low-contrast, repeating book-and-leaf pattern behind the Browse page. A translucent page-color overlay keeps the pattern quiet so text and white cards remain easy to read.

Book cover images are not included in the current listing model. When book photo uploads are added, listings can display member-provided cover images instead of relying on generic artwork.
