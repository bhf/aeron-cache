import { test, expect } from '@playwright/test';

test.describe('Aeron Cache Management', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/');
    });

    test('should display dashboard stats', async ({ page }) => {
        await expect(page.getByTestId('stats-total-ops')).toBeVisible();
        await expect(page.getByTestId('stats-total-caches')).toBeVisible();
        await expect(page.getByTestId('stats-total-items')).toBeVisible();
        await expect(page.getByTestId('stats-total-errors')).toBeVisible();
        
        const opsValue = page.getByTestId('stats-total-ops-value');
        await expect(opsValue).not.toBeEmpty();
    });

    test('should create a new cache', async ({ page }) => {
        const cacheId = `test-cache-${Date.now()}`;
        
        const input = page.getByTestId('create-cache-input');
        const submitBtn = page.getByTestId('create-cache-button');

        await input.fill(cacheId);
        await submitBtn.click();

        await expect(page.getByText(/Success/i)).toBeVisible();
        
        // Use the filter to find our new cache
        await page.getByTestId('all-caches-filter-input').fill(cacheId);
        
        // Verify it appears in the table
        await expect(page.getByRole('cell', { name: cacheId })).toBeVisible();
    });

});
