import { useState } from "react";
import UserIcon from "../icons/UserIcon.tsx";

type UserAvatarProps = {
    avatarUrl: string | null;
    name: string;
};

export default function UserAvatar({ avatarUrl, name }: Readonly<UserAvatarProps>) {
    // The provider may send no picture, or its image host may refuse to serve it
    const [imageFailed, setImageFailed] = useState(false);

    if (avatarUrl && !imageFailed) {
        return (
            <img
                src={avatarUrl}
                alt={name}
                width={32}
                height={32}
                // Google's image host can refuse requests that carry a Referer from another site
                referrerPolicy="no-referrer"
                onError={() => setImageFailed(true)}
                className="rounded-full"
            />
        );
    }

    return (
        <span
            role="img"
            aria-label={name}
            className="flex size-8 items-center justify-center rounded-full bg-surface-2 text-body/70"
        >
            <UserIcon />
        </span>
    );
}
