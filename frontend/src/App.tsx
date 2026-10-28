import { useEffect, useState, type FormEvent } from 'react'
import './App.css'

type VerificationState = 'ready' | 'submitting' | 'success' | 'error' | 'resending' | 'resent'
type SignupState = 'ready' | 'submitting' | 'submitted' | 'error'

type Member = { id?: string; displayName: string; email: string; city: string | null }
type AccountProfile = { displayName: string; email: string; city: string; pendingEmail: string | null; emailVerified: boolean }
type BookOfferType = 'SALE' | 'FREE' | 'EXCHANGE'
type BookCondition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR'
type BookListing = {
  id: string
  ownerId: string
  title: string
  author: string
  genres: string[]
  condition: BookCondition
  city: string
  offerType: BookOfferType
  askingPrice: number | null
  isbn: string | null
  description: string | null
  ownerName: string
  createdAt: string
  availability: 'AVAILABLE' | 'RESERVED' | 'EXCHANGED' | 'SOLD'
  salePriceVerified: boolean
}
type SaleBillUpload = { id: string }
type SaleBillCheck = { priceCheckPassed: boolean; detail: string }
type ExchangeStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'EXPIRED' | 'CLOSED' | 'COMPLETED'
type SaleInterestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CLOSED'
type SaleInterestItem = {
  id: string
  status: SaleInterestStatus
  book: BookListing
  requesterId: string
  requesterName: string
  ownerId: string
  ownerName: string
  incoming: boolean
  reserveListing: boolean | null
  createdAt: string
  acceptedAt: string | null
}
type ExchangeItem = {
  id: string
  status: ExchangeStatus
  requestedBook: BookListing
  offeredBook: BookListing
  requesterId: string
  requesterName: string
  ownerId: string
  ownerName: string
  cancelledById: string | null
  createdAt: string
  expiresAt: string
  acceptedAt: string | null
  cancelledAt: string | null
  requesterCompletedAt: string | null
  ownerCompletedAt: string | null
  completedAt: string | null
  incoming: boolean
}
type ChatMessage = { id: string; senderId: string; senderName: string; text: string; createdAt: string; readAt: string | null }
type SaleChatMessage = {
  id: string; senderId: string; senderName: string; text: string | null; createdAt: string; readAt: string | null
  sharedBillId: string | null; sharedBillFilename: string | null
}
type FreeClaimStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CLOSED' | 'CANCELLED'
type FreeClaimItem = {
  id: string; status: FreeClaimStatus; book: BookListing; requesterId: string; requesterName: string
  ownerId: string; ownerName: string; incoming: boolean; createdAt: string; acceptedAt: string | null
  cancelledAt: string | null; cancelledById: string | null
}
type FreeClaimMessage = { id: string; senderId: string; senderName: string; text: string; createdAt: string; readAt: string | null }
type Presence = { displayName: string; online: boolean; lastActiveAt: string | null }
const genres = ['Biography', 'Children', 'Fantasy', 'Fiction', 'History', 'Mystery', 'Non-fiction', 'Poetry', 'Romance', 'Science', 'Science fiction', 'Self-help', 'Thriller']
let accessToken: string | null = null
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
let refreshInFlight: Promise<{ accessToken: string; member: Member } | null> | null = null

// Renders consistent circular Exchanges and Profile controls next to the member's sign-out action.
function MemberHeaderActions({ member, unreadCount, onSignOut }: {
  member: Member
  unreadCount: number
  onSignOut: () => void
}) {
  const initial = member.displayName.trim().charAt(0).toUpperCase() || 'P'
  return <div className="books-member">
    <span className="books-member-details">{member.displayName} · {member.city}</span>
    <div className="books-member-actions">
      <a className="exchange-round-button" href="/exchanges" aria-label={`Exchanges${unreadCount ? `, ${unreadCount} new items` : ''}`} title="Exchanges">
        ⇄{unreadCount > 0 ? <span className="exchange-count-badge">{unreadCount > 99 ? '99+' : unreadCount}</span> : null}
      </a>
      <a className="profile-round-button" href="/profile" aria-label="Profile" title="Profile">{initial}</a>
      <button className="text-button" onClick={onSignOut}>Sign out</button>
    </div>
  </div>
}

async function readError(response: Response, fallback: string) {
  const body = await response.json().catch(() => ({}))
  return body.detail || fallback
}

// Fetches the member-specific count used by the Exchanges header badge.
async function loadUnreadCount(token: string) {
  const headers = { Authorization: `Bearer ${token}` }
  const [exchangeResponse, saleResponse, freeResponse] = await Promise.all([
    fetch(`${apiBaseUrl}/api/exchanges/unread-count`, { headers }),
    fetch(`${apiBaseUrl}/api/sale-interests/unread-count`, { headers }),
    fetch(`${apiBaseUrl}/api/free-claims/unread-count`, { headers }),
  ])
  const exchangeCount = exchangeResponse.ok ? Number((await exchangeResponse.json()).count) || 0 : 0
  const saleCount = saleResponse.ok ? Number((await saleResponse.json()).count) || 0 : 0
  const freeCount = freeResponse.ok ? Number((await freeResponse.json()).count) || 0 : 0
  return exchangeCount + saleCount + freeCount
}

// Rotates the HttpOnly refresh cookie with CSRF protection and deduplicates simultaneous browser calls.
function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const csrfResponse = await fetch(`${apiBaseUrl}/api/auth/csrf`, { credentials: 'include' })
      if (!csrfResponse.ok) throw new Error('Could not prepare secure session refresh.')
      const csrf = await csrfResponse.json()
      const response = await fetch(`${apiBaseUrl}/api/auth/refresh`, {
        method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': csrf.token },
      })
      if (response.status === 401) return null
      if (!response.ok) throw new Error('Could not refresh the BookLoop session.')
      const result = await response.json()
      return { accessToken: result.accessToken as string, member: result.member as Member }
    })().finally(() => { refreshInFlight = null })
  }
  return refreshInFlight
}

function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [city, setCity] = useState('')
  const [member, setMember] = useState<Member | null>(null)
  const [state, setState] = useState('ready')
  const [message, setMessage] = useState('')

  // Restores the browser session after a reload by rotating its HttpOnly refresh cookie.
  useEffect(() => {
    let active = true
    refreshSession().then((session) => {
      if (active && session) {
        accessToken = session.accessToken
        if (session.member.city) {
          window.location.assign('/books')
          return
        }
        setMember(session.member)
      }
    }).catch(() => {
      // A failed background restoration leaves the sign-in form available.
    })
    return () => { active = false }
  }, [])

  // Renews the short-lived access JWT before it expires while this page remains open.
  useEffect(() => {
    if (!member) return
    let active = true
    let timer: number
    const renew = async () => {
      try {
        const session = await refreshSession()
        if (active && session) {
          accessToken = session.accessToken
          setMember(session.member)
        } else if (active) {
          accessToken = null
          setMember(null)
        }
      } catch {
        if (active) timer = window.setTimeout(renew, 60_000)
      }
    }
    timer = window.setTimeout(renew, 13 * 60_000)
    return () => { active = false; window.clearTimeout(timer) }
  }, [member])

  // Authenticates a verified account and retains its short-lived JWT only in this page's JS memory.
  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setState('submitting')
    setMessage('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
        method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Sign-in failed.'))
      const result = await response.json()
      accessToken = result.accessToken
      if (result.member.city) {
        window.location.assign('/books')
        return
      }
      setMember(result.member)
      setState('ready')
    } catch (error) {
      setState('error')
      setMessage(error instanceof Error ? error.message : 'A network error occurred.')
    }
  }

  // Saves the member's required public city and replaces the limited-profile token with member access.
  async function completeCity(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken) return
    setState('submitting')
    try {
      const response = await fetch(`${apiBaseUrl}/api/accounts/me/city`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify({ city }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not save your city.'))
      const result = await response.json()
      accessToken = result.accessToken
      window.location.assign('/books')
    } catch (error) {
      setState('error')
      setMessage(error instanceof Error ? error.message : 'A network error occurred.')
    }
  }

  // Fetches the CSRF token, revokes the refresh session, and clears the in-memory access token.
  async function logout() {
    setState('submitting')
    try {
      const csrfResponse = await fetch(`${apiBaseUrl}/api/auth/csrf`, { credentials: 'include' })
      if (!csrfResponse.ok) throw new Error('Could not start sign-out safely.')
      const csrf = await csrfResponse.json()
      const response = await fetch(`${apiBaseUrl}/api/auth/logout`, {
        method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': csrf.token },
      })
      if (!response.ok) throw new Error('Could not sign out.')
      accessToken = null
      setMember(null)
      setPassword('')
      setState('ready')
    } catch (error) {
      setState('error')
      setMessage(error instanceof Error ? error.message : 'A network error occurred.')
    }
  }

  if (member && member.city === null) {
    return <main className="app-shell"><section className="welcome-card form-card">
      <p className="eyebrow">One last profile detail</p><h1 className="form-title">Your city</h1>
      <p className="welcome-copy">Your city is shown on your book listings so nearby readers can arrange an exchange.</p>
      <form className="signup-form" onSubmit={completeCity}>
        <label htmlFor="member-city">City</label><input id="member-city" required maxLength={255} value={city} onChange={(e) => setCity(e.target.value)} />
        {message ? <p className="form-error" role="alert">{message}</p> : null}
        <button className="primary-button" disabled={state === 'submitting'}>{state === 'submitting' ? 'Saving...' : 'Save city'}</button>
      </form>
    </section></main>
  }
  if (member) return <main className="app-shell"><section className="welcome-card">
    <p className="eyebrow">Signed in</p><h1 className="form-title">Welcome, {member.displayName}</h1>
    <p className="welcome-copy">Your profile is set to {member.city}. BookLoop member access is ready.</p>
    {message ? <p className="form-error" role="alert">{message}</p> : null}
    <button className="primary-button" onClick={logout} disabled={state === 'submitting'}>Sign out</button>
  </section></main>

  return <main className="app-shell"><section className="welcome-card form-card">
    <p className="eyebrow">BookLoop account</p><h1 className="form-title">Sign in</h1>
    <form className="signup-form" onSubmit={login}>
      <label htmlFor="login-email">Email address</label><input id="login-email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
      <label htmlFor="login-password">Password</label><input id="login-password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
      {state === 'error' ? <p className="form-error" role="alert">{message}</p> : null}
      <button className="primary-button" disabled={state === 'submitting'}>{state === 'submitting' ? 'Signing in...' : 'Sign in'}</button>
    </form>
    <p className="form-footer"><a href="/forgot-password">Forgot password?</a> · <a href="/signup">Create an account</a></p>
  </section></main>
}

function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  // Requests a reset link and presents the same confirmation regardless of account existence.
  async function requestReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/password-reset/request`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not submit the request.'))
      setSent(true)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'A network error occurred.')
    } finally { setBusy(false) }
  }
  return <main className="app-shell"><section className="welcome-card form-card">
    <p className="eyebrow">Account recovery</p><h1 className="form-title">Reset password</h1>
    {sent ? <p className="welcome-copy" role="status">If a verified account exists, reset instructions will be sent to that email.</p> : <form className="signup-form" onSubmit={requestReset}>
      <label htmlFor="reset-email">Email address</label><input id="reset-email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
      {error ? <p className="form-error" role="alert">{error}</p> : null}<button className="primary-button" disabled={busy}>{busy ? 'Sending...' : 'Send reset link'}</button>
    </form>}
    <p className="form-footer"><a href="/login">Back to sign in</a></p>
  </section></main>
}

function ResetPasswordPage({ token }: { token: string | null }) {
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    const url = new URL(window.location.href)
    if (url.searchParams.has('token')) { url.searchParams.delete('token'); window.history.replaceState({}, document.title, url.pathname) }
  }, [])

  // Submits a one-time token and new password, then lets the user sign in with the changed credential.
  async function submitReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) { setMessage('This reset link is missing or expired.'); return }
    if (password !== confirmation) { setMessage('Passwords do not match.'); return }
    setBusy(true); setMessage('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/password-reset/complete`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token, newPassword: password }),
      })
      if (!response.ok) throw new Error(await readError(response, 'This reset link is invalid or expired.'))
      setMessage('Password changed. You can now sign in.')
      setPassword(''); setConfirmation('')
    } catch (cause) { setMessage(cause instanceof Error ? cause.message : 'A network error occurred.') }
    finally { setBusy(false) }
  }
  return <main className="app-shell"><section className="welcome-card form-card">
    <p className="eyebrow">Account recovery</p><h1 className="form-title">Choose a new password</h1>
    <form className="signup-form" onSubmit={submitReset}>
      <label htmlFor="new-password">New password</label><input id="new-password" type="password" minLength={12} required value={password} onChange={(e) => setPassword(e.target.value)} />
      <label htmlFor="confirm-password">Confirm password</label><input id="confirm-password" type="password" minLength={12} required value={confirmation} onChange={(e) => setConfirmation(e.target.value)} />
      {message ? <p className={message.startsWith('Password changed') ? 'welcome-copy' : 'form-error'} role="status">{message}</p> : null}
      <button className="primary-button" disabled={busy}>{busy ? 'Updating...' : 'Update password'}</button>
    </form><p className="form-footer"><a href="/login">Sign in</a></p>
  </section></main>
}

function SignUpPage() {
  const [state, setState] = useState<SignupState>('ready')
  const [message, setMessage] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  async function register(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (state === 'submitting') return

    setState('submitting')
    setMessage('')
    try {
      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
      const response = await fetch(`${apiBaseUrl}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ displayName, email, password }),
      })

      const body = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(body.detail || 'We could not submit your signup. Check your details and try again.')
      }

      setState('submitted')
      setMessage(body.message || 'If signup can proceed, check your email for verification instructions.')
      setPassword('')
    } catch (error) {
      setState('error')
      setMessage(error instanceof Error ? error.message : 'A network error occurred. Please try again.')
    }
  }

  if (state === 'submitted') {
    return (
      <main className="app-shell">
        <section className="welcome-card" aria-labelledby="signup-title">
          <p className="eyebrow">BookLoop account</p>
          <h1 className="form-title" id="signup-title">Check your email</h1>
          <p className="welcome-copy" role="status" aria-live="polite">{message}</p>
        </section>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <section className="welcome-card form-card" aria-labelledby="signup-title">
        <p className="eyebrow">Join the book community</p>
        <h1 className="form-title" id="signup-title">Create your account</h1>
        <p className="welcome-copy">Sign up to share, exchange, or find books with other readers.</p>
        <form className="signup-form" onSubmit={register}>
          <label htmlFor="display-name">Display name</label>
          <input
            id="display-name"
            type="text"
            autoComplete="name"
            required
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
          />

          <label htmlFor="signup-email">Email address</label>
          <input
            id="signup-email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />

          <label htmlFor="signup-password">Password</label>
          <input
            id="signup-password"
            type="password"
            autoComplete="new-password"
            minLength={12}
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            aria-describedby="password-help"
          />
          <span className="field-help" id="password-help">Use at least 12 characters.</span>

          {state === 'error' ? <p className="form-error" role="alert">{message}</p> : null}
          <button className="primary-button" type="submit" disabled={state === 'submitting'}>
            {state === 'submitting' ? 'Creating account...' : 'Create account'}
          </button>
        </form>
        <p className="form-footer">Already have an account? <a href="/login">Sign in</a></p>
      </section>
    </main>
  )
}

function VerifyEmailPage({ token }: { token: string | null }) {
  const [state, setState] = useState<VerificationState>(token ? 'ready' : 'error')
  const [email, setEmail] = useState('')

  useEffect(() => {
    const currentUrl = new URL(window.location.href)
    if (currentUrl.searchParams.has('token')) {
      currentUrl.searchParams.delete('token')
      window.history.replaceState({}, document.title, `${currentUrl.pathname}${currentUrl.search}${currentUrl.hash}`)
    }
  }, [])

  async function verifyEmail() {
    if (!token || state === 'submitting') return

    setState('submitting')
    try {
      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
      const response = await fetch(`${apiBaseUrl}/api/auth/verify-email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      })

      if (!response.ok) throw new Error('Verification token was rejected')
      setState('success')
    } catch {
      setState('error')
    }
  }

  async function resendVerification(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (state === 'resending') return

    setState('resending')
    try {
      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
      const response = await fetch(`${apiBaseUrl}/api/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      })

      if (!response.ok) throw new Error('Could not request a verification email')
      setState('resent')
    } catch {
      setState('error')
    }
  }

  const isSuccess = state === 'success'
  const title = isSuccess
    ? 'Email verified'
    : state === 'resent'
      ? 'Check your inbox'
    : state === 'error'
      ? 'Link unavailable'
      : 'Verify your email'

  return (
    <main className="app-shell">
      <section className="welcome-card verification-card" aria-labelledby="app-title">
        <p className="eyebrow">BookLoop account</p>
        <h1 id="app-title">{title}</h1>
        <p className="welcome-copy" role="status" aria-live="polite">
          {isSuccess
            ? 'Your email is verified. You can continue to BookLoop.'
            : state === 'resent'
              ? 'If this email needs verification, a new link will be sent. Check your inbox.'
              : state === 'error'
                ? 'This link is missing, expired, or already used. Request a new link below.'
              : 'Confirm your email address to continue setting up your account.'}
        </p>
        {state === 'ready' || state === 'submitting' ? (
          <button className="primary-button" onClick={verifyEmail} disabled={state === 'submitting'}>
            {state === 'submitting' ? 'Verifying...' : 'Verify email'}
          </button>
        ) : null}
        {state === 'error' || state === 'resending' ? (
          <form className="resend-form" onSubmit={resendVerification}>
            <label htmlFor="verification-email">Email address</label>
            <input
              id="verification-email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <button className="primary-button" type="submit" disabled={state === 'resending'}>
              {state === 'resending' ? 'Sending...' : 'Send a new link'}
            </button>
          </form>
        ) : null}
      </section>
    </main>
  )
}

function ProfilePage() {
  const [profile, setProfile] = useState<AccountProfile | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [displayName, setDisplayName] = useState('')
  const [city, setCity] = useState('')
  const [newEmail, setNewEmail] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [emailBusy, setEmailBusy] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  // Restores the signed-in session and loads the account's private profile fields.
  useEffect(() => {
    let active = true
    refreshSession().then(async (session) => {
      if (!active) return
      if (!session || !session.member.city) { window.location.assign('/login'); return }
      accessToken = session.accessToken
      setToken(session.accessToken)
      loadUnreadCount(session.accessToken).then((count) => { if (active) setUnreadCount(count) }).catch(() => {})
      try {
        const response = await fetch(`${apiBaseUrl}/api/accounts/me`, {
          headers: { Authorization: `Bearer ${session.accessToken}` },
        })
        if (!response.ok) throw new Error(await readError(response, 'Could not load your profile.'))
        const result = await response.json() as AccountProfile
        if (active) {
          setProfile(result)
          setDisplayName(result.displayName)
          setCity(result.city)
        }
      } catch (cause) {
        if (active) setError(cause instanceof Error ? cause.message : 'Could not load your profile.')
      } finally {
        if (active) setLoading(false)
      }
    }).catch(() => {
      if (active) { setError('Could not restore your BookLoop session.'); setLoading(false) }
    })
    return () => { active = false }
  }, [])

  // Saves the editable display name and city; the server also updates all existing listing cities.
  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || saving) return
    setSaving(true); setMessage(''); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/accounts/me/profile`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ displayName, city }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not save your profile.'))
      const result = await response.json() as AccountProfile
      setProfile(result); setDisplayName(result.displayName); setCity(result.city)
      setMessage('Your profile details were saved.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save your profile.')
    } finally { setSaving(false) }
  }

  // Requires the current password and asks the server to verify a new email before it becomes active.
  async function requestEmailChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || emailBusy) return
    setEmailBusy(true); setMessage(''); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/accounts/me/email-change`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ currentPassword, newEmail }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not request an email change.'))
      const result = await response.json() as AccountProfile
      setProfile(result); setNewEmail(''); setCurrentPassword('')
      setMessage(`Verification was sent to ${result.pendingEmail}. Your current email remains active until verification.`)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not request an email change.')
    } finally { setEmailBusy(false) }
  }

  // Revokes the refresh session and returns the member to sign-in.
  async function signOut() {
    const csrfResponse = await fetch(`${apiBaseUrl}/api/auth/csrf`, { credentials: 'include' })
    if (!csrfResponse.ok) return
    const csrf = await csrfResponse.json()
    const response = await fetch(`${apiBaseUrl}/api/auth/logout`, {
      method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': csrf.token },
    })
    if (response.ok) { accessToken = null; window.location.assign('/login') }
  }

  return <main className="books-page">
    <header className="books-header"><a className="books-brand" href="/books">BookLoop</a>
      <div className="profile-header-actions"><a className="back-to-books" href="/books">Back to books</a>
        {profile ? <MemberHeaderActions member={{ displayName: profile.displayName, email: profile.email, city: profile.city }} unreadCount={unreadCount} onSignOut={signOut} /> : null}</div>
    </header>
    <section className="books-intro profile-intro"><div className="books-intro-copy"><p className="eyebrow">Your BookLoop account</p>
      <h1 className="form-title">Profile</h1><p className="welcome-copy">Manage the details other BookLoop members see.</p></div></section>
    <section className="add-book-panel profile-panel">
      {loading ? <p className="welcome-copy">Loading your profile...</p> : null}
      {error ? <p className="form-error" role="alert">{error}</p> : null}
      {message ? <p className="books-message" role="status">{message}</p> : null}
      {!loading && profile ? <>
        <form className="signup-form" onSubmit={saveProfile}>
          <h2>Profile details</h2>
          <label htmlFor="profile-name">Display name</label><input id="profile-name" required maxLength={255} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          <label htmlFor="profile-city">City</label><input id="profile-city" required maxLength={255} value={city} onChange={(event) => setCity(event.target.value)} />
          <label htmlFor="profile-email">Email address</label><input id="profile-email" type="email" value={profile.email} readOnly />
          <p className="field-help">{profile.emailVerified ? 'Current email address is verified.' : 'Current email address is not verified.'}</p>
          <p className="field-help">Your city is public on your book listings. Saving a new city updates all of your existing listings.</p>
          <button className="primary-button" disabled={saving}>{saving ? 'Saving...' : 'Save changes'}</button>
        </form>
        <div className="profile-email-section">
          <h2>Change email</h2>
          <p className="field-help">We’ll email a verification link to the new address and notify your current address. The current email stays active until verification.</p>
          {profile.pendingEmail ? <p className="pending-email-notice" role="status">Waiting for {profile.pendingEmail} to be verified.</p> : null}
          <form className="signup-form" onSubmit={requestEmailChange}>
            <label htmlFor="profile-new-email">New email address</label><input id="profile-new-email" type="email" autoComplete="email" required value={newEmail} onChange={(event) => setNewEmail(event.target.value)} />
            <label htmlFor="profile-current-password">Current password</label><input id="profile-current-password" type="password" autoComplete="current-password" required value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} />
            <button className="primary-button" disabled={emailBusy}>{emailBusy ? 'Sending verification...' : 'Send verification link'}</button>
          </form>
        </div>
      </> : null}
    </section>
  </main>
}

function BooksPage() {
  const [member, setMember] = useState<Member | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [books, setBooks] = useState<BookListing[]>([])
  const [mainTab, setMainTab] = useState<'browse' | 'upload' | 'mine'>('browse')
  const [offerFilter, setOfferFilter] = useState<'ALL' | BookOfferType>('ALL')
  const [titleFilter, setTitleFilter] = useState('')
  const [genreFilter, setGenreFilter] = useState('')
  const [cityFilter, setCityFilter] = useState('')
  const [title, setTitle] = useState('')
  const [author, setAuthor] = useState('')
  const [selectedGenres, setSelectedGenres] = useState<string[]>([])
  const [condition, setCondition] = useState<BookCondition>('GOOD')
  const [offerType, setOfferType] = useState<BookOfferType>('EXCHANGE')
  const [askingPrice, setAskingPrice] = useState('')
  const [saleBillFile, setSaleBillFile] = useState<File | null>(null)
  const [isbn, setIsbn] = useState('')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [requestBook, setRequestBook] = useState<BookListing | null>(null)
  const [myExchangeBooks, setMyExchangeBooks] = useState<BookListing[]>([])
  const [offeredBookId, setOfferedBookId] = useState('')
  const [requestBusy, setRequestBusy] = useState(false)
  const [verifyingListingId, setVerifyingListingId] = useState<string | null>(null)
  const [markingSaleSoldId, setMarkingSaleSoldId] = useState<string | null>(null)
  const [sendingInterestFor, setSendingInterestFor] = useState<string | null>(null)
  const [sendingFreeClaimFor, setSendingFreeClaimFor] = useState<string | null>(null)
  const [unreadCount, setUnreadCount] = useState(0)

  // Fetches member-only book listings using the selected title, genre, and city filters.
  async function loadBooks(authToken: string, filters = { title: titleFilter, genre: genreFilter, city: cityFilter, offerType: offerFilter }) {
    setBusy(true)
    try {
      const params = new URLSearchParams()
      if (filters.title.trim()) params.set('title', filters.title.trim())
      if (filters.genre) params.set('genre', filters.genre)
      if (filters.city.trim()) params.set('city', filters.city.trim())
      if (filters.offerType !== 'ALL') params.set('offerType', filters.offerType)
      const response = await fetch(`${apiBaseUrl}/api/books?${params}`, { headers: { Authorization: `Bearer ${authToken}` } })
      if (!response.ok) throw new Error(await readError(response, 'Could not load book listings.'))
      setBooks(await response.json())
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not load book listings.')
    } finally { setBusy(false) }
  }

  // Loads only books owned by the signed-in member for the My Books tab.
  async function loadMyBooks(authToken: string) {
    setBusy(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/books/mine`, { headers: { Authorization: `Bearer ${authToken}` } })
      if (!response.ok) throw new Error(await readError(response, 'Could not load your books.'))
      setBooks(await response.json())
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not load your books.')
    } finally { setBusy(false) }
  }

  // Restores the refresh-cookie session, then loads books for the authenticated member.
  useEffect(() => {
    let active = true
    refreshSession().then(async (session) => {
      if (!active) return
      if (!session) { window.location.assign('/login'); return }
      if (!session.member.city) { window.location.assign('/login'); return }
      accessToken = session.accessToken
      setToken(session.accessToken)
      setMember(session.member)
    }).catch(() => {
      if (active) { setMessage('Could not restore your session. Please sign in again.'); setBusy(false) }
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!token) return
    if (mainTab === 'mine') void loadMyBooks(token)
    if (mainTab === 'browse') void loadBooks(token)
  }, [mainTab, token])

  // Refreshes the combined incoming-request and unread-message badge while Books is open.
  useEffect(() => {
    if (!token) return
    let active = true
    const refresh = () => loadUnreadCount(token).then((count) => { if (active) setUnreadCount(count) }).catch(() => {})
    void refresh()
    const timer = window.setInterval(refresh, 15_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [token])

  // Loads the member's available exchange listings when they choose to propose an exchange.
  async function beginExchangeRequest(book: BookListing) {
    if (!token) return
    setMessage('')
    setRequestBook(book)
    setRequestBusy(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/books/mine`, { headers: { Authorization: `Bearer ${token}` } })
      if (!response.ok) throw new Error(await readError(response, 'Could not load your Exchange books.'))
      const choices = (await response.json() as BookListing[]).filter((item) => item.offerType === 'EXCHANGE' && item.availability === 'AVAILABLE')
      setMyExchangeBooks(choices)
      setOfferedBookId(choices[0]?.id || '')
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Could not load your Exchange books.')
      setRequestBook(null)
    } finally { setRequestBusy(false) }
  }

  // Submits the selected pair of Exchange listings as a pending request to the book owner.
  async function submitExchangeRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !requestBook || !offeredBookId || requestBusy) return
    setRequestBusy(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/exchanges`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ requestedListingId: requestBook.id, offeredListingId: offeredBookId }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not send the exchange request.'))
      setMessage('Exchange request sent. You can follow it on the Exchanges page.')
      setRequestBook(null)
      setOfferFilter('EXCHANGE')
      await loadBooks(token, { title: titleFilter, genre: genreFilter, city: cityFilter, offerType: 'EXCHANGE' })
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Could not send the exchange request.')
    } finally { setRequestBusy(false) }
  }

  // Switches between browsing, uploading, and the current member's own listings.
  function selectMainTab(tab: 'browse' | 'upload' | 'mine') {
    setMessage('')
    setMainTab(tab)
  }

  // Filters Browse to one offer type and fetches the matching listings.
  async function selectOfferTab(offer: 'ALL' | BookOfferType) {
    setOfferFilter(offer)
    setMessage('')
    if (token) await loadBooks(token, { title: titleFilter, genre: genreFilter, city: cityFilter, offerType: offer })
  }

  // Applies the current title, genre, and city filters to the visible listings.
  async function searchBooks(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) return
    setMessage('')
    await loadBooks(token, { title: titleFilter, genre: genreFilter, city: cityFilter, offerType: offerFilter })
  }

  // Uploads a selected bill and runs its private OCR check; returns an ID only when the price passes.
  async function uploadAndCheckBill(authToken: string, file: File, price: number) {
    const form = new FormData()
    form.append('file', file)
    const uploadResponse = await fetch(`${apiBaseUrl}/api/sale-bills`, {
      method: 'POST', headers: { Authorization: `Bearer ${authToken}` }, body: form,
    })
    if (!uploadResponse.ok) throw new Error(await readError(uploadResponse, 'Could not upload the bill.'))
    const uploaded = await uploadResponse.json() as SaleBillUpload
    const checkResponse = await fetch(`${apiBaseUrl}/api/sale-bills/${uploaded.id}/check?askingPrice=${encodeURIComponent(price.toFixed(2))}`, {
      method: 'POST', headers: { Authorization: `Bearer ${authToken}` },
    })
    const check = await checkResponse.json().catch(() => ({})) as SaleBillCheck & { detail?: string }
    if (!checkResponse.ok) throw new Error(check.detail || 'Could not check the bill.')
    if (!check.priceCheckPassed) throw new Error(check.detail || 'The bill did not pass the price check. You can publish without a bill as unverified.')
    return uploaded.id
  }

  // Verifies an existing unverified sale after the seller uploads a bill that passes the current price check.
  async function verifyExistingSale(book: BookListing, file: File) {
    if (!token || verifyingListingId) return
    setVerifyingListingId(book.id)
    setMessage('')
    try {
      const billId = await uploadAndCheckBill(token, file, book.askingPrice || 0)
      const response = await fetch(`${apiBaseUrl}/api/books/${book.id}/sale-verification`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ saleBillDocumentId: billId }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not attach the checked bill to this listing.'))
      setMessage(`OCR price check passed for “${book.title}”.`)
      await loadMyBooks(token)
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Could not verify this sale listing.')
    } finally { setVerifyingListingId(null) }
  }

  // Marks an owned Sale listing sold so it leaves Browse and its other pending interests close.
  async function markSaleListingSold(book: BookListing) {
    if (!token || markingSaleSoldId || !window.confirm(`Mark “${book.title}” as sold? It will be removed from Browse and pending interests will close.`)) return
    setMarkingSaleSoldId(book.id)
    setMessage('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/books/${book.id}/sold`, {
        method: 'POST', headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not mark this book sold.'))
      await loadMyBooks(token)
      setUnreadCount(await loadUnreadCount(token))
      setMessage(`“${book.title}” is marked sold and no longer appears in Browse.`)
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Could not mark this book sold.')
    } finally { setMarkingSaleSoldId(null) }
  }

  // Sends one authenticated member's interest in another member's available Sale listing.
  async function sendSaleInterest(book: BookListing) {
    if (!token || sendingInterestFor) return
    setSendingInterestFor(book.id)
    setMessage('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/sale-interests`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ listingId: book.id }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not send your interest.'))
      setMessage(`Your interest in “${book.title}” was sent. The seller can review it in Exchanges.`)
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Could not send your interest.')
    } finally { setSendingInterestFor(null) }
  }

  // Sends a member's request to the owner of an available Free listing.
  async function sendFreeClaim(book: BookListing) {
    if (!token || sendingFreeClaimFor) return
    setSendingFreeClaimFor(book.id)
    setMessage('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/free-claims`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ listingId: book.id }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not request this Free book.'))
      setMessage(`Your request for “${book.title}” was sent. The owner can review it in Exchanges.`)
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Could not request this Free book.')
    } finally { setSendingFreeClaimFor(null) }
  }

  // Validates and posts a listing, then reloads the results so the new book is visible.
  async function createListing(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || submitting) return
    setSubmitting(true)
    setMessage('')
    try {
      let saleBillDocumentId: string | null = null
      if (offerType === 'SALE' && saleBillFile) {
        saleBillDocumentId = await uploadAndCheckBill(token, saleBillFile, Number(askingPrice))
      }
      const response = await fetch(`${apiBaseUrl}/api/books`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ title, author, genres: selectedGenres, condition, offerType,
          askingPrice: offerType === 'SALE' ? Number(askingPrice) : null, isbn: isbn || null, description: description || null,
          saleBillDocumentId }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not create this listing.'))
      setTitle(''); setAuthor(''); setSelectedGenres([]); setAskingPrice(''); setSaleBillFile(null); setIsbn(''); setDescription('')
      setMessage(offerType === 'SALE' && !saleBillDocumentId
        ? 'Your sale listing is published as unverified. You can add a bill later from My Books.'
        : 'Your book is listed.')
      setMainTab('browse')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not create this listing.')
    } finally { setSubmitting(false) }
  }

  // Revokes the session and returns the member to sign-in.
  async function signOut() {
    const csrfResponse = await fetch(`${apiBaseUrl}/api/auth/csrf`, { credentials: 'include' })
    if (!csrfResponse.ok) return
    const csrf = await csrfResponse.json()
    const response = await fetch(`${apiBaseUrl}/api/auth/logout`, {
      method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': csrf.token },
    })
    if (response.ok) { accessToken = null; window.location.assign('/login') }
  }

  return <main className="books-page">
    <header className="books-header"><a className="books-brand" href="/books">BookLoop</a>
      {member ? <MemberHeaderActions member={member} unreadCount={unreadCount} onSignOut={signOut} /> : null}
    </header>
    <section className="books-intro"><div className="books-intro-copy"><p className="eyebrow">Your local book community</p><h1 className="form-title">Books for readers</h1>
      <p className="welcome-copy">Discover books in your community, share one, or manage your listings.</p></div>
      <img className="books-hero-image" src="/bookloop-community.png" alt="Readers exchanging a book beside a stack of books and a plant" />
    </section>
    <nav className="main-tabs" aria-label="Book sections" role="tablist">
      <button role="tab" aria-selected={mainTab === 'browse'} className={mainTab === 'browse' ? 'main-tab active' : 'main-tab'} onClick={() => selectMainTab('browse')}>Browse books</button>
      <button role="tab" aria-selected={mainTab === 'upload'} className={mainTab === 'upload' ? 'main-tab active' : 'main-tab'} onClick={() => selectMainTab('upload')}>Upload a book</button>
      <button role="tab" aria-selected={mainTab === 'mine'} className={mainTab === 'mine' ? 'main-tab active' : 'main-tab'} onClick={() => selectMainTab('mine')}>My Books</button>
    </nav>
    <section className="books-layout">
      {mainTab === 'browse' ? <div className="browse-column">
        <nav className="offer-tabs" aria-label="Filter books by offer" role="tablist">
          {(['ALL', 'SALE', 'FREE', 'EXCHANGE'] as const).map((offer) => <button key={offer} role="tab" aria-selected={offerFilter === offer} className={offerFilter === offer ? 'offer-tab active' : 'offer-tab'} onClick={() => void selectOfferTab(offer)}>{offer === 'ALL' ? 'All books' : offer === 'SALE' ? 'Sale' : offer === 'FREE' ? 'Free' : 'Exchange'}</button>)}
        </nav>
        {offerFilter === 'SALE' ? <p className="sale-notice">Sale prices are set by the seller. The OCR tick means the asking price was below the paid amount read from the uploaded bill; it does not authenticate the bill.</p> : null}
        <form className="search-panel" onSubmit={searchBooks}>
          <label htmlFor="search-title">Title</label><input id="search-title" value={titleFilter} onChange={(e) => setTitleFilter(e.target.value)} placeholder="Search by title" />
          <label htmlFor="search-genre">Genre</label><select id="search-genre" value={genreFilter} onChange={(e) => setGenreFilter(e.target.value)}><option value="">All genres</option>{genres.map((item) => <option key={item}>{item}</option>)}</select>
          <label htmlFor="search-city">City</label><input id="search-city" value={cityFilter} onChange={(e) => setCityFilter(e.target.value)} placeholder="Search by city" />
          <button className="primary-button" disabled={!token || busy}>Search books</button>
        </form>
        <div className="listing-heading"><h2>Available books</h2><span>{books.length} {books.length === 1 ? 'book' : 'books'}</span></div>
        {message ? <p className="books-message" role="status">{message}</p> : null}
        {busy ? <p className="welcome-copy">Loading books...</p> : books.length === 0 ? <p className="empty-state">No books match those filters yet. You can add one.</p> :
          <div className="book-grid">{books.map((book) => <article className="book-card" key={book.id}>
            <div className="book-card-top"><span className="offer-badge">{book.offerType === 'SALE' ? 'For sale' : book.offerType === 'FREE' ? 'Free' : 'Exchange'}</span><span className="book-condition">{book.condition.replace('_', ' ')}</span></div>
            <h3>{book.title}</h3><p className="book-author">by {book.author}</p><p className="book-genres">{book.genres.join(' · ')}</p>
            {book.askingPrice !== null ? <p className="book-price">₹{book.askingPrice.toFixed(2)}</p> : null}
            {book.offerType === 'SALE' ? <span className={book.salePriceVerified ? 'sale-verification verified' : 'sale-verification'}>{book.salePriceVerified ? '✓ OCR price check passed' : 'Price unverified'}</span> : null}
            {book.description ? <p className="book-description">{book.description}</p> : null}
            {book.offerType === 'SALE' && book.ownerId !== member?.id ? <button className="primary-button" disabled={sendingInterestFor === book.id} onClick={() => void sendSaleInterest(book)}>{sendingInterestFor === book.id ? 'Sending...' : 'I’m interested'}</button> : null}
            {book.offerType === 'FREE' && book.ownerId !== member?.id ? <button className="primary-button" disabled={sendingFreeClaimFor === book.id} onClick={() => void sendFreeClaim(book)}>{sendingFreeClaimFor === book.id ? 'Sending request…' : 'Request free book'}</button> : null}
            {book.offerType === 'EXCHANGE' && book.ownerId !== member?.id ? <button className="primary-button" onClick={() => void beginExchangeRequest(book)}>Request exchange</button> : null}
            <footer className="book-card-footer"><span>{book.ownerName}</span><span>{book.city}</span></footer>
          </article>)}</div>}
        {requestBook ? <div className="exchange-request-panel" role="dialog" aria-modal="true" aria-labelledby="exchange-request-title">
          <form className="add-book-panel" onSubmit={submitExchangeRequest}>
            <h2 id="exchange-request-title">Offer a book for “{requestBook.title}”</h2>
            {requestBusy && myExchangeBooks.length === 0 ? <p className="welcome-copy">Loading your available Exchange books...</p> : myExchangeBooks.length ? <><label htmlFor="offered-book">Your Exchange book</label><select id="offered-book" required value={offeredBookId} onChange={(event) => setOfferedBookId(event.target.value)}>{myExchangeBooks.map((book) => <option key={book.id} value={book.id}>{book.title} by {book.author}</option>)}</select><p className="field-help">The owner can accept or decline this request. The books are reserved only after acceptance.</p><button className="primary-button" disabled={requestBusy}>{requestBusy ? 'Sending...' : 'Send request'}</button></> : <p className="empty-state">You need an available Exchange listing before requesting this book. Add one in Upload a book.</p>}
            <button className="text-button" type="button" onClick={() => setRequestBook(null)}>Close</button>
          </form>
        </div> : null}
      </div> : null}
      {mainTab === 'mine' ? <div className="browse-column">
        <div className="listing-heading"><h2>My Books</h2><span>{books.length} {books.length === 1 ? 'listing' : 'listings'}</span></div>
        {message ? <p className="books-message" role="status">{message}</p> : null}
        {busy ? <p className="welcome-copy">Loading your listings...</p> : books.length === 0 ? <p className="empty-state">You haven’t listed any books yet. Open Upload a book to share one.</p> :
          <div className="book-grid">{books.map((book) => <article className="book-card" key={book.id}>
            <div className="book-card-top"><span className="offer-badge">{book.offerType === 'SALE' ? 'For sale' : book.offerType === 'FREE' ? 'Free' : 'Exchange'}</span><span className="book-condition">{book.condition.replace('_', ' ')}</span></div>
            <h3>{book.title}</h3><p className="book-author">by {book.author}</p><p className="book-genres">{book.genres.join(' · ')}</p>
            {book.askingPrice !== null ? <p className="book-price">INR {book.askingPrice.toFixed(2)}</p> : null}
            {book.offerType === 'SALE' ? <><span className={book.salePriceVerified ? 'sale-verification verified' : 'sale-verification'}>{book.salePriceVerified ? '✓ OCR price check passed' : 'Price unverified'}</span>
              {!book.salePriceVerified ? <div className="verify-sale-control"><label htmlFor={`sale-bill-${book.id}`}>Add bill to request OCR price check</label><input id={`sale-bill-${book.id}`} type="file" accept="image/jpeg,image/png,application/pdf" disabled={verifyingListingId === book.id} onChange={(event) => { const file = event.target.files?.[0]; if (file) void verifyExistingSale(book, file); event.currentTarget.value = '' }} /><span className="field-help">JPEG, PNG, or PDF · up to 5 MB · up to 10 PDF pages</span>{verifyingListingId === book.id ? <span className="field-help">Checking bill…</span> : null}</div> : null}
            </> : null}
            <span className="availability-badge">{book.availability === 'RESERVED' ? 'Reserved' : book.availability === 'EXCHANGED' ? 'Exchange completed' : book.availability === 'SOLD' ? 'Sold' : 'Available'}</span>
            {book.offerType === 'SALE' && !['SOLD', 'EXCHANGED'].includes(book.availability)
              ? <button className="secondary-button mark-sold-button" disabled={markingSaleSoldId === book.id} onClick={() => void markSaleListingSold(book)}>{markingSaleSoldId === book.id ? 'Marking sold…' : 'Mark as sold'}</button>
              : null}
            <footer className="book-card-footer"><span>{book.ownerName}</span><span>{book.city}</span></footer>
          </article>)}</div>}
      </div> : null}
      {mainTab === 'upload' ? <aside className="add-book-panel upload-tab-panel"><p className="eyebrow">Share a book</p><h2>Upload a listing</h2>
        <form className="signup-form" onSubmit={createListing}>
          <label htmlFor="book-title">Book title</label><input id="book-title" required maxLength={255} value={title} onChange={(e) => setTitle(e.target.value)} />
          <label htmlFor="book-author">Author</label><input id="book-author" required maxLength={255} value={author} onChange={(e) => setAuthor(e.target.value)} />
          <label htmlFor="book-genres">Genres (select one or more)</label><select id="book-genres" multiple required value={selectedGenres} onChange={(e) => setSelectedGenres(Array.from(e.target.selectedOptions, (option) => option.value))}>{genres.map((item) => <option key={item}>{item}</option>)}</select>
          <label htmlFor="book-condition">Condition</label><select id="book-condition" value={condition} onChange={(e) => setCondition(e.target.value as BookCondition)}><option value="NEW">New</option><option value="LIKE_NEW">Like new</option><option value="GOOD">Good</option><option value="FAIR">Fair</option></select>
          <label htmlFor="offer-type">Offer type</label><select id="offer-type" value={offerType} onChange={(e) => setOfferType(e.target.value as BookOfferType)}><option value="EXCHANGE">Exchange</option><option value="FREE">Give it free</option><option value="SALE">Sell</option></select>
          {offerType === 'SALE' ? <><label htmlFor="asking-price">Sale price (INR)</label><input id="asking-price" type="number" min="0.01" max="99999999.99" step="0.01" required value={askingPrice} onChange={(e) => setAskingPrice(e.target.value)} /><label htmlFor="sale-bill-upload">Purchase bill (optional)</label><input id="sale-bill-upload" type="file" accept="image/jpeg,image/png,application/pdf" onChange={(e) => setSaleBillFile(e.target.files?.[0] || null)} /><span className="field-help">Without a bill, the listing is published as unverified. With a bill, OCR must find a paid amount above your sale price before publication. JPEG, PNG, or PDF up to 5 MB; PDFs up to 10 pages. The bill remains private.</span></> : <p className="field-help">Sale listings without a bill are marked unverified. You can add a bill later in My Books.</p>}
          <label htmlFor="book-isbn">ISBN (optional)</label><input id="book-isbn" maxLength={20} value={isbn} onChange={(e) => setIsbn(e.target.value)} />
          <label htmlFor="book-description">Description (optional)</label><textarea id="book-description" maxLength={4000} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          <p className="field-help">The listing uses your profile city: {member?.city}.</p>
          <button className="primary-button" disabled={!token || submitting}>{submitting ? 'Publishing...' : 'Publish listing'}</button>
        </form>
      </aside> : null}
    </section>
  </main>
}

function ExchangesPage() {
  const [member, setMember] = useState<Member | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [exchanges, setExchanges] = useState<ExchangeItem[]>([])
  const [saleInterests, setSaleInterests] = useState<SaleInterestItem[]>([])
  const [freeClaims, setFreeClaims] = useState<FreeClaimItem[]>([])
  const [inboxTab, setInboxTab] = useState<'exchange' | 'sale' | 'free'>('exchange')
  const [selectedFreeClaim, setSelectedFreeClaim] = useState<string | null>(null)
  const [freeMessages, setFreeMessages] = useState<FreeClaimMessage[]>([])
  const [freeDraft, setFreeDraft] = useState('')
  const [selectedSaleInterest, setSelectedSaleInterest] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [saleMessages, setSaleMessages] = useState<SaleChatMessage[]>([])
  const [presence, setPresence] = useState<Presence | null>(null)
  const [draft, setDraft] = useState('')
  const [saleDraft, setSaleDraft] = useState('')
  const [sharedBillFile, setSharedBillFile] = useState<File | null>(null)
  const [redactedConfirmed, setRedactedConfirmed] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  // Loads exchanges for the authenticated member without changing request review state.
  async function loadExchanges(authToken: string) {
    const response = await fetch(`${apiBaseUrl}/api/exchanges`, { headers: { Authorization: `Bearer ${authToken}` } })
    if (!response.ok) throw new Error(await readError(response, 'Could not load exchanges.'))
    setExchanges(await response.json() as ExchangeItem[])
  }

  // Loads Sale interest requests where the member is either seller or interested buyer.
  async function loadSaleInterests(authToken: string) {
    const response = await fetch(`${apiBaseUrl}/api/sale-interests`, { headers: { Authorization: `Bearer ${authToken}` } })
    if (!response.ok) throw new Error(await readError(response, 'Could not load Sale interest requests.'))
    setSaleInterests(await response.json() as SaleInterestItem[])
  }

  // Loads a Sale conversation's messages; the API limits access to its accepted buyer and seller.
  async function loadSaleMessages(authToken: string, interestId: string, after?: string) {
    const query = after ? `?after=${encodeURIComponent(after)}` : ''
    const response = await fetch(`${apiBaseUrl}/api/sale-interests/${interestId}/messages${query}`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })
    if (!response.ok) throw new Error(await readError(response, 'Could not load the Sale conversation.'))
    return await response.json() as SaleChatMessage[]
  }

  // Loads Free claims only for the signed-in member as requester or listing owner.
  async function loadFreeClaims(authToken: string) {
    const response = await fetch(`${apiBaseUrl}/api/free-claims`, { headers: { Authorization: `Bearer ${authToken}` } })
    if (!response.ok) throw new Error(await readError(response, 'Could not load Free-book claims.'))
    setFreeClaims(await response.json() as FreeClaimItem[])
  }

  // Loads private Free-claim chat history; the API marks received messages read for the participant.
  async function loadFreeMessages(authToken: string, claimId: string, after?: string) {
    const query = after ? `?after=${encodeURIComponent(after)}` : ''
    const response = await fetch(`${apiBaseUrl}/api/free-claims/${claimId}/messages${query}`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })
    if (!response.ok) throw new Error(await readError(response, 'Could not load the Free-book conversation.'))
    return await response.json() as FreeClaimMessage[]
  }

  // Restores the signed-in session and opens the member's exchange inbox.
  useEffect(() => {
    let active = true
    refreshSession().then(async (session) => {
      if (!active) return
      if (!session || !session.member.city) { window.location.assign('/login'); return }
      accessToken = session.accessToken
      setMember(session.member)
      setToken(session.accessToken)
      try { await Promise.all([loadExchanges(session.accessToken), loadSaleInterests(session.accessToken), loadFreeClaims(session.accessToken)]) }
      catch (cause) { if (active) setError(cause instanceof Error ? cause.message : 'Could not load requests.') }
    }).catch(() => { if (active) setError('Could not restore your BookLoop session.') })
    return () => { active = false }
  }, [])

  // Refreshes exchange status and the combined badge while the Exchanges page is open.
  useEffect(() => {
    if (!token) return
    let active = true
    const refresh = async () => {
      try { await Promise.all([loadExchanges(token), loadSaleInterests(token), loadFreeClaims(token)]); const count = await loadUnreadCount(token); if (active) setUnreadCount(count) }
      catch (cause) { if (active) setError(cause instanceof Error ? cause.message : 'Could not refresh exchanges.') }
    }
    const timer = window.setInterval(() => { void refresh() }, 15_000)
    void loadUnreadCount(token).then((count) => { if (active) setUnreadCount(count) }).catch(() => {})
    return () => { active = false; window.clearInterval(timer) }
  }, [token])

  // Marks an incoming pending Sale interest as reviewed when the seller opens its card.
  async function openSaleInterest(item: SaleInterestItem) {
    setSelectedSaleInterest(item.id)
    setSelected(null); setMessages([]); setSaleMessages([]); setPresence(null); setError('')
    if (!token) return
    if (item.status === 'ACCEPTED') {
      try {
        setSaleMessages(await loadSaleMessages(token, item.id))
        setUnreadCount(await loadUnreadCount(token))
      } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not load the Sale conversation.') }
      return
    }
    if (!item.incoming || item.status !== 'PENDING') return
    try {
      const response = await fetch(`${apiBaseUrl}/api/sale-interests/${item.id}/review`, {
        method: 'POST', headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not open this interest request.'))
      setUnreadCount(await loadUnreadCount(token))
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not open this interest request.') }
  }

  // Accepts or declines a seller-side Sale interest and refreshes both members' request state.
  async function performSaleInterestAction(id: string, action: 'accept' | 'decline', reserveListing = false) {
    if (!token || busy) return
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/sale-interests/${id}/${action}`, {
        method: 'POST',
        headers: action === 'accept'
          ? { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
          : { Authorization: `Bearer ${token}` },
        ...(action === 'accept' ? { body: JSON.stringify({ reserveListing }) } : {}),
      })
      if (!response.ok) throw new Error(await readError(response, `Could not ${action} this interest.`))
      await loadSaleInterests(token)
      if (action === 'accept') {
        setSelectedSaleInterest(id)
        setSaleMessages(await loadSaleMessages(token, id))
      }
      setUnreadCount(await loadUnreadCount(token))
    } catch (cause) { setError(cause instanceof Error ? cause.message : `Could not ${action} this interest.`) }
    finally { setBusy(false) }
  }

  // Marks the Sale listing from its accepted chat; the API closes only its other pending interests.
  async function markSaleInterestSold(item: SaleInterestItem) {
    if (!token || busy || !window.confirm(`Mark “${item.book.title}” as sold? Other pending interests will close.`)) return
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/books/${item.book.id}/sold`, {
        method: 'POST', headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not mark this book sold.'))
      await loadSaleInterests(token)
      setUnreadCount(await loadUnreadCount(token))
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not mark this book sold.') }
    finally { setBusy(false) }
  }

  // Opens a Free-claim request, marks incoming pending claims reviewed, or loads accepted chat history.
  async function openFreeClaim(item: FreeClaimItem) {
    setSelectedFreeClaim(item.id); setSelected(null); setSelectedSaleInterest(null)
    setFreeMessages([]); setMessages([]); setSaleMessages([]); setPresence(null); setError('')
    if (!token) return
    if (item.incoming && item.status === 'PENDING') {
      try {
        const response = await fetch(`${apiBaseUrl}/api/free-claims/${item.id}/review`, {
          method: 'POST', headers: { Authorization: `Bearer ${token}` },
        })
        if (!response.ok) throw new Error(await readError(response, 'Could not open this Free-book claim.'))
        setUnreadCount(await loadUnreadCount(token))
      } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not open this Free-book claim.') }
    }
    if (item.status === 'ACCEPTED' || item.status === 'CANCELLED') {
      try {
        setFreeMessages(await loadFreeMessages(token, item.id))
        setUnreadCount(await loadUnreadCount(token))
      } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not load the Free-book conversation.') }
    }
  }

  // Lets the owner accept or decline, or either participant cancel, then refreshes requests and chat state.
  async function performFreeClaimAction(id: string, action: 'accept' | 'decline' | 'cancel') {
    if (!token || busy) return
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/free-claims/${id}/${action}`, {
        method: 'POST', headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error(await readError(response, `Could not ${action} this Free-book claim.`))
      await loadFreeClaims(token)
      if (action === 'accept') {
        setSelectedFreeClaim(id)
        setFreeMessages(await loadFreeMessages(token, id))
      } else if (action === 'cancel' && selectedFreeClaim === id) {
        setFreeMessages(await loadFreeMessages(token, id))
      }
      setUnreadCount(await loadUnreadCount(token))
    } catch (cause) { setError(cause instanceof Error ? cause.message : `Could not ${action} this Free-book claim.`) }
    finally { setBusy(false) }
  }

  // Sends a text-only private message while the selected Free claim is accepted.
  async function sendFreeChatMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !selectedFreeClaim || !freeDraft.trim() || busy) return
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/free-claims/${selectedFreeClaim}/messages`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ text: freeDraft }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not send your message.'))
      const sent = await response.json() as FreeClaimMessage
      setFreeMessages((current) => [...current, sent]); setFreeDraft('')
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not send your message.') }
    finally { setBusy(false) }
  }

  // Sends a private text message to the other member in the accepted Sale conversation.
  async function sendSaleChatMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !selectedSaleInterest || !saleDraft.trim() || busy) return
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/sale-interests/${selectedSaleInterest}/messages`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ text: saleDraft }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not send your message.'))
      const sent = await response.json() as SaleChatMessage
      setSaleMessages((current) => [...current, sent])
      setSaleDraft('')
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not send your message.') }
    finally { setBusy(false) }
  }

  // Shares only the seller's selected redacted bill copy inside the private accepted Sale conversation.
  async function shareSaleBill(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !selectedSaleInterest || !sharedBillFile || !redactedConfirmed || busy) return
    setBusy(true); setError('')
    try {
      const form = new FormData()
      form.append('file', sharedBillFile)
      const response = await fetch(`${apiBaseUrl}/api/sale-interests/${selectedSaleInterest}/messages/bill`, {
        method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form,
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not share the bill copy.'))
      const shared = await response.json() as SaleChatMessage
      setSaleMessages((current) => [...current, shared])
      setSharedBillFile(null); setRedactedConfirmed(false)
      const picker = document.getElementById('sale-shared-bill') as HTMLInputElement | null
      if (picker) picker.value = ''
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not share the bill copy.') }
    finally { setBusy(false) }
  }

  // Downloads a shared bill through the authenticated participant-only API and releases its temporary URL.
  async function downloadSharedBill(message: SaleChatMessage) {
    if (!token || !selectedSaleInterest || !message.sharedBillId) return
    try {
      const response = await fetch(`${apiBaseUrl}/api/sale-interests/${selectedSaleInterest}/messages/${message.id}/bill`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not download the shared bill.'))
      const objectUrl = URL.createObjectURL(await response.blob())
      const anchor = document.createElement('a')
      anchor.href = objectUrl
      anchor.download = message.sharedBillFilename || 'shared-redacted-bill'
      document.body.append(anchor); anchor.click(); anchor.remove()
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not download the shared bill.') }
  }

  // Fetches conversation history when an accepted, cancelled, or completed exchange is opened.
  async function openConversation(item: ExchangeItem) {
    if (!token || !['ACCEPTED', 'CANCELLED', 'COMPLETED'].includes(item.status)) return
    setSelected(item.id)
    setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/exchanges/${item.id}/messages`, { headers: { Authorization: `Bearer ${token}` } })
      if (!response.ok) throw new Error(await readError(response, 'Could not load conversation.'))
      setMessages(await response.json() as ChatMessage[])
      const count = await loadUnreadCount(token)
      setUnreadCount(count)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not load conversation.') }
  }

  // Opens one request and marks only that incoming pending request as reviewed.
  async function openExchange(item: ExchangeItem) {
    if (!token) return
    setSelected(item.id); setPresence(null); setMessages([])
    if (item.incoming && item.status === 'PENDING') {
      try {
        const response = await fetch(`${apiBaseUrl}/api/exchanges/${item.id}/review`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
        if (!response.ok) throw new Error(await readError(response, 'Could not open this request.'))
        setUnreadCount(await loadUnreadCount(token))
      } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not open this request.') }
    }
    if (['ACCEPTED', 'CANCELLED', 'COMPLETED'].includes(item.status)) await openConversation(item)
  }

  // Polls new chat messages every five seconds while the selected conversation is open.
  useEffect(() => {
    if (!token || !selected) return
    const item = exchanges.find((candidate) => candidate.id === selected)
    if (!item || !['ACCEPTED', 'CANCELLED', 'COMPLETED'].includes(item.status)) return
    let active = true
    const poll = async () => {
      const after = messages.length ? `?after=${encodeURIComponent(messages[messages.length - 1].createdAt)}` : ''
      try {
        const response = await fetch(`${apiBaseUrl}/api/exchanges/${selected}/messages${after}`, { headers: { Authorization: `Bearer ${token}` } })
        if (!response.ok) return
        const incoming = await response.json() as ChatMessage[]
        if (active && incoming.length) setMessages((current) => [...current, ...incoming.filter((message) => !current.some((existing) => existing.id === message.id))])
        const count = await loadUnreadCount(token)
        if (active) setUnreadCount(count)
      } catch { /* Transient polling errors retry on the next interval. */ }
    }
    const timer = window.setInterval(() => { void poll() }, 5_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [token, selected, exchanges, messages])

  // Polls the selected accepted Sale conversation every five seconds and refreshes the unread badge.
  useEffect(() => {
    if (!token || !selectedSaleInterest) return
    const item = saleInterests.find((candidate) => candidate.id === selectedSaleInterest)
    if (!item || item.status !== 'ACCEPTED') return
    let active = true
    const poll = async () => {
      const after = saleMessages.length ? saleMessages[saleMessages.length - 1].createdAt : undefined
      try {
        const incoming = await loadSaleMessages(token, selectedSaleInterest, after)
        if (active && incoming.length) setSaleMessages((current) => [
          ...current,
          ...incoming.filter((message) => !current.some((existing) => existing.id === message.id)),
        ])
        const count = await loadUnreadCount(token)
        if (active) setUnreadCount(count)
      } catch { /* Temporary polling failures retry on the next interval. */ }
    }
    const timer = window.setInterval(() => { void poll() }, 5_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [token, selectedSaleInterest, saleInterests, saleMessages])

  // Polls an active Free-claim chat every five seconds and updates the shared unread badge.
  useEffect(() => {
    if (!token || !selectedFreeClaim) return
    const item = freeClaims.find((candidate) => candidate.id === selectedFreeClaim)
    if (!item || item.status !== 'ACCEPTED') return
    let active = true
    const poll = async () => {
      const after = freeMessages.length ? freeMessages[freeMessages.length - 1].createdAt : undefined
      try {
        const incoming = await loadFreeMessages(token, selectedFreeClaim, after)
        if (active && incoming.length) setFreeMessages((current) => [
          ...current,
          ...incoming.filter((message) => !current.some((existing) => existing.id === message.id)),
        ])
        const count = await loadUnreadCount(token)
        if (active) setUnreadCount(count)
      } catch { /* Temporary polling failures retry on the next interval. */ }
    }
    const timer = window.setInterval(() => { void poll() }, 5_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [token, selectedFreeClaim, freeClaims, freeMessages])

  // Records chat presence every 30 seconds and displays only the other exchange participant's status.
  useEffect(() => {
    if (!token || !selected) return
    const item = exchanges.find((candidate) => candidate.id === selected)
    if (!item || !['ACCEPTED', 'CANCELLED', 'COMPLETED'].includes(item.status)) return
    let active = true
    const poll = async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/exchanges/${selected}/presence`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
        if (response.ok && active) setPresence(await response.json() as Presence)
      } catch { /* Presence will be refreshed on the next heartbeat. */ }
    }
    void poll()
    const timer = window.setInterval(poll, 30_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [token, selected, exchanges])

  // Performs an authorized exchange action and refreshes both participants' visible request state.
  async function performAction(id: string, action: 'accept' | 'decline' | 'cancel' | 'complete') {
    if (!token || busy) return
    setBusy(true); setError('')
    try {
      const response = await fetch(`${apiBaseUrl}/api/exchanges/${id}/${action}`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
      if (!response.ok) throw new Error(await readError(response, `Could not ${action} this exchange.`))
      await loadExchanges(token)
      if (action === 'accept') {
        const accepted = exchanges.find((item) => item.id === id)
        if (accepted) await openConversation({ ...accepted, status: 'ACCEPTED' })
      }
    } catch (cause) { setError(cause instanceof Error ? cause.message : `Could not ${action} this exchange.`) }
    finally { setBusy(false) }
  }

  // Sends plain text in the active exchange chat and clears the composer after success.
  async function sendChatMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !selected || !draft.trim() || busy) return
    setBusy(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/exchanges/${selected}/messages`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ text: draft }),
      })
      if (!response.ok) throw new Error(await readError(response, 'Could not send message.'))
      const sent = await response.json() as ChatMessage
      setMessages((current) => [...current, sent])
      setDraft('')
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not send message.') }
    finally { setBusy(false) }
  }

  // Revokes the refresh session and returns the member to sign-in.
  async function signOut() {
    const csrfResponse = await fetch(`${apiBaseUrl}/api/auth/csrf`, { credentials: 'include' })
    if (!csrfResponse.ok) return
    const csrf = await csrfResponse.json()
    const response = await fetch(`${apiBaseUrl}/api/auth/logout`, { method: 'POST', credentials: 'include', headers: { 'X-XSRF-TOKEN': csrf.token } })
    if (response.ok) { accessToken = null; window.location.assign('/login') }
  }

  const selectedExchange = exchanges.find((item) => item.id === selected) || null
  const selectedSaleInterestItem = saleInterests.find((item) => item.id === selectedSaleInterest) || null
  const selectedFreeClaimItem = freeClaims.find((item) => item.id === selectedFreeClaim) || null
  const incomingSaleInterestCount = saleInterests.filter((item) => item.incoming && item.status === 'PENDING').length
  const incomingFreeClaimCount = freeClaims.filter((item) => item.incoming && item.status === 'PENDING').length
  const partnerName = selectedExchange ? selectedExchange.incoming ? selectedExchange.requesterName : selectedExchange.ownerName : ''
  const activeHistory = ['ACCEPTED', 'CANCELLED', 'COMPLETED'].includes(selectedExchange?.status || '')
  return <main className="books-page">
    <header className="books-header"><a className="books-brand" href="/books">BookLoop</a>{member ? <MemberHeaderActions member={member} unreadCount={unreadCount} onSignOut={signOut} /> : null}</header>
    <section className="books-intro profile-intro"><div className="books-intro-copy"><p className="eyebrow">Your book community</p><h1 className="form-title">Exchanges</h1><p className="welcome-copy">Review requests, coordinate accepted exchanges, and keep a record of completed or cancelled exchanges.</p></div></section>
    {error ? <p className="form-error" role="alert">{error}</p> : null}
    <section className="exchanges-layout">
      <div className="browse-column">
        <nav className="offer-tabs request-tabs" aria-label="Request inbox" role="tablist">
          <button role="tab" aria-selected={inboxTab === 'exchange'} className={inboxTab === 'exchange' ? 'offer-tab active' : 'offer-tab'} onClick={() => { setInboxTab('exchange'); setSelectedSaleInterest(null); setSelectedFreeClaim(null) }}>Exchanges</button>
          <button role="tab" aria-selected={inboxTab === 'sale'} className={inboxTab === 'sale' ? 'offer-tab active' : 'offer-tab'} onClick={() => { setInboxTab('sale'); setSelected(null); setSelectedFreeClaim(null) }}>Sale interests{incomingSaleInterestCount ? ` (${incomingSaleInterestCount})` : ''}</button>
          <button role="tab" aria-selected={inboxTab === 'free'} className={inboxTab === 'free' ? 'offer-tab active' : 'offer-tab'} onClick={() => { setInboxTab('free'); setSelected(null); setSelectedSaleInterest(null) }}>Free claims{incomingFreeClaimCount ? ` (${incomingFreeClaimCount})` : ''}</button>
        </nav>
        {inboxTab === 'exchange' ? <section><h2>Requests and history</h2>{exchanges.length === 0 ? <p className="empty-state">No exchange requests yet. Browse books to request an exchange.</p> : <div className="exchange-list">
        {exchanges.map((item) => <article className={selected === item.id ? 'exchange-card selected' : 'exchange-card'} key={item.id}>
          <button className="exchange-card-select" onClick={() => void openExchange(item)}>
            <span className="exchange-card-title">{item.incoming ? `${item.requesterName} wants your book` : `Your request to ${item.ownerName}`}</span>
            <span>{item.requestedBook.title} ⇄ {item.offeredBook.title}</span><span className="exchange-status">{item.status.toLowerCase()}</span>
          </button>
          {item.incoming && item.status === 'PENDING' ? <div className="exchange-actions"><button className="primary-button" disabled={busy} onClick={() => void performAction(item.id, 'accept')}>Accept</button><button className="secondary-button" disabled={busy} onClick={() => void performAction(item.id, 'decline')}>Decline</button></div> : null}
          {['PENDING', 'ACCEPTED'].includes(item.status) ? <button className="text-button" disabled={busy} onClick={() => void performAction(item.id, 'cancel')}>Cancel request</button> : null}
          {item.status === 'ACCEPTED' ? <button className="secondary-button" disabled={busy || (item.incoming ? Boolean(item.ownerCompletedAt) : Boolean(item.requesterCompletedAt))} onClick={() => void performAction(item.id, 'complete')}>{(item.incoming ? item.ownerCompletedAt : item.requesterCompletedAt) ? 'You confirmed completion' : 'Confirm exchange complete'}</button> : null}
          {item.status === 'ACCEPTED' ? <button className="text-button" onClick={() => void openConversation(item)}>Open chat</button> : null}
        </article>)}
       </div>}</section> : inboxTab === 'sale' ? <section><h2>Sale interests</h2>{saleInterests.length === 0 ? <p className="empty-state">No Sale interest requests yet. Browse Sale listings to express interest in a book.</p> : <div className="exchange-list">
         {saleInterests.map((item) => <article className={selectedSaleInterest === item.id ? 'exchange-card selected' : 'exchange-card'} key={item.id}>
           <button className="exchange-card-select" onClick={() => void openSaleInterest(item)}>
             <span className="exchange-card-title">{item.incoming ? `${item.requesterName} is interested in your book` : `Your interest in ${item.ownerName}’s book`}</span>
             <span>{item.book.title}{item.book.askingPrice !== null ? ` · INR ${item.book.askingPrice.toFixed(2)}` : ''}</span><span className="exchange-status">{item.status.toLowerCase()}</span>
           </button>
           {item.incoming && item.status === 'PENDING' ? <div className="sale-interest-actions"><button className="primary-button" disabled={busy} onClick={() => void performSaleInterestAction(item.id, 'accept', true)}>Accept · reserve book</button><button className="secondary-button" disabled={busy} onClick={() => void performSaleInterestAction(item.id, 'accept', false)}>Accept · keep listed</button><button className="text-button" disabled={busy} onClick={() => void performSaleInterestAction(item.id, 'decline')}>Decline</button></div> : null}
           {item.status === 'ACCEPTED' ? <p className="field-help">Accepted{item.incoming && item.reserveListing ? '; listing reserved' : ''}{item.book.availability === 'SOLD' ? '; marked sold' : ''}. Private chat is open.</p> : null}
           {item.incoming && item.status === 'ACCEPTED' && item.book.availability !== 'SOLD' ? <button className="secondary-button" disabled={busy} onClick={() => void markSaleInterestSold(item)}>Mark book sold</button> : null}
           {item.incoming && item.status === 'CLOSED' ? <p className="field-help">This request closed because the listing was reserved or marked sold.</p> : null}
         </article>)}
       </div>}</section> : <section><h2>Free claims</h2>{freeClaims.length === 0 ? <p className="empty-state">No Free-book claims yet. Browse Free books to request one.</p> : <div className="exchange-list">
         {freeClaims.map((item) => <article className={selectedFreeClaim === item.id ? 'exchange-card selected' : 'exchange-card'} key={item.id}>
           <button className="exchange-card-select" onClick={() => void openFreeClaim(item)}>
             <span className="exchange-card-title">{item.incoming ? `${item.requesterName} requested your Free book` : `Your request to ${item.ownerName}`}</span>
             <span>{item.book.title}</span><span className="exchange-status">{item.status.toLowerCase()}</span>
           </button>
           {item.incoming && item.status === 'PENDING' ? <div className="exchange-actions"><button className="primary-button" disabled={busy} onClick={() => void performFreeClaimAction(item.id, 'accept')}>Accept · reserve book</button><button className="secondary-button" disabled={busy} onClick={() => void performFreeClaimAction(item.id, 'decline')}>Decline</button></div> : null}
           {['PENDING', 'ACCEPTED'].includes(item.status) ? <button className="text-button" disabled={busy} onClick={() => void performFreeClaimAction(item.id, 'cancel')}>Cancel claim</button> : null}
           {item.status === 'ACCEPTED' ? <p className="field-help">The book is reserved. Use private chat to arrange pickup.</p> : null}
           {item.status === 'CLOSED' ? <p className="field-help">This claim closed because another claimant was accepted.</p> : null}
           {item.status === 'CANCELLED' ? <p className="field-help">The claim was cancelled. The book is available again.</p> : null}
         </article>)}
       </div>}</section>}
      </div>
      <aside className="add-book-panel exchange-chat-panel"><h2>{selectedFreeClaimItem ? `Chat with ${selectedFreeClaimItem.incoming ? selectedFreeClaimItem.requesterName : selectedFreeClaimItem.ownerName}` : selectedSaleInterestItem ? `Chat with ${selectedSaleInterestItem.incoming ? selectedSaleInterestItem.requesterName : selectedSaleInterestItem.ownerName}` : selectedExchange ? `Chat with ${partnerName}` : 'Exchange chat'}</h2>
        {selectedFreeClaimItem && ['ACCEPTED', 'CANCELLED'].includes(selectedFreeClaimItem.status) ? <>
          <div className="chat-history" aria-live="polite">{freeMessages.map((item) => <article className={item.senderId === member?.id ? 'chat-message own' : 'chat-message'} key={item.id}><strong>{item.senderName}</strong><p>{item.text}</p><time>{new Date(item.createdAt).toLocaleString()}</time></article>)}{freeMessages.length === 0 ? <p className="empty-state">No messages yet. Arrange a pickup with the book owner.</p> : null}</div>
          {selectedFreeClaimItem.status === 'ACCEPTED' ? <form className="chat-composer" onSubmit={sendFreeChatMessage}><label htmlFor="free-chat-draft">Message</label><textarea id="free-chat-draft" maxLength={2000} required value={freeDraft} onChange={(event) => setFreeDraft(event.target.value)} /><button className="primary-button" disabled={busy || !freeDraft.trim()}>Send</button></form> : <p className="field-help">This chat is read-only because the claim was cancelled. The listing is available again.</p>}
        </> : null}
        {selectedFreeClaimItem && !['ACCEPTED', 'CANCELLED'].includes(selectedFreeClaimItem.status) ? <p className="empty-state">Private chat opens when the owner accepts the request. This claim is {selectedFreeClaimItem.status.toLowerCase()}.</p> : null}
        {selectedSaleInterestItem?.status === 'ACCEPTED' ? <>
          <div className="chat-history" aria-live="polite">{saleMessages.map((item) => <article className={item.senderId === member?.id ? 'chat-message own' : 'chat-message'} key={item.id}>
            <strong>{item.senderName}</strong>
            {item.text ? <p>{item.text}</p> : null}
            {item.sharedBillId ? <button className="text-button shared-bill-download" onClick={() => void downloadSharedBill(item)}>Download shared bill: {item.sharedBillFilename}</button> : null}
            <time>{new Date(item.createdAt).toLocaleString()}</time>
          </article>)}{saleMessages.length === 0 ? <p className="empty-state">No messages yet. Contact each other to discuss the book.</p> : null}</div>
          {selectedSaleInterestItem.incoming ? <form className="share-bill-form" onSubmit={shareSaleBill}>
            <label htmlFor="sale-shared-bill">Share a redacted bill copy (optional)</label>
            <input id="sale-shared-bill" type="file" accept="image/jpeg,image/png,application/pdf" required onChange={(event) => { setSharedBillFile(event.target.files?.[0] || null); setRedactedConfirmed(false) }} />
            <span className="field-help">JPEG, PNG, or PDF up to 5 MB. Redact personal and payment details before uploading. Only this buyer can access the copy.</span>
            <label className="redacted-confirmation"><input type="checkbox" required checked={redactedConfirmed} onChange={(event) => setRedactedConfirmed(event.target.checked)} /> I confirm this is a redacted copy I intend to share.</label>
            <button className="secondary-button" disabled={busy || !sharedBillFile || !redactedConfirmed}>{busy ? 'Sharing…' : 'Share bill copy'}</button>
          </form> : null}
          <form className="chat-composer" onSubmit={sendSaleChatMessage}><label htmlFor="sale-chat-draft">Message</label><textarea id="sale-chat-draft" maxLength={2000} required value={saleDraft} onChange={(event) => setSaleDraft(event.target.value)} /><button className="primary-button" disabled={busy || !saleDraft.trim()}>Send</button></form>
        </> : null}
        {selectedSaleInterestItem && selectedSaleInterestItem.status !== 'ACCEPTED' ? <p className="empty-state">Sale chat opens after the seller accepts an interest request. This request is {selectedSaleInterestItem.status.toLowerCase()}.</p> : null}
        {selectedExchange && activeHistory ? <><p className="presence-label">{presence ? presence.online ? 'Online now' : presence.lastActiveAt ? `Last active ${formatLastActive(presence.lastActiveAt)}` : 'Status unavailable' : 'Checking status…'}</p>
          <div className="chat-history" aria-live="polite">{messages.map((item) => <article className={item.senderId === member?.id ? 'chat-message own' : 'chat-message'} key={item.id}><strong>{item.senderName}</strong><p>{item.text}</p><time>{new Date(item.createdAt).toLocaleString()}</time></article>)}{messages.length === 0 ? <p className="empty-state">No messages yet. Say hello to coordinate the exchange.</p> : null}</div>
          {selectedExchange.status === 'ACCEPTED' ? <form className="chat-composer" onSubmit={sendChatMessage}><label htmlFor="chat-draft">Message</label><textarea id="chat-draft" maxLength={2000} required value={draft} onChange={(event) => setDraft(event.target.value)} /><button className="primary-button" disabled={busy || !draft.trim()}>Send</button></form> : <p className="field-help">This conversation is read-only because the exchange is {selectedExchange.status.toLowerCase()}.</p>}
        </> : selectedFreeClaimItem || selectedSaleInterestItem ? null : <p className="empty-state">Open an accepted exchange to chat. Chat history remains available after cancellation or completion.</p>}
      </aside>
    </section>
  </main>
}

// Formats partner activity as a short relative time label for the exchange conversation.
function formatLastActive(value: string) {
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60_000))
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? '' : 's'} ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`
  const days = Math.floor(hours / 24)
  return `${days} day${days === 1 ? '' : 's'} ago`
}

function App() {
  const pathname = window.location.pathname.replace(/\/+$/, '') || '/'
  if (pathname === '/login') return <LoginPage />
  if (pathname === '/books') return <BooksPage />
  if (pathname === '/exchanges') return <ExchangesPage />
  if (pathname === '/profile') return <ProfilePage />
  if (pathname === '/forgot-password') return <ForgotPasswordPage />
  if (pathname === '/reset-password') {
    return <ResetPasswordPage token={new URLSearchParams(window.location.search).get('token')} />
  }
  if (pathname === '/signup') {
    return <SignUpPage />
  }

  if (pathname === '/verify-email') {
    const token = new URLSearchParams(window.location.search).get('token')
    return <VerifyEmailPage token={token} />
  }

  return (
    <main className="app-shell">
      <section className="welcome-card" aria-labelledby="app-title">
        <p className="eyebrow">Peer-to-peer book sharing</p>
        <h1 id="app-title">BookLoop</h1>
        <p className="welcome-copy">
          The frontend foundation is ready. We can shape the marketplace
          experience one feature at a time.
        </p>
        <a className="primary-button signup-link" href="/signup">Create an account</a>
        <p className="form-footer"><a href="/login">Sign in</a></p>
      </section>
    </main>
  )
}

export default App
