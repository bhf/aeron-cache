import { test, expect } from '@playwright/test';

test.describe('Aeron Cache Operations', () => {
    let cacheId: string;

    // Used for running against a live backend
    test.describe.configure({ mode: 'serial' });

    test.beforeAll(async () => {
        cacheId = `ops-test-${Date.now()}`;
    });

    test.beforeEach(async ({ page }) => {
        await page.goto('/');
    });

    test('should create a cache and add items', async ({ page }) => {
        // Create the cache
        await page.getByTestId('create-cache-input').fill(cacheId);
        await page.getByTestId('create-cache-button').click();
        await expect(page.getByText(/Success/i)).toBeVisible();

        // Navigate to the cache view. Scope to the caches tab panel since the
        // counters tab renders the same table (and shared testids).
        await page.getByTestId('caches-tab-panel').getByTestId('all-caches-filter-input').fill(cacheId);

        await page.getByTestId(`view-cache-${cacheId}`).click();
        await expect(page).toHaveURL(new RegExp(`/cache/${cacheId}`));

        const items = [
            { key: 'key1', value: 'value1' },
            { key: 'key2', value: 'value2' },
            { key: 'key3', value: 'value3' }
        ];

        for (const item of items) {
            await page.getByTestId('add-item-key-input').fill(item.key);
            await page.getByTestId('add-item-value-input').fill(item.value);

            await page.getByTestId('add-item-button').click();

            await expect(page.getByText(/Success/i).first()).toBeVisible();
            await expect(page.getByTestId(`cache-item-row-${item.key}`)).toBeVisible();
        }
    });

    test('should remove an item from the specified cache', async ({ page }) => {
        await page.goto(`/cache/${cacheId}`);
        
        const itemKey = 'key2';
        const row = page.getByTestId(`cache-item-row-${itemKey}`);
        
        // Find the remove button within that row
        await row.getByTestId(new RegExp(`remove-item-trigger`)).click();
        
        // Handle confirmation dialog
        await page.getByTestId('dialog-confirm').click();
        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(row).not.toBeVisible();
    });

    test('should clear the specified cache', async ({ page }) => {
        await page.goto(`/cache/${cacheId}`);
        
        // Click the clear cache button
        await page.getByTestId('clear-cache-trigger').click();
        await page.getByTestId('dialog-confirm').click();
        
        await expect(page.getByText(/Success/i).first()).toBeVisible();
        
        // Verify no items in table
        await expect(page.getByTestId('cache-item-row-key1')).not.toBeVisible();
    });

    test('should delete the specified cache', async ({ page }) => {
        await page.goto(`/cache/${cacheId}`);

        await page.getByTestId('delete-cache-trigger').click();
        await page.getByTestId('dialog-confirm').click();

        await expect(page.getByText(/Success/i).first()).toBeVisible();

        // Should be redirected to home
        await expect(page).toHaveURL(/\/$/);

        // Check the cache is now gone from the main table
        // - filter for it and expect the empty row state. Scope to the caches
        // tab panel since the counters tab renders the same table.
        const cachesPanel = page.getByTestId('caches-tab-panel');
        await cachesPanel.getByTestId('all-caches-filter-input').fill(cacheId);
        await expect(cachesPanel.getByTestId('all-caches-empty-row')).toBeVisible();
    });
});

test.describe('Aeron Cache TTL', () => {
    let cacheId: string;

    // Used for running against a live backend
    test.describe.configure({ mode: 'serial' });

    test.beforeAll(async () => {
        cacheId = `ttl-test-${Date.now()}`;
    });

    test.beforeEach(async ({ page }) => {
        await page.goto('/');
    });

    test('should add an item with a TTL and register a removal timer', async ({ page }) => {
        // Create the cache
        await page.getByTestId('create-cache-input').fill(cacheId);
        await page.getByTestId('create-cache-button').click();
        await expect(page.getByText(/Success/i)).toBeVisible();

        // Navigate to the cache view. Scope to the caches tab panel since the
        // counters tab renders the same table (and shared testids).
        await page.getByTestId('caches-tab-panel').getByTestId('all-caches-filter-input').fill(cacheId);
        await page.getByTestId(`view-cache-${cacheId}`).click();
        await expect(page).toHaveURL(new RegExp(`/cache/${cacheId}`));

        // Add an item with a TTL. Use minutes so the timer stays pending well
        // beyond the lifetime of the test.
        const itemKey = 'ttl-key';
        await page.getByTestId('add-item-key-input').fill(itemKey);
        await page.getByTestId('add-item-value-input').fill('ttl-value');
        await page.getByTestId('add-item-ttl-input').fill('5');
        await page.getByTestId('add-item-ttl-unit-select').selectOption('m');
        await page.getByTestId('add-item-button').click();

        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(page.getByTestId(`cache-item-row-${itemKey}`)).toBeVisible();

        // The TTL should have registered a pending removal timer. Check the
        // timers tab on the home page, filtered to our cache.
        await page.goto('/');
        await page.getByTestId('timers-tab').click();
        const timersPanel = page.getByTestId('timers-tab-panel');
        await timersPanel.getByTestId('all-timers-filter-input').fill(cacheId);

        await expect(timersPanel.getByRole('cell', { name: cacheId })).toBeVisible();
        await expect(timersPanel.getByRole('cell', { name: itemKey, exact: true })).toBeVisible();
        await expect(timersPanel.getByTestId('all-timers-empty-row')).not.toBeVisible();
    });

    test('should add an item without a TTL and register no removal timer', async ({ page }) => {
        await page.goto(`/cache/${cacheId}`);

        // Add an item leaving the TTL field blank - it should never expire.
        const itemKey = 'no-ttl-key';
        await page.getByTestId('add-item-key-input').fill(itemKey);
        await page.getByTestId('add-item-value-input').fill('no-ttl-value');
        await page.getByTestId('add-item-button').click();

        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(page.getByTestId(`cache-item-row-${itemKey}`)).toBeVisible();

        // No timer should exist for this key. Filter the timers tab by cache and
        // confirm the untimed key is absent (the timed key from the prior test
        // may still be present, so we assert on the specific key cell).
        await page.goto('/');
        await page.getByTestId('timers-tab').click();
        const timersPanel = page.getByTestId('timers-tab-panel');
        await timersPanel.getByTestId('all-timers-filter-input').fill(cacheId);

        await expect(timersPanel.getByRole('cell', { name: itemKey, exact: true })).not.toBeVisible();
    });

    test('should delete the TTL test cache', async ({ page }) => {
        await page.goto(`/cache/${cacheId}`);

        await page.getByTestId('delete-cache-trigger').click();
        await page.getByTestId('dialog-confirm').click();

        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(page).toHaveURL(/\/$/);
    });
});

test.describe('Aeron Cache JSON Viewer', () => {
    let cacheId: string;

    // Used for running against a live backend
    test.describe.configure({ mode: 'serial' });

    test.beforeAll(async () => {
        cacheId = `json-test-${Date.now()}`;
    });

    test.beforeEach(async ({ page }) => {
        await page.goto('/');
    });

    test('should show a JSON viewer for JSON values but not for plain values', async ({ page }) => {
        // Create the cache
        await page.getByTestId('create-cache-input').fill(cacheId);
        await page.getByTestId('create-cache-button').click();
        await expect(page.getByText(/Success/i)).toBeVisible();

        // Navigate to the cache view. Scope to the caches tab panel since the
        // counters tab renders the same table (and shared testids).
        await page.getByTestId('caches-tab-panel').getByTestId('all-caches-filter-input').fill(cacheId);
        await page.getByTestId(`view-cache-${cacheId}`).click();
        await expect(page).toHaveURL(new RegExp(`/cache/${cacheId}`));

        // Add a JSON-valued item and a plain-valued item.
        const jsonKey = 'json-key';
        const jsonValue = '{"name":"widget","count":3,"active":true,"tags":["a","b"]}';
        await page.getByTestId('add-item-key-input').fill(jsonKey);
        await page.getByTestId('add-item-value-input').fill(jsonValue);
        await page.getByTestId('add-item-button').click();
        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(page.getByTestId(`cache-item-row-${jsonKey}`)).toBeVisible();

        const plainKey = 'plain-key';
        await page.getByTestId('add-item-key-input').fill(plainKey);
        await page.getByTestId('add-item-value-input').fill('just a plain string value');
        await page.getByTestId('add-item-button').click();
        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(page.getByTestId(`cache-item-row-${plainKey}`)).toBeVisible();

        // The JSON row has a view-json button; the plain row does not.
        const jsonRow = page.getByTestId(`cache-item-row-${jsonKey}`);
        const plainRow = page.getByTestId(`cache-item-row-${plainKey}`);
        await expect(jsonRow.getByTestId('view-json-button')).toBeVisible();
        await expect(plainRow.getByTestId('view-json-button')).toHaveCount(0);

        // Open the viewer and check the value is pretty-printed (indented, so it
        // spans multiple lines) and contains the expected content.
        await jsonRow.getByTestId('view-json-button').click();
        await expect(page.getByTestId('json-viewer-dialog')).toBeVisible();

        const content = page.getByTestId('json-viewer-content');
        await expect(content).toBeVisible();
        await expect(content).toContainText('"name"');
        await expect(content).toContainText('"widget"');
        await expect(content).toContainText('"tags"');

        // Pretty-printed output is multi-line (the raw value is single-line).
        const text = await content.innerText();
        expect(text.split('\n').length).toBeGreaterThan(1);
    });

    test('should delete the JSON test cache', async ({ page }) => {
        await page.goto(`/cache/${cacheId}`);

        await page.getByTestId('delete-cache-trigger').click();
        await page.getByTestId('dialog-confirm').click();

        await expect(page.getByText(/Success/i).first()).toBeVisible();
        await expect(page).toHaveURL(/\/$/);
    });
});
