export type CompileResponse = {
    code: string;
};

export type TokenResponse = Array<{
    type: string;
    literal: string | number | boolean | null;
    index: number;
}>;

export type AstResponse = {
    node: string;
    [key: string]: unknown;
};

const API_BASE_URL = import.meta.env.VITE_API_URL?.replace(/\/$/, "") ?? "http://localhost:7070";

async function postJson<T>(path: string, source: string): Promise<T> {
    const res = await fetch(`${API_BASE_URL}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source }),
    });

    const text = await res.text();
    const json = text ? JSON.parse(text) : null;

    if (!res.ok) {
        const message =
            json?.message ??
            json?.error ??
            `${res.status} ${res.statusText}`;

        throw new Error(
            `Request failed (${res.status}) [${path}]: ${message}`
        );
    }

    return json as T;
}

export async function compileSource(source: string): Promise<CompileResponse> {
    return postJson<CompileResponse>("/compile", source);
}

export async function getTokens(source: string): Promise<TokenResponse> {
    return postJson<TokenResponse>("/tokens", source);
}

export async function getAst(source: string): Promise<AstResponse> {
    return postJson<AstResponse>("/ast", source);
}