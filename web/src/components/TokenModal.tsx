import Modal from "./Modal";

type Token = {
    type: "IDENTIFIER" | "INTEGER" | "DECIMAL" | "CHARACTER" | "STRING" | "OPERATOR";
    literal: string;
    index: number;
};

type TokenModalProps = {
    tokens: Token[];
    onClose: () => void;
};

function tokenClass(type: Token["type"]) {
    switch (type) {
        case "IDENTIFIER":
            return "token-chip identifier";
        case "INTEGER":
            return "token-chip integer";
        case "DECIMAL":
            return "token-chip decimal";
        case "CHARACTER":
            return "token-chip character";
        case "STRING":
            return "token-chip string";
        case "OPERATOR":
            return "token-chip operator";
    }
}

export default function TokenModal({ tokens, onClose }: TokenModalProps) {
    return (
        <Modal title="Tokens" onClose={onClose}>
            <div className="token-grid">
                {tokens.map((t, i) => (
                    <div key={i} className={tokenClass(t.type)}>
                        {t.literal}
                    </div>
                ))}
            </div>
        </Modal>
    );
}