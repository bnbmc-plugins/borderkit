declare type HandlerReturn = boolean | number
declare type PassportNextFunction = () => HandlerReturn;
declare type HandlerFunction = (passport: Passport, player: Player, next?: PassportNextFunction) => HandlerReturn

declare type Passport = {
    givenName: string
    familyName: string
    issuingAuthority: string
    expiryDate: Date
    dateOfBirth: Date
    placeOfBirth: string
    isExpired: boolean
} | null;

// declare type Passport = null;

declare type Player = {
    send: (message: string) => void
    sendError: (message: string) => void
}
