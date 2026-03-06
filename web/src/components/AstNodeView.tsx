type AstNode = {
    node: string;
    [key: string]: any;
};

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

export default function AstNodeView({ node }: { node: AstNode }) {
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

    return (
        <div className="ast-branch">
            <div className={nodeClass(node.node)}>
                {renderLabel(node)}
            </div>

            {children.length > 0 && (
                <div className="ast-children">
                    {children.map(([key, value]) => (
                        <div key={key} className="ast-child-group">
                            <div className="ast-edge-label">{key}</div>

                            {Array.isArray(value) ? (
                                value.map((child, i) =>
                                    child && typeof child === "object" && "node" in child ? (
                                        <AstNodeView key={`${key}-${i}`} node={child} />
                                    ) : null
                                )
                            ) : value && "node" in value ? (
                                <AstNodeView node={value} />
                            ) : null}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}