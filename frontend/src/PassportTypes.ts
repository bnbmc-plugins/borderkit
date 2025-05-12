declare type HandlerReturn = HandlerReturnRaw | Promise<HandlerReturnRaw>
declare type HandlerReturnRaw = boolean | number
declare type PassportNextFunction = () => Promise<HandlerReturnRaw>;
declare type HandlerFunction = (passport: Passport, player: Player, next?: PassportNextFunction) => HandlerReturn

declare type Passport = {
    givenName: string
    familyName: string
    issuingAuthority: string
    expiryDate: Date
    issueDate?: Date
    dateOfBirth: Date
    placeOfBirth: string
    isExpired: boolean
    passportNumber: string
} | null;

// declare type Passport = null;

declare type Player = {
    send: (message: string) => void
    sendError: (message: string) => void
}
