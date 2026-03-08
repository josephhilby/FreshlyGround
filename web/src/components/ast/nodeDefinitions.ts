export const NODE_DEFINITIONS: Record<string, string> = {
    Source: `
source ::=
    { field } { method }`,

    Field: `
field ::=
    "LET" [ "CONST" ] name ":" type [ "=" value ] ";"`,

    Method: `
method ::=
    "DEF" name "(" [ param ":" paramType ] ")" [ ":" returnType ] "DO"
        { statements }
    "END"`,

    Declaration: `
declaration ::=
    "LET" name [ ":" type ] [ "=" value ] ";"`,

    Assignment: `
assignment ::=
    receiver "=" value ";"`,

    Expression: `
expression ::=
    expression ";"`,

    If: `
if ::=
    "IF" condition "DO"
        { thenStatements }
    [ "ELSE"
        { elseStatements } ]
    "END"`,

    For: `
for ::=
    "FOR" "(" [ initialization ] ";" condition ";" [ increment ] ")"
        { statements }
    "END"`,

    While: `
while ::=
    "WHILE" condition "DO"
        { statements }
    "END"`,

    Return: `
return ::=
    "RETURN" value ";"`,

    Binary: `
binary ::=
    expression operator expression`,

    Access: `
access ::=
    [ receiver "." ] name`,

    Function: `
function ::=
    [ receiver "." ] name "(" [ arguments ] ")"`,

    Group: `
group ::=
    "(" expression ")"`,

    Literal: `
literal ::=
    data`,
};