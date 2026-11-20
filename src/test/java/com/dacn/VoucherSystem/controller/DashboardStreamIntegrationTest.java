package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.cache.RateLimitPolicyCache;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.realtime.DashboardEventHub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardStreamIntegrationTest extends BaseIntegrationTest {

    private static final String STREAM_URL = "/statistic/stream";
    private static final long WAIT_MS = 5_000;

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RateLimitPolicyCache policyCache;

    @BeforeEach
    void setUp() {
        testFixture.cleanDatabase();
        policyCache.invalidate();
    }

    @AfterEach
    void tearDown() {
        policyCache.invalidate();
        testFixture.cleanDatabase();
    }

    // ---------- kết nối ----------

    @Test
    void streamRequiresAuthentication() throws Exception {
        mockMvc.perform(get(STREAM_URL).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void authenticatedClientReceivesConnectedEventImmediately() throws Exception {
        MvcResult result = openStream();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("event:connected"), "Thiếu sự kiện connected. Nội dung: " + body);
    }

    // ---------- Redis -> SSE ----------

    @Test
    void messagePublishedToRedisIsForwardedToOpenStream() throws Exception {
        MvcResult result = openStream();

        redis.convertAndSend(DashboardEventHub.CHANNEL, "{\"type\":\"MANUAL_PING\"}");

        String body = awaitBody(result, "MANUAL_PING");
        assertTrue(body.contains("event:dashboard"));
    }

    // ---------- sự kiện phát ra từ luồng áp mã thật ----------

    @Test
    void successfulRedemptionEmitsRedemptionSuccessEvent() throws Exception {
        RedemptionTestFixture.Fixture f = testFixture.createFixture(
                BatchType.GENERIC, CampaignStatus.ACTIVE, 1, 100);
        MvcResult result = openStream();

        redeem(f.code(), "DASH-" + UUID.randomUUID(), "100.00")
                .andExpect(status().isOk());

        String body = awaitBody(result, "REDEMPTION_SUCCESS");
        assertTrue(body.contains(f.code()), "Sự kiện thành công phải mang mã voucher. Nội dung: " + body);
    }

    @Test
    void failedRedemptionEmitsRedemptionFailedEvent() throws Exception {
        RedemptionTestFixture.Fixture f = testFixture.createFixture(
                BatchType.GENERIC, CampaignStatus.ACTIVE, 1, 100);
        MvcResult result = openStream();

        // Đơn 10 < ngưỡng 50 của condition -> 422, phát REDEMPTION_FAILED
        redeem(f.code(), "DASH-" + UUID.randomUUID(), "10.00")
                .andExpect(status().isUnprocessableEntity());

        String body = awaitBody(result, "REDEMPTION_FAILED");
        assertTrue(body.contains("REDEMPTION_FAILED"));
    }

    private MvcResult openStream() throws Exception {
        return mockMvc.perform(get(STREAM_URL)
                        .with(user("viewer").authorities(new SimpleGrantedAuthority("CAMPAIGN_MANAGER")))
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    // Sự kiện từ Redis đi qua listener container trên thread khác, nên phải chờ có giới hạn thời gian
    private String awaitBody(MvcResult result, String needle) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        String body = "";
        while (System.currentTimeMillis() < deadline) {
            body = result.getResponse().getContentAsString();
            if (body.contains(needle)) {
                return body;
            }
            Thread.sleep(50);
        }
        fail("Không nhận được '" + needle + "' trong stream sau " + WAIT_MS + "ms. Nội dung: " + body);
        return body;
    }

    private ResultActions redeem(String code, String customerId, String subtotal) throws Exception {
        BigDecimal amount = new BigDecimal(subtotal);
        Map<String, Object> body = Map.of(
                "voucherCode", code,
                "customerId", customerId,
                "cartSubtotal", amount,
                "customerSegments", Set.of(),
                "items", List.of(Map.of(
                        "productId", "PRODUCT-01",
                        "quantity", 1,
                        "unitPrice", amount
                ))
        );

        return mockMvc.perform(post("/redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Device-Fingerprint", "DEVICE-" + customerId)
                .content(objectMapper.writeValueAsString(body))
                .with(req -> {
                    req.setRemoteAddr("127.0.0.1");
                    return req;
                }));
    }
}