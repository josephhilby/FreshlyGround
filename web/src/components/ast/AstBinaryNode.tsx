import AstNodeLabel from "./AstNodeLabel";
import AstNodeView from "./AstNodeView";
import type { AstNode } from "./nodeUtils.ts";
import { nodeClass } from "./nodeUtils";

export default function AstBinaryNode({ node }: { node: AstNode }) {
    const left = node.left;
    const right = node.right;

    return (
        <div
            className={`ast-inline-node ${nodeClass(node.node)}`}
            onClick={(e) => e.stopPropagation()}
        >
            <AstNodeLabel node={node} />

            {left && typeof left === "object" && "node" in left && (
                <AstNodeView node={left as AstNode} />
            )}

            {node.operator && (
                <div className="ast-chip">{String(node.operator)}</div>
            )}

            {right && typeof right === "object" && "node" in right && (
                <AstNodeView node={right as AstNode} />
            )}
        </div>
    );
}