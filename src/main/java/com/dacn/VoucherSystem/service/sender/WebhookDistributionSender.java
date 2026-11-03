package com.dacn.VoucherSystem.service.sender;

import com.dacn.VoucherSystem.exception.DistributionSendFailedException;
import com.dacn.VoucherSystem.exception.InvalidDistributionTargetException;
import com.dacn.VoucherSystem.service.interfaces.DistributionLogService;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Service("WEBHOOK")
public class WebhookDistributionSender implements DistributionSender {

    private final RestClient restClient;

    public WebhookDistributionSender() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String channel() {
        return DistributionLogService.WEBHOOK;
    }

    @Override
    public void validateTarget(String targetAddress) {
        URI uri;
        try {
            uri = URI.create(targetAddress);
        } catch (IllegalArgumentException ex) {
            throw new InvalidDistributionTargetException(
                    "targetAddress is not a valid webhook URL: " + targetAddress);
        }

        String scheme = uri.getScheme();
        boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!httpScheme || uri.getHost() == null) {
            throw new InvalidDistributionTargetException(
                    "targetAddress must be an absolute http(s) URL: " + targetAddress);
        }
    }

    @Override
    public void send(DistributionPayload payload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "voucher.distributed");
        body.put("voucherId", payload.voucherId());
        body.put("voucherCode", payload.voucherCode());
        body.put("batchId", payload.batchId());
        body.put("campaignId", payload.campaignId());
        body.put("campaignName", payload.campaignName());
        body.put("sentAt", Instant.now().toString());

        try {
            restClient.post()
                    .uri(URI.create(payload.targetAddress()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new DistributionSendFailedException(
                    "Failed to call webhook " + payload.targetAddress());
        }
    }
}