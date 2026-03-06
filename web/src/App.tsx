import { useState } from "react";
import { compileSource, getTokens, getAst } from "./api/compiler";
import TokenModal from "./components/TokenModal";
import TreeModal from "./components/TreeModal";
import "./App.css";

function App() {
    const [source, setSource] = useState(
        `DEF main(): Integer DO
    print("hello world");
    RETURN 0;
END`
    );

    const [output, setOutput] = useState("");
    const [error, setError] = useState<string | null>(null);

    const [showTokens, setShowTokens] = useState(false);
    const [showAst, setShowAst] = useState(false);
    const [tokens, setTokens] = useState<unknown[]>([]);
    const [ast, setAst] = useState<unknown | null>(null);

    async function compile() {
        setError(null);

        try {
            const result = await compileSource(source);
            setOutput(result.code ?? "");
        } catch (e: any) {
            setError(e.message ?? "Compilation failed");
            setOutput("");
        }
    }

    async function openTokens() {
        setError(null);

        try {
            const result = await getTokens(source);
            setTokens(result);
            setShowTokens(true);
        } catch (e: any) {
            setError(e.message ?? "Failed to load tokens");
        }
    }

    async function openAst() {
        setError(null);

        try {
            const result = await getAst(source);
            setAst(result);
            setShowAst(true);
        } catch (e: any) {
            setError(e.message ?? "Failed to load AST");
        }
    }

    return (
        <div className="app">
            <div className="title">
                ☕ FreshlyGround Compiler
            </div>

            <div className="editor-container">
                <textarea
                    className="editor"
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                />

                <textarea className="editor output" value={output} readOnly />
            </div>

            <div className="button-row">
                <button className="compile-button" onClick={compile}>
                    Compile
                </button>

                <button className="compile-button" onClick={openTokens}>
                    Tokens
                </button>

                <button className="compile-button" onClick={openAst}>
                    AST
                </button>
            </div>

            {error && (
                <div className="error">
                    {error}
                </div>
            )}

            {showTokens && (
                <TokenModal
                    tokens={tokens as any[]}
                    source={source}
                    onClose={() => setShowTokens(false)}
                />
            )}

            {showAst && ast && (
                <TreeModal
                    ast={ast}
                    onClose={() => setShowAst(false)}
                />
            )}
        </div>
    );
}

export default App;