function getCookie(name: string): string | null {
    const regex = new RegExp(`(?:^|; )${name}=([^;]*)`);
    const match = regex.exec(document.cookie);

    return match ? decodeURIComponent(match[1]) : null;
}

export function fetchWithCsrf(input: string, init: RequestInit = {}) {
    return fetch(input, {
        ...init,
        headers: {
            ...init.headers,
            "X-XSRF-TOKEN": getCookie("XSRF-TOKEN") ?? "",
        },
    });
}