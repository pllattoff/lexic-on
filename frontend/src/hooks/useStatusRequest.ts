import { useState } from "react";

// Manages the loading and error states of vocabulary status requests.
// Shared by the word popup, the dictionary word list and adding all untracked words.
export function useStatusRequest(failureMessage = "Couldn't save the status. Try again.") {
    const [pending, setPending] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Handle status requests, ensuring the UI is updated only after the backend confirms the change.
    // Returns whether the request succeeded.
    async function runStatusRequest(request: () => Promise<void>): Promise<boolean> {
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

    return { pending, errorMessage, runStatusRequest, clearError };
}