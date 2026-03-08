import { useState } from "react";
import AstNodeLabel from "./AstNodeLabel";
import AstNodeView from "./AstNodeView";
import type { AstNode } from "./nodeUtils";
import {
    childEntries,
    isBranchExpandable,
    isBranchKey,
    isInlineKey,
    isTopLevelNodeType,
    isSourceNodeType,
    nodeClass,
} from "./nodeUtils";

export default function AstBranchNode({ node }: { node: AstNode }) {
    const [collapsed, setCollapsed] = useState(false);

    const children = childEntries(node);
    const branchChildren = children.filter(([key]) => isBranchKey(key));
    const inlineChildren = children.filter(([key]) => isInlineKey(key));
    const hasChildren = branchChildren.length > 0;

    const isTopLevelNode = isTopLevelNodeType(node.node);
    const isSourceNode = isSourceNodeType(node.node);
    const branchExpandable = isBranchExpandable(node.node);

    function toggleCollapsed(e: React.MouseEvent) {
        e.stopPropagation();

        if (hasChildren) {
            setCollapsed((prev) => !prev);
        }
    }

    return (
        <div
            className={[
                "ast-branch",
                isTopLevelNode ? "ast-branch-top-level" : "",
                isSourceNode ? "ast-branch-source" : "",
                branchExpandable && hasChildren ? "clickable" : "",
                branchExpandable && hasChildren ? (collapsed ? "collapsed" : "expanded") : "",
            ].join(" ").trim()}
            onClick={branchExpandable && hasChildren ? toggleCollapsed : undefined}
        >
            <div
                className={[
                    nodeClass(node.node),
                    !branchExpandable && hasChildren ? "clickable" : "",
                    !branchExpandable && hasChildren ? (collapsed ? "collapsed" : "expanded") : "",
                    isSourceNode ? "ast-node-source" : "",
                ].join(" ").trim()}
                onClick={
                    !branchExpandable && hasChildren
                        ? toggleCollapsed
                        : undefined
                }
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