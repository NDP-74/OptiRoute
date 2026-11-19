package com.optiroute.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Suppression refusée (HTTP 409) car l'élément est encore utilisé par des événements.
 */
public class ResourceInUseException extends ResponseStatusException {

    public ResourceInUseException(String resource, long eventCount) {
        super(HttpStatus.CONFLICT, "Impossible de supprimer " + resource + " : utilisé par " + eventCount + " événement"
            + (eventCount > 1 ? "s" : "") + ". Supprimez d'abord ces événements depuis le détail du transport.");
    }

    public static void ensureUnused(String resource, long eventCount) {
        if (eventCount > 0) {
            throw new ResourceInUseException(resource, eventCount);
        }
    }
}
