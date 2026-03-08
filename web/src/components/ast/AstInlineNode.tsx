import type { AstNode } from "./nodeUtils";
import { isInlineKey, nodeClass, childEntries } from "./nodeUtils";
import AstNodeLabel from "./AstNodeLabel";

export default function InlineAstNode({ node }: { node: AstNode }) {
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
                <AstNodeLabel node={node} />

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