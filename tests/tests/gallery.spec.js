// @ts-check
// The `wallpapers` and `desktop` server boundaries are stubbed with page.route:
// the system under test is the `gallery` frontend only.
const { test, expect } = require('@playwright/test');

const PREVIEW = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="16" height="10"/>';

const SOURCES = [
  { id: 'trek', title: 'Star Trek', description: 'Starships and the final frontier', seasonal: false, status: 'valid' },
  { id: 'fjords', title: 'Nature on Unsplash', description: 'Fjords and forests', seasonal: true, status: 'valid' },
  { id: 'broken', title: 'No Url', description: 'forgot the url', seasonal: false, status: 'invalid', reason: 'missing url' },
  { id: 'elsewhere', title: 'Elsewhere', description: 'not an image API', seasonal: false, status: 'unsupported',
    reason: 'unsupported host example.com, expected wallhaven.cc or api.unsplash.com' },
];

const wallpaper = (id, resolution = '3840x2160') => ({ id, provider: 'wallhaven', preview: PREVIEW, resolution });

const PAGES = {
  trek: [
    { page: 1, hasMore: true, wallpapers: [wallpaper('enterprise'), wallpaper('voyager', '2560x1440')] },
    { page: 2, hasMore: false, wallpapers: [wallpaper('defiant', '5120x2880')] },
  ],
  fjords: [
    { page: 1, hasMore: false, wallpapers: [{
      ...wallpaper('fjord', '6000x4000'), provider: 'unsplash',
      photographer: { name: 'Jean-Luc Picard', profile: 'https://unsplash.com/@picard' },
    }] },
  ],
};

const gate = () => {
  let open;
  const opened = new Promise(resolve => open = resolve);
  return { opened, open };
};

const json = (route, status, body) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });

/** Two Unsplash wallpapers per page whose photographer names the season, so a page shows which filter it came from. */
const seasonalPage = (season, number) => ({
  page: number,
  hasMore: number < 2,
  wallpapers: [1, 2].map(n => ({
    ...wallpaper(`${season || 'all'}-${number}-${n}`, '6000x4000'), provider: 'unsplash',
    photographer: { name: `${season || 'all-season'} photographer ${number}.${n}`, profile: 'https://unsplash.com/@lcars' },
  })),
});

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ sources?: (route: any) => Promise<void>, failing?: Record<string, string>, pageGate?: Promise<void>,
 *           apply?: (route: any) => Promise<void>, seasonal?: boolean, requests?: URLSearchParams[] }} [options]
 */
const backend = async (page, options = {}) => {
  const applied = [];
  await page.route(url => url.pathname.startsWith('/sources') || url.pathname.startsWith('/desktop'), async route => {
    const url = new URL(route.request().url());
    if (url.pathname === '/sources') {
      return options.sources ? options.sources(route) : json(route, 200, SOURCES);
    }
    const wallpapers = url.pathname.match(/^\/sources\/([^/]+)\/wallpapers$/);
    if (wallpapers) {
      const [, id] = wallpapers;
      const number = Number(url.searchParams.get('page'));
      if (id === 'fjords') options.requests?.push(url.searchParams);
      if (options.pageGate && id === 'trek') await options.pageGate;
      if (options.failing?.[id]) return json(route, 502, { reason: options.failing[id] });
      if (options.seasonal && id === 'fjords') return json(route, 200, seasonalPage(url.searchParams.get('season'), number));
      return json(route, 200, PAGES[id][number - 1]);
    }
    if (url.pathname === '/desktop/wallpaper' && route.request().method() === 'PUT') {
      applied.push(route.request().postDataJSON());
      return options.apply ? options.apply(route) : json(route, 200, { file: '/Users/picard/Pictures/wallpi/x.jpg' });
    }
    return route.fallback();
  });
  return applied;
};

const section = (page, title) => page.getByRole('region', { name: title });
const card = (page, title, number, resolution) =>
  section(page, title).getByRole('button', { name: `Apply ${title} wallpaper ${number}, ${resolution}` });

test.describe('R1: Browse sources', () => {

  test('R1.1 — shows one section per source with title and description, in configured order', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 2 })).toHaveText(['Star Trek', 'Nature on Unsplash', 'No Url', 'Elsewhere']);
    await expect(section(page, 'Star Trek')).toContainText('Starships and the final frontier');
    await expect(section(page, 'Elsewhere')).toContainText('not an image API');
  });

  test('R1.2 — fills a section with cards of its first page, showing preview and resolution', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    await expect(section(page, 'Star Trek').getByRole('button', { name: /^Apply Star Trek wallpaper/ })).toHaveCount(2);
    await expect(card(page, 'Star Trek', 1, '3840x2160')).toContainText('3840x2160');
    await expect(card(page, 'Star Trek', 2, '2560x1440')).toContainText('2560x1440');
    await expect(card(page, 'Star Trek', 1, '3840x2160').locator('img')).toHaveAttribute('src', PREVIEW);
  });

  test('R1.3 — shows the Unsplash photographer, linked to their profile', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    const photographer = section(page, 'Nature on Unsplash').getByRole('link', { name: 'Jean-Luc Picard' });
    await expect(photographer).toHaveAttribute('href', /^https:\/\/unsplash\.com\/@picard\?/);
    await expect(section(page, 'Nature on Unsplash')).toContainText('Photo by Jean-Luc Picard on Unsplash');
    await expect(section(page, 'Star Trek')).not.toContainText('Photo by');
  });

  test('R1.4 — shows a loading indication while a section loads', async ({ page }) => {
    const pageLoad = gate();
    await backend(page, { pageGate: pageLoad.opened });
    await page.goto('/');
    await expect(section(page, 'Star Trek').getByRole('status')).toHaveText('Loading wallpapers…');
    pageLoad.open();
    await expect(card(page, 'Star Trek', 1, '3840x2160')).toBeVisible();
    await expect(section(page, 'Star Trek').getByRole('status')).toHaveCount(0);
  });

  test('R1.5 — shows the reason inside a failing, invalid or unsupported section and keeps the others', async ({ page }) => {
    await backend(page, { failing: { trek: "source 'Star Trek' is unreachable: java.net.ConnectException" } });
    await page.goto('/');
    await expect(section(page, 'Star Trek').getByRole('alert')).toHaveText("source 'Star Trek' is unreachable: java.net.ConnectException");
    await expect(section(page, 'No Url').getByRole('alert')).toHaveText('missing url');
    await expect(section(page, 'Elsewhere').getByRole('alert')).toHaveText('unsupported host example.com, expected wallhaven.cc or api.unsplash.com');
    await expect(card(page, 'Nature on Unsplash', 1, '6000x4000')).toBeVisible();
  });

  test('R1.6 — shows a single error naming the sources file when sources cannot be listed', async ({ page }) => {
    const reason = 'sources file /Users/picard/.wallpi/sources.json is not valid JSON: Unexpected char';
    await backend(page, { sources: route => json(route, 500, { reason }) });
    await page.goto('/');
    await expect(page.getByRole('alert')).toHaveText(`Could not list the sources in ~/.wallpi/sources.json: ${reason}`);
    await expect(page.getByRole('heading', { level: 2 })).toHaveCount(0);
  });
});

test.describe('R2: Load more', () => {

  test('R2.1 — appends the next page to the section', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    await section(page, 'Star Trek').getByRole('button', { name: 'Load more Star Trek wallpapers' }).click();
    await expect(card(page, 'Star Trek', 3, '5120x2880')).toBeVisible();
    await expect(section(page, 'Star Trek').getByRole('button', { name: /^Apply Star Trek wallpaper/ })).toHaveCount(3);
  });

  test('R2.2 — offers no load more for a section without a further page', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    await expect(card(page, 'Nature on Unsplash', 1, '6000x4000')).toBeVisible();
    await expect(section(page, 'Nature on Unsplash').getByRole('button', { name: /^Load more/ })).toHaveCount(0);
    await section(page, 'Star Trek').getByRole('button', { name: 'Load more Star Trek wallpapers' }).click();
    await expect(card(page, 'Star Trek', 3, '5120x2880')).toBeVisible();
    await expect(section(page, 'Star Trek').getByRole('button', { name: /^Load more/ })).toHaveCount(0);
  });
});

test.describe('R3: Apply a wallpaper', () => {

  test('R3.1 — requests the apply and marks the chosen card as applying', async ({ page }) => {
    const applying = gate();
    const applied = await backend(page, { apply: async route => { await applying.opened; await json(route, 200, { file: 'x' }); } });
    await page.goto('/');
    await card(page, 'Star Trek', 1, '3840x2160').click();
    await expect(card(page, 'Star Trek', 1, '3840x2160')).toContainText('Applying…');
    expect(applied).toEqual([{ sourceId: 'trek', wallpaperId: 'enterprise' }]);
    applying.open();
  });

  test('R3.2 — marks the applied card and clears the previous mark', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    const enterprise = card(page, 'Star Trek', 1, '3840x2160');
    const voyager = card(page, 'Star Trek', 2, '2560x1440');
    await enterprise.click();
    await expect(enterprise).toContainText('Applied');
    await expect(enterprise).toHaveAttribute('aria-current', 'true');
    await voyager.click();
    await expect(voyager).toContainText('Applied');
    await expect(voyager).toHaveAttribute('aria-current', 'true');
    await expect(enterprise).not.toContainText('Applied');
    await expect(enterprise).toHaveAttribute('aria-current', 'false');
  });

  test('R3.3 — shows the reason on the card when applying fails', async ({ page }) => {
    const reason = 'downloading https://w.wallhaven.cc/full/enterprise.jpg failed: HTTP 404';
    await backend(page, { apply: route => json(route, 502, { reason }) });
    await page.goto('/');
    await card(page, 'Star Trek', 1, '3840x2160').click();
    await expect(section(page, 'Star Trek').getByRole('alert')).toHaveText(reason);
    await expect(card(page, 'Star Trek', 1, '3840x2160')).not.toContainText('Applied');
  });

  test('R3.4 — prevents choosing another card while applying', async ({ page }) => {
    const applying = gate();
    const applied = await backend(page, { apply: async route => { await applying.opened; await json(route, 200, { file: 'x' }); } });
    await page.goto('/');
    await card(page, 'Star Trek', 1, '3840x2160').click();
    await expect(card(page, 'Star Trek', 2, '2560x1440')).toBeDisabled();
    await expect(card(page, 'Nature on Unsplash', 1, '6000x4000')).toBeDisabled();
    applying.open();
    await expect(card(page, 'Star Trek', 2, '2560x1440')).toBeEnabled();
    expect(applied).toHaveLength(1);
  });

  test('R3.5 — lets cards be chosen by keyboard', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    const enterprise = card(page, 'Star Trek', 1, '3840x2160');
    await enterprise.focus();
    await page.keyboard.press('Enter');
    await expect(enterprise).toContainText('Applied');
    const voyager = card(page, 'Star Trek', 2, '2560x1440');
    await voyager.focus();
    await page.keyboard.press('Space');
    await expect(voyager).toContainText('Applied');
  });
});

test.describe('R4: Filter by season', () => {

  const photographers = (page, season) =>
    section(page, 'Nature on Unsplash').getByRole('link', { name: new RegExp(`^${season} photographer`) });

  test('R4.1 — offers a season filter in seasonal sections only', async ({ page }) => {
    await backend(page);
    await page.goto('/');
    const filter = section(page, 'Nature on Unsplash').getByRole('group', { name: 'Season' });
    await expect(filter.getByRole('radio')).toHaveCount(5);
    for (const choice of ['All seasons', 'Spring', 'Summer', 'Autumn', 'Winter']) {
      await expect(filter.getByRole('radio', { name: choice })).toBeVisible();
    }
    await expect(section(page, 'Star Trek').getByRole('radio')).toHaveCount(0);
  });

  test('R4.2 — preselects the current meteorological season of the northern hemisphere', async ({ page }) => {
    const days = [
      ['2026-02-28', 'Winter', 'winter'], ['2026-03-01', 'Spring', 'spring'], ['2026-05-31', 'Spring', 'spring'],
      ['2026-06-01', 'Summer', 'summer'], ['2026-08-31', 'Summer', 'summer'], ['2026-09-01', 'Autumn', 'autumn'],
      ['2026-11-30', 'Autumn', 'autumn'], ['2026-12-01', 'Winter', 'winter'],
    ];
    for (const [day, label, season] of days) {
      const requests = [];
      await page.unrouteAll();
      await backend(page, { seasonal: true, requests });
      await page.clock.setFixedTime(new Date(`${day}T12:00:00`));
      await page.goto('/');
      await expect(section(page, 'Nature on Unsplash').getByRole('radio', { name: label }), day).toBeChecked();
      await expect(photographers(page, season), day).toHaveCount(2);
      expect(requests.map(request => request.get('season')), day).toEqual([season]);
    }
  });

  test('R4.3 — replaces the cards with the first page for the chosen season', async ({ page }) => {
    const requests = [];
    await backend(page, { seasonal: true, requests });
    await page.clock.setFixedTime(new Date('2026-11-04T12:00:00'));
    await page.goto('/');
    await expect(photographers(page, 'autumn')).toHaveCount(2);
    await section(page, 'Nature on Unsplash').getByRole('radio', { name: 'Spring' }).check();
    await expect(photographers(page, 'spring')).toHaveCount(2);
    await expect(photographers(page, 'autumn')).toHaveCount(0);
    await expect(section(page, 'Nature on Unsplash').getByRole('button', { name: /^Apply/ })).toHaveCount(2);
    expect(requests.at(-1).toString()).toBe('page=1&season=spring');
  });

  test('R4.4 — loads the next page within the chosen season', async ({ page }) => {
    const requests = [];
    await backend(page, { seasonal: true, requests });
    await page.clock.setFixedTime(new Date('2026-11-04T12:00:00'));
    await page.goto('/');
    await section(page, 'Nature on Unsplash').getByRole('radio', { name: 'Winter' }).check();
    await expect(photographers(page, 'winter')).toHaveCount(2);
    await section(page, 'Nature on Unsplash').getByRole('button', { name: 'Load more Nature on Unsplash wallpapers' }).click();
    await expect(photographers(page, 'winter')).toHaveCount(4);
    expect(requests.at(-1).toString()).toBe('page=2&season=winter');
  });

  test('R4.5 — shows the source unfiltered for all seasons', async ({ page }) => {
    const requests = [];
    await backend(page, { seasonal: true, requests });
    await page.clock.setFixedTime(new Date('2026-11-04T12:00:00'));
    await page.goto('/');
    await expect(photographers(page, 'autumn')).toHaveCount(2);
    await section(page, 'Nature on Unsplash').getByRole('radio', { name: 'All seasons' }).check();
    await expect(photographers(page, 'all-season')).toHaveCount(2);
    await expect(photographers(page, 'autumn')).toHaveCount(0);
    expect(requests.at(-1).has('season')).toBe(false);
  });
});
