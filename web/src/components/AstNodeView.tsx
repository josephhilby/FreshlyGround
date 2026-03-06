import { useState } from "react";

type AstNode = {
    node: string;
    [key: string]: any;
};

function isBranchKey(key: string) {
    return (
        key === "fields" ||
        key === "methods" ||
        key === "statements" ||
        key === "thenStatements" ||
        key === "elseStatements"
    );
}

function isInlineKey(key: string) {
    return (
        key === "value" ||
        key === "expression" ||
        key === "arguments" ||
        key === "receiver"
    );
}

function nodeClass(nodeType: string) {
    if (nodeType === "Source" || nodeType === "Method" || nodeType === "Field") {
        return "ast-node structural";
    }

    if (
        nodeType === "Declaration" ||
        nodeType === "Assignment" ||
        nodeType === "Return" ||
        nodeType === "If" ||
        nodeType === "For" ||
        nodeType === "While"
    ) {
        return "ast-node statement";
    }

    return "ast-node expression";
}

function renderLabel(node: AstNode) {
    return (
        <div className="ast-label">
            <div className="ast-kind">{node.node}</div>

            {node.name && <div className="ast-chip">{String(node.name)}</div>}
            {node.operator && <div className="ast-chip">{String(node.operator)}</div>}
            {node.typeName && <div className="ast-chip">{String(node.typeName)}</div>}
            {node.returnTypeName && (
                <div className="ast-chip">{String(node.returnTypeName)}</div>
            )}

            {node.node === "Field" && node.value && (
                <div className="ast-equals">=</div>
            )}

            {node.literal !== undefined && node.literal !== null && (
                <div className="ast-chip">{String(node.literal)}</div>
            )}
        </div>
    );
}

function childEntries(node: AstNode) {
    return Object.entries(node).filter(([key, value]) => {
        if (key === "node") return false;
        if (value === null || value === undefined) return false;

        if (Array.isArray(value)) {
            if (value.length === 0) return false;

            return value.some(
                (child) => child && typeof child === "object" && "node" in child
            );
        }

        if (typeof value !== "object") return false;

        return "node" in value;
    });
}

function InlineAstNode({ node }: { node: AstNode }) {
    const children = childEntries(node);
    const inlineChildren = children.filter(([key]) => isInlineKey(key));

    return (
        <div
            className={`ast-inline-node ${nodeClass(node.node)}`}
            onClick={(e) => e.stopPropagation()}
        >
            {renderLabel(node)}

            {inlineChildren.length > 0 && (
                <div className="ast-inline-children">
                    {inlineChildren.map(([key, value]) => (
                        <div key={key} className="ast-inline-group">

                            {Array.isArray(value) ? (
                                <div className="ast-inline-list">
                                    {key === "arguments" && <span className="ast-paren">(</span>}

                                    {value.map((child, i) =>
                                        child && typeof child === "object" && "node" in child ? (
                                            <InlineAstNode key={`${key}-${i}`} node={child} />
                                        ) : null
                                    )}

                                    {key === "arguments" && <span className="ast-paren">)</span>}
                                </div>
                            ) : value && typeof value === "object" && "node" in value ? (
                                <InlineAstNode node={value} />
                            ) : null}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default function AstNodeView({ node }: { node: AstNode }) {
    const [collapsed, setCollapsed] = useState(false);

    const children = childEntries(node);

    if (
        node.node === "Expression" &&
        children.length === 1 &&
        children[0][1] &&
        typeof children[0][1] === "object" &&
        !Array.isArray(children[0][1]) &&
        "node" in children[0][1]
    ) {
        return <AstNodeView node={children[0][1]} />;
    }

    const branchChildren = children.filter(([key]) => isBranchKey(key));
    const inlineChildren = children.filter(([key]) => isInlineKey(key));
    const hasChildren = branchChildren.length > 0;

    return (
        <div className="ast-branch">
            <div
                className={`${nodeClass(node.node)} ${hasChildren ? "clickable" : ""}`}
                onClick={hasChildren ? () => setCollapsed(!collapsed) : undefined}
            >
                {renderLabel(node)}

                {inlineChildren.length > 0 && (
                    <div className="ast-inline-children">
                        {inlineChildren.map(([key, value]) => (
                            <div key={key} className="ast-inline-group">
                                {Array.isArray(value) ? (
                                    <div className="ast-inline-list">
                                        {key === "arguments" && <span className="ast-paren">(</span>}

                                        {value.map((child, i) =>
                                            child && typeof child === "object" && "node" in child ? (
                                                <InlineAstNode key={`${key}-${i}`} node={child} />
                                            ) : null
                                        )}

                                        {key === "arguments" && <span className="ast-paren">)</span>}
                                    </div>
                                ) : value && typeof value === "object" && "node" in value ? (
                                    <InlineAstNode node={value} />
                                ) : null}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {hasChildren && !collapsed && (
                <div className="ast-children">
                    {branchChildren.flatMap(([key, value]) => {
                        if (Array.isArray(value)) {
                            return value.map((child, i) =>
                                child && typeof child === "object" && "node" in child ? (
                                    <div key={`${key}-${i}`} className="ast-child-group">
                                        <AstNodeView node={child} />
                                    </div>
                                ) : null
                            );
                        }

                        return value && typeof value === "object" && "node" in value ? (
                            <div key={key} className="ast-child-group">
                                <AstNodeView node={value} />
                            </div>
                        ) : null;
                    })}
                </div>
            )}
        </div>
    );
}