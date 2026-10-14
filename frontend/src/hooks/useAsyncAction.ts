import { useState } from "react";

// Manages the loading and error states of requests that modify backend data.
// Run the action, ensuring the UI is updated only after the backend confirms the change.
// Each hook call keeps its own state, so independent actions use separate calls.
export function useAsyncAction() {
    const [pending, setPending] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    async function runAction(request: () => Promise<void>, failureMessage: string): Promise<boolean> {
        setPending(true);
        setErrorMessage(null);

        try {
            await request();
            return true;
        } catch {
            setErrorMessage(failureMessage);
            return false;
        } finally {
            setPending(false);
        }
    }

    function clearError() {
        setErrorMessage(null);
    }

    return { pending, errorMessage, runAction, clearError };
}