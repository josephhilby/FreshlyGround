import { useLayoutEffect, useRef } from "react";
import Modal from "./Modal";
import Legend from "./Legend";
import AstNodeView from "./AstNodeView.tsx";

type TreeModalProps = {
    ast: any;
    onClose: () => void;
};

export default function TreeModal({ ast, onClose }: TreeModalProps) {
    const treeRef = useRef<HTMLDivElement>(null);

    useLayoutEffect(() => {
        if (!treeRef.current) return;

        treeRef.current.style.removeProperty("--ast-node-height");
        // Force reflow so nodes return to natural height before measuring
        void treeRef.current.offsetHeight;

        const nodes = treeRef.current.querySelectorAll(
            ".ast-node:not(.ast-inline-node):not(.clickable)"
        );
        let maxH = 0;
        nodes.forEach((el) => {
            if (el.querySelector(".ast-inline-children")) return;
            const h = (el as HTMLElement).offsetHeight;
            if (h > maxH) maxH = h;
        });

        if (maxH > 0) {
            treeRef.current.style.setProperty("--ast-node-height", `${maxH}px`);
        }
    }, [ast]);

    return (
        <Modal title="AST" onClose={onClose}>
            <div ref={treeRef} className="ast-tree">
                <AstNodeView node={ast} />
            </div>
            <Legend
                items={[
                    { label: "Structural", className: "structural" },
                    { label: "Statement", className: "statement" },
                    { label: "Expression", className: "expression" },
                ]}
            />
        </Modal>
    );
}