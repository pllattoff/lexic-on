import { useEffect, useState } from 'react'

type Me = { login: string; email: string | null; avatarUrl: string | null }

function getCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
    return match ? decodeURIComponent(match[1]) : null
}

export default function AccountMenu() {
    const [me, setMe] = useState<Me | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        fetch('/api/auth/me')
            .then((res) => (res.ok ? res.json() : null))
            .then(setMe)
            .finally(() => setLoading(false))
    }, [])

    async function postLogout() {
        return fetch('/logout', {
            method: 'POST',
            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') ?? '' },
        })
    }

    async function handleLogout() {
        let res = await postLogout()
        if (res.status === 403) {
            // CSRF token rotates on successful login; the cookie read right after
            // a fresh login can be stale. The rejected request re-syncs it, so a
            // single retry with the fresh value succeeds.
            res = await postLogout()
        }
        setMe(null)
    }

    if (loading) return null

    if (!me) {
        return <a href="/oauth2/authorization/github">Continue with GitHub</a>
    }

    return (
        <button onClick={handleLogout} title={me.email ?? me.login}>
            {me.avatarUrl && <img src={me.avatarUrl} alt={me.login} width={32} height={32} className="rounded-full" />}
        </button>
    )
}
