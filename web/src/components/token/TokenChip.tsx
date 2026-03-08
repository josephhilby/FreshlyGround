import "./token.css";
import type { Token } from "./tokenUtils";
import { tokenClass, tokenTitle } from "./tokenUtils";

type TokenChipProps = {
    token: Token;
    lineNumber: number;
};

export default function TokenChip({ token, lineNumber }: TokenChipProps) {
    return (
        <div
            className={tokenClass(token)}
            title={tokenTitle(token, lineNumber)}
        >
            {token.literal}
        </div>
    );
}