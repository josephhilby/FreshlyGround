import { useState } from "react";
import { compileSource, getTokens, getAst } from "./api/compiler";
import TokenModal from "./components/TokenModal";
import TreeModal from "./components/TreeModal";
import "./styles/app.css";

function App() {
    const [output, setOutput] = useState("");
    const [error, setError] = useState<string | null>(null);

    const [showTokens, setShowTokens] = useState(false);
    const [showAst, setShowAst] = useState(false);
    const [tokens, setTokens] = useState<any[]>([]);
    const [ast, setAst] = useState<any | null>(null);

    const [source, setSource] = useState(
        `// A quick tour of fields, methods, loops, conditionals, 
// and function calls.

LET brewCount: Integer;
LET drink: String = "coffee";

DEF pourMany(cup: String): Nil DO
    FOR ( brewCount = 0; brewCount < 3; brewCount = brewCount + 1 )
        print("pouring " + cup);
    END
END

DEF main(): Integer DO
    brewCount = 0;
    LET sleepyLevel: Integer = 5;

    WHILE brewCount < 6 DO
        IF brewCount < sleepyLevel DO
            pourMany(drink);
        ELSE
            print(drink.length());
        END

        brewCount = brewCount + 1;
    END

    RETURN 0;
END`
    );

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
                <div className="editor-wrapper">
                    <div className="line-numbers">
                        {source.split("\n").map((_, i) => (
                            <div key={i}>{i + 1}</div>
                        ))}
                    </div>

                    <textarea
                        id="left"
                        className="editor editor-left"
                        value={source}
                        wrap="off"
                        spellCheck={false}
                        onChange={(e) => setSource(e.target.value)}
                        onScroll={(e) => {
                            const wrapper = e.currentTarget.parentElement;
                            const gutter = wrapper?.querySelector(".line-numbers") as HTMLElement | null;

                            if (gutter) {
                                gutter.scrollTop = e.currentTarget.scrollTop;
                            }
                        }}
                    />
                </div>

                <textarea id="right" className="editor output" value={output} readOnly />
            </div>

            <div className="button-row">
                <button className="compile-button" onClick={openTokens}>
                    Tokens
                </button>

                <button className="compile-button" onClick={openAst}>
                    AST
                </button>

                <button className="compile-button" onClick={compile}>
                    Compile
                </button>
            </div>

            {error && (
                <div className="error">
                    {error}
                </div>
            )}

            {showTokens && (
                <TokenModal
                    tokens={tokens}
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