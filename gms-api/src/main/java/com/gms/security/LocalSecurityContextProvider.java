package com.gms.security;

/**
 * GAP-25: This class has been intentionally left as a no-op stub.
 *
 * The full local-auth user extraction logic previously duplicated here
 * has been consolidated into {@link SecurityContextProvider}, which handles
 * both 'local' and 'b2c' auth modes via the gms.security.auth-mode property.
 *
 * This file is retained only to avoid breaking any existing Spring bean references
 * in test configurations.  It is NOT registered as a @Component and has no runtime effect.
 */
public final class LocalSecurityContextProvider {
    private LocalSecurityContextProvider() {}
}
