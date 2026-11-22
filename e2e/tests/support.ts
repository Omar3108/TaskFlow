import { expect, type APIRequestContext, type Browser, type Page } from '@playwright/test'

export const PASSWORD = 'e2e-password-123'

/** A fresh e-mail per call, so tests never collide with each other or with earlier runs. */
export function uniqueEmail(name: string) {
  return `${name.split(' ')[0].toLowerCase()}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.test`
}

export interface ApiUser {
  id: string
  email: string
  displayName: string
  token: string
}

/**
 * Talks to the API directly to prepare data quickly. Tests use it for set-up only; the
 * behaviour under test is always driven through the browser.
 */
export class Api {
  constructor(private readonly request: APIRequestContext) {}

  async register(displayName: string): Promise<ApiUser> {
    const email = uniqueEmail(displayName)
    const body = await this.call(null, 'POST', '/auth/register', {
      email,
      password: PASSWORD,
      displayName,
    })
    return { id: body.user.id, email, displayName, token: body.accessToken }
  }

  async call(user: ApiUser | null, method: string, path: string, data?: unknown) {
    const response = await this.request.fetch(`/api/v1${path}`, {
      method,
      data,
      headers: user ? { Authorization: `Bearer ${user.token}` } : {},
    })
    expect(response.ok(), `${method} ${path} -> ${response.status()}`).toBeTruthy()
    return response.status() === 204 ? null : response.json()
  }

  /** Workspace owned by `owner`, with the given members, and a board with the default columns. */
  async team(owner: ApiUser, members: { user: ApiUser; role: 'ADMIN' | 'MEMBER' }[] = []) {
    const workspace = await this.call(owner, 'POST', '/workspaces', { name: 'E2E team' })
    for (const { user, role } of members) {
      const invitation = await this.call(owner, 'POST', `/workspaces/${workspace.id}/invitations`, {
        email: user.email,
        role,
      })
      await this.call(user, 'POST', `/invitations/${invitation.token}/accept`)
    }
    const board = await this.call(owner, 'POST', `/workspaces/${workspace.id}/boards`, {
      name: 'Launch plan',
      description: null,
    })
    const detail = await this.call(owner, 'GET', `/boards/${board.id}`)
    return {
      workspaceId: workspace.id as string,
      boardId: board.id as string,
      columnIds: (detail.columns as { id: string }[]).map((column) => column.id),
    }
  }
}

export async function signIn(page: Page, email: string) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('button', { name: 'Account menu' })).toBeVisible()
}

/** A separate browser session (its own cookies), signed in as the given user. */
export async function sessionFor(browser: Browser, email: string) {
  const context = await browser.newContext()
  const page = await context.newPage()
  await signIn(page, email)
  return { context, page }
}

/** The cards of a column, top to bottom, by title. */
export function cardsIn(page: Page, column: string) {
  return page
    .getByRole('region', { name: column, exact: true })
    .locator('li[aria-roledescription="sortable"]')
}

/**
 * Drags with real mouse events. dnd-kit only starts a drag after the pointer has moved a few
 * pixels, so the movement is split into steps, like a person would do it.
 */
export async function drag(
  page: Page,
  from: { x: number; y: number },
  to: { x: number; y: number },
) {
  await page.mouse.move(from.x, from.y)
  await page.mouse.down()
  await page.mouse.move(from.x + 10, from.y + 10, { steps: 5 })
  await page.mouse.move(to.x, to.y, { steps: 20 })
  await page.mouse.up()
}

export async function centerOf(page: Page, locator: ReturnType<Page['locator']>) {
  const box = await locator.boundingBox()
  if (!box) throw new Error('Element is not visible')
  return { x: box.x + box.width / 2, y: box.y + box.height / 2 }
}
