import { expect, test } from '@playwright/test'
import { PASSWORD, cardsIn, centerOf, drag, uniqueEmail } from './support'

// Flow 1: a new user goes from registration to a working board, entirely through the UI.
test('register, create a workspace and a board, add a card and drag it to Done', async ({
  page,
}) => {
  await test.step('register', async () => {
    await page.goto('/register')
    await page.getByLabel('Name').fill('Erin E2E')
    await page.getByLabel('Email').fill(uniqueEmail('erin'))
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD)
    await page.getByLabel('Confirm password').fill(PASSWORD)
    await page.getByRole('button', { name: 'Create account' }).click()
    await expect(page.getByRole('heading', { name: 'Your workspaces' })).toBeVisible()
  })

  await test.step('create a workspace', async () => {
    await page.getByRole('button', { name: 'New workspace' }).click()
    const dialog = page.getByRole('dialog', { name: 'Create workspace' })
    await dialog.getByLabel('Workspace name').fill('Product team')
    await dialog.getByRole('button', { name: 'Create' }).click()
    await expect(page.getByRole('heading', { name: 'Product team' })).toBeVisible()
  })

  await test.step('create a board, which starts with three columns', async () => {
    await page.getByRole('button', { name: 'New board' }).click()
    const dialog = page.getByRole('dialog', { name: 'New board' })
    await dialog.getByLabel('Board name').fill('Website relaunch')
    await dialog.getByRole('button', { name: 'Create board' }).click()
    await expect(page.getByRole('heading', { name: 'Website relaunch' })).toBeVisible()
    await expect(page).toHaveTitle('Website relaunch · TaskFlow')
    for (const column of ['To do', 'In progress', 'Done']) {
      await expect(page.getByRole('region', { name: column, exact: true })).toBeVisible()
    }
  })

  await test.step('add two cards to To do', async () => {
    const todo = page.getByRole('region', { name: 'To do', exact: true })
    await todo.getByRole('button', { name: '+ Add card' }).click()
    const title = todo.getByLabel('Title of the new card in To do')
    await title.fill('Write the pricing page')
    await title.press('Enter')
    await title.fill('Pick the fonts')
    await title.press('Enter')
    await expect(cardsIn(page, 'To do')).toHaveText(['Write the pricing page', 'Pick the fonts'])
  })

  await test.step('drag a card to Done', async () => {
    const card = page.getByRole('button', { name: 'Pick the fonts' })
    const done = page.getByRole('region', { name: 'Done', exact: true })
    await drag(
      page,
      await centerOf(page, card),
      await centerOf(page, done.getByRole('button', { name: '+ Add card' })),
    )
    await expect(cardsIn(page, 'Done')).toHaveText(['Pick the fonts'])
    await expect(cardsIn(page, 'To do')).toHaveText(['Write the pricing page'])
  })

  await test.step('the move was saved: it survives a reload', async () => {
    await page.reload()
    await expect(cardsIn(page, 'Done')).toHaveText(['Pick the fonts'])
    await expect(cardsIn(page, 'To do')).toHaveText(['Write the pricing page'])
  })
})
