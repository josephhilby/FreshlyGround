import "./ast.css";
import type { AstNode } from "./nodeUtils";
import { isBranchRenderable } from "./nodeUtils";
import AstBranchNode from "./AstBranchNode";
import AstInlineNode from "./AstInlineNode";
import AstBinaryNode from "./AstBinaryNode";
import AstIfNode from "./AstIfNode";
import AstForNode from "./AstForNode.tsx";
import AstAssignmentNode from "./AstAssignmentNode";

export default function AstNodeView({ node }: { node: AstNode }) {
    if (node.node === "Binary") {
        return <AstBinaryNode node={node} />;
    }

    if (node.node === "Assignment") {
        return <AstAssignmentNode node={node} />;
    }

    if (node.node === "If") {
        return <AstIfNode node={node} />;
    }

    if (node.node === "For") {
        return <AstForNode node={node} />;
    }

    if (isBranchRenderable(node)) {
        return <AstBranchNode node={node} />;
    }

    return <AstInlineNode node={node} />;
}
