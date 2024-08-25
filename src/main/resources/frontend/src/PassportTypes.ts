declare type HandlerReturn = boolean | number
declare type PassportNextFunction = () => HandlerReturn;
declare type HandlerFunction = (passport: Passport, player: Player, next?: PassportNextFunction) => HandlerReturn

declare interface Passport {
    givenName: string
    familyName: string
    issuingAuthority: string
    expiryDate: Date
    dateOfBirth: Date
    placeOfBirth: string
    isExpired: boolean
}

declare interface Player {
    send: (message: string) => void
    sendError: (message: string) => void
}
