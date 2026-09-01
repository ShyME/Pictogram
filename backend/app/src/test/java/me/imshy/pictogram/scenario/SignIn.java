package me.imshy.pictogram.scenario;

/**
 * How a Pictogram session is obtained — the one part of {@link PictogramApi} that differs
 * per transport. An in-process run stands {@code mock-oauth2-server} in for Google and
 * follows the browser redirect chain; a black-box run against the deployed image performs
 * the real handshake. Either way the adapter drives the identity-provider dance and yields
 * the refresh-token cookie value; redeeming it for an access token is
 * {@link HttpPictogramApi}'s job, because that call is just "speaking the API".
 */
interface SignIn {

    /**
     * Drive the identity-provider handshake for {@code email} and return the value of the
     * refresh-token cookie the browser would now be holding.
     */
    String authenticate(String email);
}
