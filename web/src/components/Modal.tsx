type ModalProps = {
    title: string;
    onClose: () => void;
    children: React.ReactNode;
};

export default function Modal({ title, onClose, children }: ModalProps) {
    return (
        <div className="modal-backdrop" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>{title}</h2>
                    <button onClick={onClose}>✕</button>
                </div>

                {children}
            </div>
        </div>
    );
}