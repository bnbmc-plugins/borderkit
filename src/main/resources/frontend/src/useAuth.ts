import * as jose from 'jose'

interface JwtPayload extends jose.JWTPayload {
    jurisdiction: string
}

export function useAuth() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('auth');
    if (!token) {
        return {
            auth: ""
        }
    }

    const jwt = jose.decodeJwt<JwtPayload>(token);

    return {
        auth: token,
        user: jwt.sub,
        jurisdiction: jwt.jurisdiction
    }
}