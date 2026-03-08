import { useState } from "react";
import "./ast.css";
import AstNodeLabel from "./AstNodeLabel";
import AstNodeView from "./AstNodeView";
import type { AstNode } from "./nodeUtils";
import { childEntries, isInlineKey, nodeClass } from "./nodeUtils";

export default function AstIfNode({ node }: { node: AstNode }) {
    const [collapsed, setCollapsed] = useState(false);

    const children = childEntries(node);
    const inlineChildren = children.filter(([key]) => isInlineKey(key));

    const thenStatements = Array.isArray(node.thenStatements) ? node.thenStatements : [];
    const elseStatements = Array.isArray(node.elseStatements) ? node.elseStatements : [];

    const hasThenStatements = thenStatements.some(
        (child) => child && typeof child === "object" && "node" in child
    );

    const hasElseStatements = elseStatements.some(
        (child) => child && typeof child === "object" && "node" in child
    );

    const hasChildren = hasThenStatements || hasElseStatements;

    function toggleCollapsed(e: React.MouseEvent) {
        e.stopPropagation();

        if (hasChildren) {
            setCollapsed((prev) => !prev);
        }
    }

    return (
        <div className="ast-branch">
            <div
                className={[
                    nodeClass(node.node),
                    hasChildren ? "clickable" : "",
                    hasChildren ? (collapsed ? "collapsed" : "expanded") : "",
                ].join(" ").trim()}
                onClick={hasChildren ? toggleCollapsed : undefined}
            >
                {hasChildren && <span className="ast-caret">▶</span>}

                <AstNodeLabel node={node} />

                {inlineChildren.length > 0 && (
                    <div className="ast-inline-children">
                        {inlineChildren.map(([key, value]) => (
                            <div key={key} className="ast-inline-group">
                                {Array.isArray(value) ? (
                                    <div className="ast-inline-list">
                                        {key === "arguments" && <span className="ast-paren">(</span>}

                                        {value.map((child, i) =>
                                            child && typeof child === "object" && "node" in child ? (
                                                <AstNodeView key={`${key}-${i}`} node={child} />
                                            ) : null
                                        )}

                                        {key === "arguments" && <span className="ast-paren">)</span>}
                                    </div>
                                ) : value && typeof value === "object" && "node" in value ? (
                                    <AstNodeView node={value} />
                                ) : null}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {hasChildren && !collapsed && (
                <div className="ast-children">
                    {hasThenStatements && (
                        <div className="ast-child-section">
                            <div className="ast-section-label">then</div>

                            {thenStatements.map((child, i) =>
                                child && typeof child === "object" && "node" in child ? (
                                    <div key={`then-${i}`} className="ast-child-group">
                                        <AstNodeView node={child} />
                                    </div>
                                ) : null
                            )}
                        </div>
                    )}

                    {hasElseStatements && (
                        <div className="ast-child-section">
                            <div className="ast-section-label">else</div>

                            {elseStatements.map((child, i) =>
                                child && typeof child === "object" && "node" in child ? (
                                    <div key={`else-${i}`} className="ast-child-group">
                                        <AstNodeView node={child} />
                                    </div>
                                ) : null
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}