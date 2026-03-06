import Modal from "./Modal";

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

function tokenClass(type: Token["type"]) {
    switch (type) {
        case "IDENTIFIER":
            return "token-chip identifier";
        case "INTEGER":
            return "token-chip integer";
        case "DECIMAL":
            return "token-chip decimal";
        case "CHARACTER":
            return "token-chip character";
        case "STRING":
            return "token-chip string";
        case "OPERATOR":
            return "token-chip operator";
    }
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
                    <div key={lineIndex} className="token-line">
                        {lineTokens.map((t, i) => (
                            <div key={i} className={tokenClass(t.type)}>
                                {t.literal}
                            </div>
                        ))}
                    </div>
                ))}
            </div>
        </Modal>
    );
}