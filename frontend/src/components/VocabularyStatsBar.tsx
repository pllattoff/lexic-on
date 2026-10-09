import { STATUS_LABELS, STATUS_STYLES, VOCABULARY_STATUSES } from "../lib/vocabulary.ts";
import type { TextToken } from "../types/text.ts";

const UNTRACKED_BLOCK_CLASSNAME = "border-outline-strong bg-surface-2 text-body";

// Each word occurrence counts, so the percentages of all blocks add up to 100%
export default function StatusStatsBar({ tokens }: Readonly<{ tokens: TextToken[] }>) {
    if (tokens.length === 0) return null;

    const blocks = [
        {
            key: "UNTRACKED",
            label: "Untracked",
            count: tokens.filter((token) => token.status === null).length,
            className: UNTRACKED_BLOCK_CLASSNAME,
        },
        ...VOCABULARY_STATUSES.map((status) => ({
            key: status,
            label: STATUS_LABELS[status],
            count: tokens.filter((token) => token.status === status).length,
            className: STATUS_STYLES[status].block,
        })),
    ];

    return (
        <div className="grid w-full grid-cols-2 gap-2 sm:grid-cols-4">
            {blocks.map((block) => (
                <div key={block.key} className={`rounded-md border px-3 py-2 text-center ${block.className}`}>
                    <div className="font-medium">{block.label}</div>
                    <div className="text-sm">
                        {block.count} ({Math.round((block.count / tokens.length) * 100)}%)
                    </div>
                </div>
            ))}
        </div>
    );
}