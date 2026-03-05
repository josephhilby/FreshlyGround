import { useState } from "react";
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

    async function compile() {
        setError(null);

        const res = await fetch("/compile", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ source }),
        });

        const json = await res.json();

        if (!res.ok) {
            setError(json.message ?? "Compilation failed");
            setOutput("");
            return;
        }

        setOutput(json.code ?? "");
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

                <textarea className="editor output" value={output} readOnly/>

            </div>

            <button className="compile-button" onClick={compile}>
                Compile
            </button>

            {error && (
                <div className="error">
                    {error}
                </div>
            )}

        </div>
    );
}

export default App;