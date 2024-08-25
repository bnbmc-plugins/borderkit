type HandlerReturn = boolean | number
export type HandlerFunction = (passport: Passport, player: Player, next?: () => HandlerReturn) => HandlerReturn

export interface Passport {
    givenName: string
    familyName: string
    issuingAuthority: string
    expiryDate: Date
    dateOfBirth: Date
    placeOfBirth: string
    isExpired: boolean
}

export interface Player {
    send: (message: string) => void
    sendError: (message: string) => void
}