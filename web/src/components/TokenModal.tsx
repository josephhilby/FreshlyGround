import Modal from "./Modal";
import Legend from "./Legend";

type Token = {
    type: "IDENTIFIER" | "INTEGER" | "DECIMAL" | "CHARACTER" | "STRING" | "OPERATOR";
    literal: string;
    index: number;
};

type TokenModalProps = {
    tokens: Token[];
    source: string;
    onClose: () => void;
};

const KEYWORDS = new Set([
    "LET",
    "CONST",
    "DEF",
    "DO",
    "END",
    "IF",
    "ELSE",
    "FOR",
    "WHILE",
    "RETURN",
]);

const PUNCTUATION = new Set(["(", ")", ":", ";", ","]);

function computeLineStarts(source: string) {
    const starts = [0];

    for (let i = 0; i < source.length; i++) {
        if (source[i] === "\n") {
            starts.push(i + 1);
        }
    }

    return starts;
}

function tokenLine(index: number, lineStarts: number[]) {
    let line = 0;

    while (line + 1 < lineStarts.length && index >= lineStarts[line + 1]) {
        line++;
    }

    return line;
}

function tokenClass(token: Token) {
    if (token.type === "IDENTIFIER" && KEYWORDS.has(token.literal)) {
        return "token-chip keyword";
    }

    if (token.type === "IDENTIFIER") {
        return "token-chip identifier";
    }

    if (token.type === "INTEGER" || token.type === "DECIMAL") {
        return "token-chip numeric";
    }

    if (token.type === "CHARACTER") {
        return "token-chip character";
    }

    if (token.type === "STRING") {
        return "token-chip string";
    }

    if (token.type === "OPERATOR" && PUNCTUATION.has(token.literal)) {
        return "token-chip punctuation";
    }

    return "token-chip operator";
}

function tokenTitle(token: Token, lineNumber: number) {
    return [
        `Type: ${token.type}`,
        `Literal: ${token.literal}`,
        `Index: ${token.index}`,
        `Line: ${lineNumber + 1}`,
    ].join("\n");
}

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
                            {lineTokens.map((t, i) => (
                                <div
                                    key={i}
                                    className={tokenClass(t)}
                                    title={tokenTitle(t, lineIndex)}
                                >
                                    {t.literal}
                                </div>
                            ))}
                        </div>
                    </div>
                ))}
            </div>

            <Legend
                items={[
                    { label: "Keyword", className: "keyword" },
                    { label: "Identifier", className: "identifier" },
                    { label: "Numeric", className: "numeric" },
                    { label: "Character", className: "character" },
                    { label: "String", className: "string" },
                    { label: "Punctuation", className: "punctuation" },
                    { label: "Operator", className: "operator" },
                ]}
            />
        </Modal>
    );
}