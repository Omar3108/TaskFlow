import { expect, test } from '@playwright/test'
import { Api, PASSWORD, signIn, uniqueEmail } from './support'

// Flow 2: an owner invites someone by e-mail; the invitee signs up from the link, joins, and
// sees the board with member permissions only.
test('an invited person signs up from the link and works as a member', async ({
  page,
  browser,
  request,
}) => {
  const api = new Api(request)
  const owner = await api.register('Olivia Owner')
  const team = await api.team(owner)
  const inviteeEmail = uniqueEmail('ivan')

  let link = ''
  await test.step('the owner invites by e-mail and gets a shareable link', async () => {
    await signIn(page, owner.email)
    await page.goto(`/workspaces/${team.workspaceId}`)
    await page.getByLabel('Email address').fill(inviteeEmail)
    await page.getByRole('button', { name: 'Invite' }).click()
    link = await page.getByLabel('Invitation link').inputValue()
    expect(link).toContain('/invite/')
  })

  const invitee = await browser.newContext()
  const guest = await invitee.newPage()

  await test.step('the invitee opens the link, is asked to sign in, and creates an account', async () => {
    await guest.goto(new URL(link).pathname)
    await expect(guest.getByRole('button', { name: 'Sign in' })).toBeVisible()
    await guest.getByRole('link', { name: 'Create an account' }).click()
    await guest.getByLabel('Name').fill('Ivan Invitee')
    await guest.getByLabel('Email').fill(inviteeEmail)
    await guest.getByLabel('Password', { exact: true }).fill(PASSWORD)
    await guest.getByLabel('Confirm password').fill(PASSWORD)
    await guest.getByRole('button', { name: 'Create account' }).click()
  })

  await test.step('back on the invitation, they accept and land in the workspace', async () => {
    await expect(
      guest.getByText('Olivia Owner invited you to join E2E team as Member.'),
    ).toBeVisible()
    await guest.getByRole('button', { name: 'Accept invitation' }).click()
    await expect(guest.getByRole('heading', { name: 'E2E team' })).toBeVisible()
    await expect(guest.getByRole('link', { name: /Launch plan/ })).toBeVisible()
  })

  await test.step('on the board they can add cards but not manage the board', async () => {
    await guest.getByRole('link', { name: /Launch plan/ }).click()
    await expect(guest.getByRole('heading', { name: 'Launch plan' })).toBeVisible()
    await expect(guest.getByRole('button', { name: '+ Add card' }).first()).toBeVisible()
    for (const adminOnly of ['Labels', 'Edit board', 'Delete board', '+ Add column']) {
      await expect(guest.getByRole('button', { name: adminOnly })).toHaveCount(0)
    }
    await expect(guest.getByRole('button', { name: /^Column actions/ })).toHaveCount(0)
  })

  await test.step('the owner now sees them in the member list', async () => {
    await page.reload()
    await expect(page.getByText('Ivan Invitee').first()).toBeVisible()
  })

  await invitee.close()
})
