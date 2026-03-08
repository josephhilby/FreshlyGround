export type AstNode = {
    node: string;
    [key: string]: any;
};

export function isSourceNodeType(nodeType: string) {
    return nodeType === "Source"
}

export function isMethodNodeType(nodeType: string) {
    return nodeType === "Method";
}

export function isTopLevelNodeType(nodeType: string) {
    return (
        nodeType === "Method" ||
        nodeType === "Field"
    );
}

export function isBranchExpandable(nodeType: string) {
    return (
        isSourceNodeType(nodeType) ||
        isMethodNodeType(nodeType)
    );
}

export function isBranchKey(key: string) {
    return (
        key === "fields" ||
        key === "methods" ||
        key === "statements" ||
        key === "thenStatements" ||
        key === "elseStatements"
    );
}

export function isInlineKey(key: string) {
    return (
        key === "value" ||
        key === "parameters" ||
        key === "expression" ||
        key === "condition" ||
        key === "initialization" ||
        key === "increment" ||
        key === "arguments" ||
        key === "receiver" ||
        key === "left" ||
        key === "right"
    );
}

export function nodeClass(nodeType: string) {
    if (nodeType === "Source") {
        return "ast-node source"
    }

    if (nodeType === "Method" || nodeType === "Field") {
        return "ast-node top-level";
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

export function isBranchRenderable(node: AstNode) {
    return (
        node.node === "Source" ||
        node.node === "Method" ||
        node.node === "Field" ||
        node.node === "Declaration" ||
        node.node === "Expression" ||
        node.node === "If" ||
        node.node === "For" ||
        node.node === "While" ||
        node.node === "Return"
    );
}

export function childEntries(node: AstNode) {
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