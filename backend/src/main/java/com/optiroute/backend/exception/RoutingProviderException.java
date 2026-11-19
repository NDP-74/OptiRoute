package com.optiroute.backend.exception;

/**
 * Échec d'appel au fournisseur d'itinéraire (HERE, PTV) : indisponibilité, délai dépassé ou réponse en erreur.
 * Le message ne contient jamais l'URL appelée, qui porte la clé d'API.
 */
public class RoutingProviderException extends RuntimeException {

    public RoutingProviderException(String message) {
        super(message);
    }

    public RoutingProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
