# Gallery
> List the configured sources, show the wallpaper cards of the chosen one, and put the previewed wallpaper on every screen.

## Boundary
- `browse-sources` — list all sources and show the chosen one's wallpapers
- `choose-source` — show another source's section
- `load-more` — append the next page of a source's wallpapers
- `preview-wallpaper` — look at a card's wallpaper large and step through the shown cards
- `apply-wallpaper` — put the previewed wallpaper on all screens
- `filter-by-season` — choose the season a seasonal section shows
- `filter-by-resolution` — show only cards whose original is at least a minimum width

## Requirements
### R1: Browse sources
- R1.1 — When the gallery opens, the BC shall list every source by title in configured order and show the first source's section with its title and description.
- R1.2 — When a source's section is shown for the first time, the BC shall fill it with cards for the first page of its wallpapers, each showing the preview and the original's resolution.
- R1.3 — Where a wallpaper comes from Unsplash, the BC shall show the photographer's name on its card, linked to their profile.
- R1.4 — While a section's wallpapers are loading, the BC shall show a loading indication in that section.
- R1.5 — If a source is invalid or unsupported, or its wallpapers fail to load, then the BC shall show the reason inside that source's section and keep every other source choosable.
- R1.6 — If the sources cannot be listed at all, then the BC shall show a single error with the reason, naming the sources file.
- R1.7 — When a source is chosen in the list, the BC shall show its section in place of the previous one and mark it as chosen in the list.
- R1.8 — While a section is shown, the BC shall show how many cards it shows.

### R2: Load more
- R2.1 — When more wallpapers of a section are requested, the BC shall append the next page's cards to that section.
- R2.2 — While a section has no further page, the BC shall not offer to load more for it.

### R3: Apply a wallpaper
- R3.1 — When setting the previewed wallpaper is requested, the BC shall request it be applied, close the preview and mark its card as applying.
- R3.2 — When applying succeeds, the BC shall mark that card as in use and remove the mark from any previously applied card.
- R3.3 — If applying fails, then the BC shall show the reason on that card.
- R3.4 — While a wallpaper is being applied, the BC shall prevent setting another.
- R3.5 — The BC shall let every card and every preview control be used by keyboard as well as by pointer.
- R3.6 — When applying succeeds, the BC shall confirm that the wallpaper was set on every screen and show it as the current wallpaper beside the source list.

### R4: Filter by season
- R4.1 — Where a source is seasonal, the BC shall offer a season filter in its section with the choices all seasons, spring, summer, autumn and winter.
- R4.2 — When the gallery opens, the BC shall preselect the current meteorological season of the northern hemisphere: spring March to May, summer June to August, autumn September to November, winter December to February.
- R4.3 — When a season is chosen, the BC shall replace the section's cards with the first page of wallpapers for that season.
- R4.4 — While a season is chosen, when more wallpapers are requested, the BC shall load the next page for that season.
- R4.5 — When all seasons is chosen, the BC shall show the source's wallpapers unfiltered.

### R5: Preview a wallpaper
- R5.1 — When a card is chosen, the BC shall open a preview of its wallpaper showing the resolution, its position among the section's shown cards and, where it comes from Unsplash, "Photo by <name> on Unsplash" linked to the photographer and to Unsplash.
- R5.2 — When the next or previous wallpaper is requested in the preview, the BC shall preview the adjacent shown card, wrapping around at either end.
- R5.3 — While the preview is open, the BC shall request the next wallpaper on the right arrow key, the previous one on the left arrow key, and close the preview on Escape.
- R5.4 — When the preview is closed, the BC shall show the section as it was.

### R6: Filter by resolution
- R6.1 — The BC shall offer a resolution filter with the choices any, 4K+, 5K+ and 6K+, with any preselected.
- R6.2 — When a minimum resolution is chosen, the BC shall show only the cards whose original is at least 3840, 5120 or 6000 pixels wide respectively, in every section.

## Entities
- Source, Wallpaper

## Out of scope
- Screen selection, a lock-screen target and fit modes.
- Favorites, a history of used wallpapers, and searching by photographer.
- A preview image larger than the provider's preview size.
- Editing sources.
- Remembering the applied wallpaper across reloads.
