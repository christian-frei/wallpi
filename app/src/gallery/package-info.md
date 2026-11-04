# Gallery
> Show every configured source as a section of wallpaper cards and apply the card that is chosen.

## Boundary
- `browse-sources` — show all sources with the first page of their wallpapers
- `load-more` — append the next page of a source's wallpapers
- `apply-wallpaper` — put the chosen card's wallpaper on all screens

## Requirements
### R1: Browse sources
- R1.1 — When the gallery opens, the BC shall show one section per source with its title and description, in configured order.
- R1.2 — When a source's section is shown, the BC shall fill it with cards for the first page of its wallpapers, each showing the preview and the original's resolution.
- R1.3 — Where a wallpaper comes from Unsplash, the BC shall show the photographer's name on its card, linked to their profile.
- R1.4 — While a section's wallpapers are loading, the BC shall show a loading indication in that section.
- R1.5 — If a source is invalid or unsupported, or its wallpapers fail to load, then the BC shall show the reason inside that source's section and keep showing all other sections.
- R1.6 — If the sources cannot be listed at all, then the BC shall show a single error with the reason, naming the sources file.

### R2: Load more
- R2.1 — When more wallpapers of a section are requested, the BC shall append the next page's cards to that section.
- R2.2 — While a section has no further page, the BC shall not offer to load more for it.

### R3: Apply a wallpaper
- R3.1 — When a card is chosen, the BC shall request the wallpaper be applied and mark that card as applying.
- R3.2 — When applying succeeds, the BC shall mark that card as applied and remove the mark from any previously applied card.
- R3.3 — If applying fails, then the BC shall show the reason on that card.
- R3.4 — While a wallpaper is being applied, the BC shall prevent choosing another card.
- R3.5 — The BC shall let every card be chosen by keyboard as well as by pointer.

## Entities
- Source, Wallpaper

## Out of scope
- Screen selection.
- Editing sources.
- Remembering the applied wallpaper across reloads.
