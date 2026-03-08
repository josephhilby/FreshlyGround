import Modal from "./Modal";
import Legend from "./Legend";
import AstNodeView from "./ast/AstNodeView.tsx";

type TreeModalProps = {
    ast: any;
    onClose: () => void;
};

export default function TreeModal({ ast, onClose }: TreeModalProps) {

    return (
        <Modal title="AST" onClose={onClose}>
            <div className="ast-tree">
                <AstNodeView node={ast} />
            </div>
            <Legend
                items={[
                    { label: "Top-Level", className: "top-level" },
                    { label: "Statement", className: "statement" },
                    { label: "Expression", className: "expression" },
                ]}
            />
        </Modal>
    );
}