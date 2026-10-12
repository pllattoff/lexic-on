import { Link } from "react-router";

export default function NotFoundPage() {
    return (
        <div className="mx-auto max-w-3xl p-4">
            <h1 className="mb-2 text-lg font-medium text-heading">Page not found</h1>
            <Link to="/" className="underline">Back to the text</Link>
        </div>
    );
}