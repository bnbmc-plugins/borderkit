export const defaultGlobalFunction = `function handler(passport, player, next) {
    if (passport?.isExpired) return false;
    return next();
}`

export const defaultHandlerTemplate = `function handler(passport, player) {
    // Handle checking of the passport
}`
