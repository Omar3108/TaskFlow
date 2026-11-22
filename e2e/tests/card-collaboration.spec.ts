import { expect, test } from '@playwright/test'
import { Api, sessionFor } from './support'

// Flow 3: two people work on the same card. Labels, comments and filters along the way, and
// the optimistic-locking conflict when both edit it at once.
test('two people edit the same card: the second one is told about the conflict', async ({
  browser,
  request,
}) => {
  const api = new Api(request)
  const owner = await api.register('Ada Owner')
  const admin = await api.register('Grace Admin')
  const team = await api.team(owner, [{ user: admin, role: 'ADMIN' }])
  const [todo] = team.columnIds
  await api.call(owner, 'POST', `/boards/${team.boardId}/labels`, { name: 'Bug', color: 'RED' })
  const card = await api.call(owner, 'POST', `/columns/${todo}/cards`, {
    title: 'Checkout fails on Safari',
  })
  await api.call(owner, 'POST', `/columns/${todo}/cards`, { title: 'Update the logo' })

  const ada = await sessionFor(browser, owner.email)
  const grace = await sessionFor(browser, admin.email)
  const cardUrl = `/boards/${team.boardId}?card=${card.id}`

  await test.step('both open the card from a shared link', async () => {
    for (const { page } of [ada, grace]) {
      await page.goto(cardUrl)
      await expect(
        page.getByRole('dialog', { name: 'Card details' }).getByLabel('Title'),
      ).toHaveValue('Checkout fails on Safari')
    }
  })

  await test.step('Ada labels it as a bug and saves first', async () => {
    const dialog = ada.page.getByRole('dialog', { name: 'Card details' })
    await dialog.getByRole('checkbox', { name: 'Bug' }).check()
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(ada.page.getByText('Card saved')).toBeVisible()
    await expect(ada.page.getByRole('button', { name: 'Checkout fails on Safari' })).toContainText(
      'Bug',
    )
  })

  await test.step('Grace saves her stale copy and is told someone else changed it', async () => {
    const dialog = grace.page.getByRole('dialog', { name: 'Card details' })
    await dialog.getByLabel('Title').fill('Checkout fails on Safari 17')
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(dialog.getByRole('alert')).toContainText('Someone else changed this card')
    await expect(dialog.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  await test.step("she loads the latest version, which has Ada's label, and saves again", async () => {
    const dialog = grace.page.getByRole('dialog', { name: 'Card details' })
    await dialog.getByRole('button', { name: 'Load latest version' }).click()
    await expect(dialog.getByRole('checkbox', { name: 'Bug' })).toBeChecked()
    await expect(dialog.getByLabel('Title')).toHaveValue('Checkout fails on Safari')
    await dialog.getByLabel('Title').fill('Checkout fails on Safari 17')
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(grace.page.getByText('Card saved')).toBeVisible()
  })

  await test.step('a comment from Grace shows up for Ada, and on the card on the board', async () => {
    await grace.page.goto(cardUrl)
    const comments = grace.page.getByRole('region', { name: /Comments/ })
    await comments.getByLabel('Add a comment').fill('Reproduced on an iPhone too.')
    await comments.getByRole('button', { name: 'Comment' }).click()
    await expect(comments.getByText('Reproduced on an iPhone too.')).toBeVisible()

    await ada.page.goto(`/boards/${team.boardId}`)
    const onBoard = ada.page.getByRole('button', { name: 'Checkout fails on Safari 17' })
    await expect(onBoard.getByLabel('1 comment')).toBeVisible()
  })

  await test.step('filtering by the Bug label leaves only that card, and the filter is in the URL', async () => {
    await ada.page.getByRole('combobox', { name: 'Label' }).selectOption({ label: 'Bug' })
    await expect(ada.page).toHaveURL(/label=/)
    await expect(
      ada.page.getByRole('button', { name: 'Checkout fails on Safari 17' }),
    ).toBeVisible()
    await expect(ada.page.getByRole('button', { name: 'Update the logo' })).toHaveCount(0)
    await ada.page.reload()
    await expect(ada.page.getByRole('button', { name: 'Update the logo' })).toHaveCount(0)
  })

  await ada.context.close()
  await grace.context.close()
})
