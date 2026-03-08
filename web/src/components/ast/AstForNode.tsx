import { useState } from "react";
import "./ast.css";
import AstNodeLabel from "./AstNodeLabel";
import AstNodeView from "./AstNodeView";
import type { AstNode } from "./nodeUtils";
import { nodeClass } from "./nodeUtils";

export default function AstForNode({ node }: { node: AstNode }) {
    const [collapsed, setCollapsed] = useState(false);

    const init = node.initialization;
    const condition = node.condition;
    const inc = node.increment;

    const statements = Array.isArray(node.statements) ? node.statements : [];

    const hasChildren = statements.length > 0;

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

                <div className="ast-for-header">
                    <span className="ast-paren">(</span>

                    <div className="ast-for-control">
                        {init && (
                            <div className="ast-for-line">
                                <AstNodeView node={init} />
                                <span className="ast-for-semi">;</span>
                            </div>
                        )}

                        {condition && (
                            <div className="ast-for-line">
                                <AstNodeView node={condition} />
                                <span className="ast-for-semi">;</span>
                            </div>
                        )}

                        {inc && (
                            <div className="ast-for-line">
                                <AstNodeView node={inc} />
                            </div>
                        )}
                    </div>

                    <span className="ast-paren">)</span>
                </div>
            </div>

            {hasChildren && !collapsed && (
                <div className="ast-children">
                    {statements.map((child: AstNode, i: number) => (
                        <div key={i} className="ast-child-group">
                            <AstNodeView node={child} />
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}