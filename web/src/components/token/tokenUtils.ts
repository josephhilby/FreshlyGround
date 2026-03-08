export type Token = {
    type: "IDENTIFIER" | "INTEGER" | "DECIMAL" | "CHARACTER" | "STRING" | "OPERATOR";
    literal: string;
    index: number;
};

export function computeLineStarts(source: string) {
    const starts = [0];

    for (let i = 0; i < source.length; i++) {
        if (source[i] === "\n") {
            starts.push(i + 1);
        }
    }

    return starts;
}

export function tokenLine(index: number, lineStarts: number[]) {
    let line = 0;

    while (line + 1 < lineStarts.length && index >= lineStarts[line + 1]) {
        line++;
    }

    return line;
}

export function tokenClass(token: Token) {
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

    return "token-chip operator";
}

export function tokenTitle(token: Token, lineNumber: number) {
    return [
        `Type: ${token.type}`,
        `Literal: ${token.literal}`,
        `Index: ${token.index}`,
        `Line: ${lineNumber + 1}`,
    ].join("\n");
}