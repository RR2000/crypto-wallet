import {Log, OidcClient, UserManager} from "oidc-client-ts"

Log.setLogger(console);
Log.setLevel(Log.DEBUG);

export const settings = {
    authority: "http://localhost:9000",
    client_id: "crypto-client",
    client_secret: "secret",
    redirect_uri: "http://127.0.0.1:5173/callback",
    response_type: "code",
    loadUserInfo: true,
    scope: "openid profile email wallets.private_keys",
    client_authentication: "client_secret_basic",
};

export {
    Log,
    OidcClient,
    UserManager
};