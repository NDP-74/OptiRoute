package com.optiroute.backend.client;

import java.util.function.Supplier;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.optiroute.backend.exception.RoutingProviderException;

/**
 * Convertit les erreurs HTTP des fournisseurs d'itinéraire en {@link RoutingProviderException}.
 * La cause d'origine n'est pas conservée : son message contient l'URL appelée, donc la clé d'API HERE.
 */
final class ProviderCalls {

    private ProviderCalls() {
    }

    static String call(String provider, Supplier<String> request) {
        try {
            return request.get();
        } catch (RestClientResponseException exception) {
            throw new RoutingProviderException(provider + " a répondu " + exception.getStatusCode().value() + " : "
                + abbreviate(exception.getResponseBodyAsString()));
        } catch (ResourceAccessException exception) {
            throw new RoutingProviderException(provider + " injoignable ou délai dépassé ("
                + (exception.getCause() == null ? "erreur réseau" : exception.getCause().getClass().getSimpleName()) + ").");
        } catch (RestClientException exception) {
            throw new RoutingProviderException(provider + " : réponse inexploitable (" + exception.getClass().getSimpleName() + ").");
        }
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 300 ? body : body.substring(0, 300) + "…";
    }
}
