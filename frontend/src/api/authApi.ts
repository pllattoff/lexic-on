import { fetchWithCsrf } from "../lib/csrf.ts";
import type { CurrentUser } from "../types/auth.ts";

// Return the current user if authenticated (/me endpoint requires authentication)
export async function getCurrentUser(): Promise<CurrentUser | null> {
    // Triggers the backend to create a CSRF cookie for later state-changing requests
    await fetch("/api/auth/csrf");

    const res = await fetch("/api/auth/me");

    return res.ok ? res.json() : null;
}

export async function logout(): Promise<Response> {
    return fetchWithCsrf("/logout", {
        method: "POST",
        credentials: "include",
    });
}