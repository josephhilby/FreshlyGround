import type { AstNode } from "./nodeUtils";
import { NODE_DEFINITIONS } from "./nodeDefinitions";

function renderParams(node: AstNode) {
    if (node.node !== "Method") return null;

    const params = Array.isArray(node.parameters) ? node.parameters : [];
    const types = Array.isArray(node.parameterTypeNames) ? node.parameterTypeNames : [];

    return (
        <div className="ast-inline-list">
            <span className="ast-paren">(</span>

            {params.map((param, i) => (
                <span key={i} className="ast-chip">
                    {String(param)}
                    {types[i] ? `: ${String(types[i])}` : ""}
                </span>
            ))}

            <span className="ast-paren">)</span>
        </div>
    );
}

export default function AstNodeLabel({ node }: { node: AstNode }) {
    return (
        <div className="ast-label">
            <div className="ast-kind-wrapper">
                <div className="ast-kind">{node.node}</div>
                <div className="ast-tooltip">
                    <pre>{(NODE_DEFINITIONS[node.node] ?? "").trim()}</pre>
                </div>
            </div>

            {node.name && <div className="ast-chip">{String(node.name)}</div>}

            {renderParams(node)}

            {(node.node === "Field" || node.node === "Declaration") && node.value && (
                <div className="ast-equals">=</div>
            )}

            {node.literal !== undefined && node.literal !== null && (
                <div className="ast-chip">{String(node.literal)}</div>
            )}
        </div>
    );
}