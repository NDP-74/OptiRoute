package com.optiroute.backend.client;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.optiroute.backend.config.HttpClientConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DashdocClient {

    private static final URI TRANSPORTS_URI = URI.create("https://api.dashdoc.eu/api/v4/transports/");
    private static final String API_HOST = "api.dashdoc.eu";
    private static final List<String> IMPORT_STATUSES = List.of("acknowledged");// "sent_to_trucker","assigned","on_loading_site","loading_complete","on_the_way","acknowledged");

    private final RestClient restClient;
    private final String token;
    private final Duration readTimeout;

    @Autowired
    public DashdocClient(RestClient.Builder restClientBuilder, @Value("${dashdoc.api.token:}") String token, @Value("${dashdoc.api.connect-timeout:5s}") Duration connectTimeout,
        @Value("${dashdoc.api.read-timeout:120s}") Duration readTimeout) {
        this(restClientBuilder.clone().requestFactory(HttpClientConfig.requestFactory(connectTimeout,readTimeout)).build(), token, readTimeout);
    }

    DashdocClient(RestClient restClient, String token, Duration readTimeout) {
        this.restClient = restClient;
        this.token = token;
        this.readTimeout = readTimeout;
    }

    public List<JsonNode> getTransports() {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La variable d’environnement DASHDOC_API_TOKEN n’est pas configurée.");
        }

        Map<String, JsonNode> transportsByUid = new LinkedHashMap<>();
        List<JsonNode> transportsWithoutUid = new ArrayList<>();
        log.info("Filtre Dashdoc appliqué sur les statuts : {}", String.join(", ", IMPORT_STATUSES));

        for (String status : IMPORT_STATUSES) {
            for (JsonNode transport : getTransportsByStatus(status)) {
                String uid = transport.path("uid").asText("");
                if (uid.isBlank()) {
                    transportsWithoutUid.add(transport);
                } else {
                    transportsByUid.putIfAbsent(uid, transport);
                }
            }
        }

        List<JsonNode> transports = new ArrayList<>(transportsByUid.values());
        transports.addAll(transportsWithoutUid);
        log.info("Récupération Dashdoc terminée : {} transport(s) distinct(s)", transports.size());
        return transports;
    }

    private List<JsonNode> getTransportsByStatus(String status) {
        List<JsonNode> transports = new ArrayList<>();
        Set<URI> visitedPages = new LinkedHashSet<>();
        URI currentPage = URI.create(TRANSPORTS_URI + "?status=" + status);
        int pageNumber = 0;

        while (currentPage != null) {
            if (!isDashdocTransportUri(currentPage) || !visitedPages.add(currentPage)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La pagination retournée par Dashdoc est invalide.");
            }

            pageNumber++;
            log.info("Récupération des transports Dashdoc au statut {} — page {}", status, pageNumber);
            JsonNode page;
            try {
                page = restClient.get().uri(currentPage).header(HttpHeaders.AUTHORIZATION, token).retrieve().body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                int upstreamStatus = exception.getStatusCode().value();
                log.warn("Dashdoc a répondu avec le statut HTTP {} pour le statut {} — page {}",
                    upstreamStatus, status, pageNumber);
                if (upstreamStatus == HttpStatus.UNAUTHORIZED.value() || upstreamStatus == HttpStatus.FORBIDDEN.value()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Dashdoc a refusé la clé API. Vérifiez DASHDOC_API_TOKEN et l’en-tête Authorization.", exception);
                }
                if (upstreamStatus == HttpStatus.GATEWAY_TIMEOUT.value()) {
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "La passerelle Dashdoc a dépassé son délai de traitement. Réessayez plus tard.", exception);
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Dashdoc a répondu avec une erreur HTTP " + upstreamStatus + ".", exception);
            } catch (ResourceAccessException exception) {
                log.warn("Délai de réponse Dashdoc dépassé après {} pour le statut {} — page {}",
                    readTimeout, status, pageNumber);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "Dashdoc n’a pas répondu dans le délai imparti. Réessayez ou augmentez DASHDOC_READ_TIMEOUT.", exception);
            } catch (RestClientException exception) {
                log.warn("Échec de l’appel à Dashdoc pour le statut {} — page {} : {}",
                    status, pageNumber, exception.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Impossible de récupérer les transports depuis Dashdoc.", exception);
            }

            if (page == null || !page.path("results").isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La réponse de Dashdoc ne contient pas la liste des transports attendue.");
            }
            page.path("results").forEach(transports::add);
            log.info("Dashdoc : {} transport(s) au statut {} récupéré(s) sur la page {}",
                page.path("results").size(), status, pageNumber);

            JsonNode next = page.get("next");
            currentPage = next == null || next.isNull() || next.asText().isBlank()
                ? null
                : TRANSPORTS_URI.resolve(next.asText());
        }

        return transports;
    }

    private static boolean isDashdocTransportUri(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) && API_HOST.equalsIgnoreCase(uri.getHost()) && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getPath() != null
            && uri.getPath().startsWith("/api/v4/transports/");
    }
}
