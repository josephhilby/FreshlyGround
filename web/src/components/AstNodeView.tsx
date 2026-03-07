import { useState } from "react";

type AstNode = {
    node: string;
    [key: string]: any;
};

const NODE_DEFINITIONS: Record<string, string> = {
    Source: `
source ::=
    { field }
    { method }`,

    Field: `
field ::=
    "LET" [ "CONST" ] name ":" type
    [ "=" value ]
    ";"`,

    Method: `
method ::=
    "DEF" name "(" [ param ":" paramType ] ")"
    [ ":" returnType ] "DO"
        { statements }
    "END"`,

    Declaration: `
declaration ::=
    "LET" name
    [ ":" type ]
    [ "=" value ]
    ";"`,

    Assignment: `
assignment ::=
    receiver "=" value ";"`,

    Expression: `
expression ::=
    expression ";"`,

    If: `
if ::=
    "IF" condition "DO"
        { thenStatements }
    [ "ELSE"
        { elseStatements } ]
    "END"`,

    For: `
for ::=
    "FOR" "(" [ initialization ] ";" condition ";" [ increment ] ")"
        { statements }
    "END"`,

    While: `
while ::=
    "WHILE" condition "DO"
        { statements }
    "END"`,

    Return: `
return ::=
    "RETURN" value ";"`,

    Binary: `
binary ::=
    left operator right`,

    Access: `
access ::=
    [ receiver "." ] name`,

    Function: `
function ::=
    [ receiver "." ] name "(" [ arguments ] ")"`,

    Group: `
group ::=
    "(" expression ")"`,

    Literal: `
literal ::=
    data`,
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
        key === "receiver" ||
        key === "left" ||
        key === "right" ||
        key === "condition" ||
        key === "parameters"
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
        nodeType === "Expression" ||
        nodeType === "If" ||
        nodeType === "For" ||
        nodeType === "While"
    ) {
        return "ast-node statement";
    }

    return "ast-node expression";
}

function renderMethodParams(node: AstNode) {
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

function renderLabel(node: AstNode) {
    return (
        <div className="ast-label">
            <div className="ast-kind-wrapper">
                <div className="ast-kind">{node.node}</div>
                <div className="ast-tooltip">
                    <pre>{(NODE_DEFINITIONS[node.node] ?? "").trim()}</pre>
                </div>
            </div>

            {node.name && <div className="ast-chip">{String(node.name)}</div>}

            {node.node === "Method" && (
                <>
                    <span className="ast-paren">(</span>
                    <span className="ast-paren">)</span>
                </>
            )}

            {(node.node === "Field" || node.node === "Declaration") && node.value && (
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

    if (node.node === "Binary") {
        const left = node.left;
        const right = node.right;

        return (
            <div
                className={`ast-inline-node ${nodeClass(node.node)}`}
                onClick={(e) => e.stopPropagation()}
            >
                {renderLabel(node)}

                {left && typeof left === "object" && "node" in left && (
                    <InlineAstNode node={left} />
                )}

                {node.operator && (
                    <div className="ast-chip">{String(node.operator)}</div>
                )}

                {right && typeof right === "object" && "node" in right && (
                    <InlineAstNode node={right} />
                )}
            </div>
        );
    }

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
    const branchChildren = children.filter(([key]) => isBranchKey(key));
    const inlineChildren = children.filter(([key]) => isInlineKey(key));
    const hasChildren = branchChildren.length > 0;

    return (
        <div className="ast-branch">
            <div
                className={[
                    nodeClass(node.node),
                    hasChildren ? "clickable" : "",
                    hasChildren ? (collapsed ? "collapsed" : "expanded") : "",
                ].join(" ").trim()}
                onClick={hasChildren ? () => setCollapsed(!collapsed) : undefined}
            >
                {hasChildren && (
                    <span className="ast-caret">▶</span>
                )}

                {renderLabel(node)}

                {inlineChildren.length > 0 && (
                    <div className="ast-inline-children">
                        {node.node === "Assignment" ? (
                            <div className="ast-inline-list">
                                {node.receiver &&
                                typeof node.receiver === "object" &&
                                "node" in node.receiver ? (
                                    <InlineAstNode node={node.receiver} />
                                ) : null}

                                <span className="ast-equals">=</span>

                                {node.value &&
                                typeof node.value === "object" &&
                                "node" in node.value ? (
                                    <InlineAstNode node={node.value} />
                                ) : null}
                            </div>
                        ) : (
                            inlineChildren.map(([key, value]) => (
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
                            ))
                        )}
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