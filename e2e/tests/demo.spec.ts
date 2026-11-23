import { expect, test } from '@playwright/test'

// The first thing a recruiter clicks. Also a guard for the Content Security Policy: the web
// container sends the same headers as production, and any violation fails this test.
test('"Try the demo" opens a private, pre-filled board without signing up', async ({ page }) => {
  const cspViolations: string[] = []
  page.on('console', (message) => {
    if (message.text().includes('Content Security Policy')) cspViolations.push(message.text())
  })

  await page.goto('/login')
  await page.getByRole('button', { name: 'Try the demo' }).click()

  await expect(page.getByRole('heading', { name: 'Your workspaces' })).toBeVisible()
  await page.getByRole('link', { name: /Demo workspace/ }).click()
  await page.getByRole('link', { name: /Product launch/ }).click()

  await expect(page.getByRole('heading', { name: 'Product launch' })).toBeVisible()
  await expect(page.getByRole('region', { name: 'To do', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Design the landing page hero' })).toBeVisible()
  // A toast proves the toast library's runtime styles are allowed by the CSP.
  await page.getByRole('button', { name: 'Design the landing page hero' }).click()
  await page
    .getByRole('dialog', { name: 'Card details' })
    .getByRole('button', { name: 'Save' })
    .click()
  await expect(page.getByText('Card saved')).toBeVisible()

  expect(cspViolations).toEqual([])
})
