import Modal from "./Modal";
import Legend from "./Legend";
import TokenChip from "./token/TokenChip";
import type { Token } from "./token/tokenUtils";
import { computeLineStarts, tokenLine } from "./token/tokenUtils";

type TokenModalProps = {
    tokens: Token[];
    source: string;
    onClose: () => void;
};

export default function TokenModal({ tokens, source, onClose }: TokenModalProps) {
    const lineStarts = computeLineStarts(source);
    const lines: Token[][] = [];

    tokens.forEach((token) => {
        const line = tokenLine(token.index, lineStarts);

        if (!lines[line]) {
            lines[line] = [];
        }

        lines[line].push(token);
    });

    return (
        <Modal title="Tokens" onClose={onClose}>
            <div className="token-lines">
                {lines.map((lineTokens, lineIndex) => (
                    <div key={lineIndex} className="token-line-row">
                        <div className="token-line-number">{lineIndex + 1}</div>

                        <div className="token-line">
                            {lineTokens.map((token, i) => (
                                <TokenChip
                                    key={i}
                                    token={token}
                                    lineNumber={lineIndex}
                                />
                            ))}
                        </div>
                    </div>
                ))}
            </div>

            <Legend
                items={[
                    { label: "Identifier", className: "identifier" },
                    { label: "Numeric", className: "numeric" },
                    { label: "Character", className: "character" },
                    { label: "String", className: "string" },
                    { label: "Operator", className: "operator" },
                ]}
            />
        </Modal>
    );
}