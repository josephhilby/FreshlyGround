import Modal from "./Modal";
import AstNodeView from "./AstNodeView.tsx";

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
            <div className="ast-legend">
                <div className="legend-item">
                    <span className="legend-box structural"></span>
                    Structural
                </div>

                <div className="legend-item">
                    <span className="legend-box statement"></span>
                    Statement
                </div>

                <div className="legend-item">
                    <span className="legend-box expression"></span>
                    Expression
                </div>
            </div>
        </Modal>
    );
}