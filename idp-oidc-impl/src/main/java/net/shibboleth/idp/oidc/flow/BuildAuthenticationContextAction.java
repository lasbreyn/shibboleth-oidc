package net.shibboleth.idp.oidc.flow;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.navigate.MessageLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Builds an authentication context message from an incoming request.
 */
public class BuildAuthenticationContextAction extends AbstractProfileAction {
    private final Logger log = LoggerFactory.getLogger(BuildAuthenticationContextAction.class);

    @Nonnull private Function<ProfileRequestContext,AuthnRequest> requestLookupStrategy;

    @Nullable
    private AuthnRequest authnRequest;

    /**
     * Instantiates a new authentication context action.
     */
    public BuildAuthenticationContextAction() {
        requestLookupStrategy =
                Functions.compose(new MessageLookup<>(AuthnRequest.class), new InboundMessageContextLookup());
    }

    /**
     * Sets request lookup strategy.
     *
     * @param strategy the strategy
     */
    public void setRequestLookupStrategy(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        requestLookupStrategy = Constraint.isNotNull(strategy, "AuthnRequest lookup strategy cannot be null");
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        authnRequest = requestLookupStrategy.apply(profileRequestContext);
        if (authnRequest == null) {
            log.debug("{} No inbound AuthnRequest, passive and forced flags will be off", getLogPrefix());
        }

        return super.doPreExecute(profileRequestContext);
    }

    @Nonnull
    @Override
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
                              @Nonnull final ProfileRequestContext profileRequestContext) {
        log.debug("{} Building authentication context", getLogPrefix());
        final AuthenticationContext ac = new AuthenticationContext();
        if (authnRequest != null) {
            ac.setForceAuthn(authnRequest.isForceAuthn());
            ac.setIsPassive(authnRequest.isPassive());
        }

        profileRequestContext.addSubcontext(ac, true);
        profileRequestContext.setBrowserProfile(true);
        return Events.Proceed.event(this);
    }
}
