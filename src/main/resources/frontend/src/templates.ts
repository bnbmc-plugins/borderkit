export const defaultGlobalFunction = `function handler(passport, player, next) {
    if (passport?.isExpired) return false;
    return next();
}`

export const defaultHandlerTemplate = `function handler(passport, player) {
    // Handle checking of the passport
    return false;
}`

export const handlerPreamble = `/**
 * Handle attestation of a passport globally
 * @param {Passport} passport The passport to attest
 * @param {Player} player The player holding the passport
 * @param {PassportNextFunction} next Hand control over to the next function in the pipeline
 * @returns {HandlerReturn} A number of blocks that redstone output should stay on for if successful, false otherwise
 */
`;