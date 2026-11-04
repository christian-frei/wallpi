// @ts-check
const { defineConfig, devices } = require('@playwright/test');
const { homedir } = require('node:os');
const { join } = require('node:path');

// zws opens a browser on start; headless AWT suppresses that and the server keeps serving
const zws = process.env.ZWS ?? join(homedir(), '.claude/skills/web-conventions/scripts/zws');

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  webServer: {
    command: `java -Djava.awt.headless=true --source 25 ${zws} ../app/src --single`,
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
});
