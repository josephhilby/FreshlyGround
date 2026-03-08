import AstNodeLabel from "./AstNodeLabel";
import AstNodeView from "./AstNodeView";
import type { AstNode } from "./nodeUtils.ts";
import { nodeClass } from "./nodeUtils";

export default function AstAssignmentNode({ node }: { node: AstNode }) {
    const receiver = node.receiver;
    const value = node.value;

    return (
        <div className="ast-branch">
            <div
                className={nodeClass(node.node)}
                onClick={(e) => e.stopPropagation()}
            >
                <AstNodeLabel node={node} />

                <div className="ast-inline-children">
                    <div className="ast-inline-list">
                        {receiver && typeof receiver === "object" && "node" in receiver && (
                            <AstNodeView node={receiver} />
                        )}

                        <span className="ast-equals">=</span>

                        {value && typeof value === "object" && "node" in value && (
                            <AstNodeView node={value} />
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}