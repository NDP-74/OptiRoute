package com.optiroute.backend.service.cost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optiroute.backend.config.HttpClientConfig;
import com.optiroute.backend.dto.response.cost.FuelResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;

/**
 * Prix moyen du gazole (data.economie.gouv.fr), mis en cache une heure.
 * <p>
 * Un seul thread rafraîchit le cache à la fois ; les autres reçoivent immédiatement la dernière valeur connue
 * (ou la valeur par défaut). Après un échec, aucun nouvel appel n'est tenté avant {@link #RETRY_DELAY}.
 */
@Slf4j
@Service
public class FuelPriceService {

    static final double DEFAULT_DIESEL_PRICE = 1.8;
    private static final Duration CACHE_DURATION = Duration.ofHours(1);
    private static final Duration RETRY_DELAY = Duration.ofMinutes(10);
    private static final String URL =
        "https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records?limit=100";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile Double cachedDieselPrice;
    private volatile Instant nextRefresh = Instant.MIN;

    public FuelPriceService(@Value("${app.fuel-price.connect-timeout:3s}") Duration connectTimeout,
            @Value("${app.fuel-price.read-timeout:5s}") Duration readTimeout) {
        this.restTemplate = new RestTemplate(HttpClientConfig.requestFactory(connectTimeout, readTimeout));
    }

    public double getAverageDieselPrice() {
        if (Instant.now().isBefore(nextRefresh) || !refreshLock.tryLock()) {
            return currentPrice();
        }
        try {
            if (Instant.now().isBefore(nextRefresh)) {
                return currentPrice();
            }
            OptionalDouble average = fetchAveragePrice();
            if (average.isPresent()) {
                cachedDieselPrice = average.getAsDouble();
                nextRefresh = Instant.now().plus(CACHE_DURATION);
            } else {
                nextRefresh = Instant.now().plus(RETRY_DELAY);
            }
            return currentPrice();
        } finally {
            refreshLock.unlock();
        }
    }

    private double currentPrice() {
        Double price = cachedDieselPrice;
        return price != null ? price : DEFAULT_DIESEL_PRICE;
    }

    private OptionalDouble fetchAveragePrice() {
        try {
            @SuppressWarnings("null")
            ResponseEntity<byte[]> response = restTemplate.exchange(URL, HttpMethod.GET, null, byte[].class);
            byte[] bodyBytes = response.getBody();
            if (bodyBytes == null) {
                log.warn("Prix du gazole : réponse vide, dernière valeur connue conservée.");
                return OptionalDouble.empty();
            }
            FuelResponse body = objectMapper.readValue(decode(bodyBytes), FuelResponse.class);
            OptionalDouble average = body.getResults().stream().map(FuelResponse.Result::getGazolePrix).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
            if (average.isEmpty()) {
                log.warn("Prix du gazole : aucun prix dans la réponse, dernière valeur connue conservée.");
            }
            return average;
        } catch (Exception exception) {
            log.warn("Prix du gazole indisponible ({}), dernière valeur connue conservée ; nouvel essai dans {} min.",
                exception.getClass().getSimpleName(), RETRY_DELAY.toMinutes());
            return OptionalDouble.empty();
        }
    }

    private static String decode(byte[] bodyBytes) throws IOException {
        if (bodyBytes.length > 2 && bodyBytes[0] == (byte) 0x1f && bodyBytes[1] == (byte) 0x8b) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bodyBytes)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                gis.transferTo(out);
                return out.toString(StandardCharsets.UTF_8);
            }
        }
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }
}
