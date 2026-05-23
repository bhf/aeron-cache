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

        // Navigate to the cache view
        await page.getByTestId('all-caches-filter-input').fill(cacheId);

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
        // - filter for it and expect the empty row state
        await page.getByTestId('all-caches-filter-input').fill(cacheId);
        await expect(page.getByTestId('all-caches-empty-row')).toBeVisible();
    });
});
