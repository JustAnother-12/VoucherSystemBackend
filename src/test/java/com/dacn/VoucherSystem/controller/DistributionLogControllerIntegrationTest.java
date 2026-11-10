package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.entity.Campaign;
import com.dacn.VoucherSystem.entity.DistributionLog;
import com.dacn.VoucherSystem.entity.Voucher;
import com.dacn.VoucherSystem.entity.VoucherBatch;
import com.dacn.VoucherSystem.enums.BatchType;
import com.dacn.VoucherSystem.enums.CampaignStatus;
import com.dacn.VoucherSystem.enums.DistributionStatus;
import com.dacn.VoucherSystem.fixture.RedemptionTestFixture;
import com.dacn.VoucherSystem.repository.DistributionLogRepository;
import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * - JavaMailSender được mock để không gửi mail thật.
 * - Kênh WEBHOOK gọi tới một HTTP server cục bộ dựng ngay trong test (không gọi ra ngoài).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.mail.no-reply=no-reply@test.local")
public class DistributionLogControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/distributions";
    private static final String CUSTOMER_EMAIL = "customer@example.com";

    @Autowired
    private RedemptionTestFixture testFixture;

    @Autowired
    private DistributionLogRepository distributionLogRepository;

    @Autowired
    private TransactionTemplate tx;

    @PersistenceContext
    private EntityManager em;

    @MockitoBean
    private JavaMailSender mailSender;

    // ---- webhook server cục bộ ----
    private HttpServer webhookServer;
    private final AtomicInteger webhookResponseStatus = new AtomicInteger(200);
    private final List<String> webhookBodies = new CopyOnWriteArrayList<>();

    private record Ctx(Long campaignId, Long batchId) {
    }

    @BeforeEach
    void setUp() throws Exception {
        distributionLogRepository.deleteAllInBatch();
        testFixture.cleanDatabase();

        webhookResponseStatus.set(200);
        webhookBodies.clear();
        webhookServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        webhookServer.createContext("/", exchange -> {
            webhookBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(webhookResponseStatus.get(), -1);
            exchange.close();
        });
        webhookServer.start();
    }

    @AfterEach
    void tearDown() {
        webhookServer.stop(0);
        distributionLogRepository.deleteAllInBatch();
        testFixture.cleanDatabase();
    }

    private RequestPostProcessor auth() {
        return user("it_admin").authorities(new SimpleGrantedAuthority("SYSTEM"));
    }

    private String webhookUrl(String path) {
        return "http://127.0.0.1:" + webhookServer.getAddress().getPort() + path;
    }

    private static String allocations(int emailQuota, int webhookQuota) {
        return """
                {"allocations":[{"channel":"EMAIL","quota":%d},{"channel":"WEBHOOK","quota":%d}]}
                """.formatted(emailQuota, webhookQuota);
    }

    private static String emailOnly(int emailQuota) {
        return """
                {"allocations":[{"channel":"EMAIL","quota":%d}]}
                """.formatted(emailQuota);
    }

    private static JsonNode json(String text) {
        return JsonMapper.builder().build().readTree(text);
    }

    private static String body(Long batchId, Long campaignId, String channel, String targetAddress) {
        return """
                {"batchId":%d,"campaignId":%d,"channel":"%s","targetAddress":"%s"}
                """.formatted(batchId, campaignId, channel, targetAddress);
    }

    /**
     * Tạo fixture, gán distributionChannelsJson cho batch và đảm bảo batch có ít nhất {@code voucherCount} voucher.
     */
    private Ctx prepare(
            BatchType type,
            String allocationsJson,
            int voucherCount
    ) throws Exception {
        return prepare(
                type,
                allocationsJson,
                voucherCount,
                null,
                null
        );
    }

    private Ctx prepare(
            BatchType type,
            String allocationsJson,
            int voucherCount,
            Integer generatedCount,
            Integer maxUsageCount
    ) throws Exception {

        RedemptionTestFixture.Fixture fixture =
                generatedCount == null
                        ? testFixture.createFixture(type, CampaignStatus.ACTIVE)
                        : testFixture.createFixture(
                        type,
                        CampaignStatus.ACTIVE,
                        generatedCount,
                        maxUsageCount
                );

        Long campaignId = fixture.campaignId();

        Long batchId = em.createQuery(
                        "select b.id from VoucherBatch b where b.campaign.id = :cid",
                        Long.class
                )
                .setParameter("cid", campaignId)
                .getSingleResult();

        tx.executeWithoutResult(status -> {
            VoucherBatch batch = em.find(VoucherBatch.class, batchId);
            batch.setDistributionChannelsJson(json(allocationsJson));

            long existing = countVouchers(batchId);
            Campaign campaignRef = em.getReference(Campaign.class, campaignId);

            for (long i = existing; i < voucherCount; i++) {
                em.persist(Voucher.builder()
                        .createdAt(LocalDateTime.now())
                        .batch(batch)
                        .campaign(campaignRef)
                        .code("DIST-" + UUID.randomUUID()
                                .toString().substring(0, 8).toUpperCase())
                        .build());
            }
        });

        return new Ctx(campaignId, batchId);
    }

    private long countVouchers(Long batchId) {
        return em.createQuery("select count(v) from Voucher v where v.batch.id = :bid", Long.class)
                .setParameter("bid", batchId)
                .getSingleResult();
    }

    private String voucherCode(Long voucherId) {
        return em.find(Voucher.class, voucherId).getCode();
    }

    private ResultActions postDistribution(Long batchId, Long campaignId, String channel, String target)
            throws Exception {
        return mockMvc.perform(post(BASE_URL)
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(batchId, campaignId, channel, target)));
    }

    /** Gọi POST và kỳ vọng 201, trả về phần "data" của response. */
    private JsonNode createAndReadData(Ctx ctx, String channel, String target) throws Exception {
        String content = postDistribution(ctx.batchId(), ctx.campaignId(), channel, target)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content).get("data");
    }

    @Test
    @DisplayName("BULK + EMAIL: gửi thành công -> 201, SENT, log được lưu, mail gửi từ no-reply")
    void createDistributionBulkEmailSuccess() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);

        String content = postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", CUSTOMER_EMAIL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Create distribution successfully!"))
                .andExpect(jsonPath("$.data.campaignId").value(ctx.campaignId()))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.targetAddress").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(content).get("data");
        Long logId = data.get("id").asLong();
        Long voucherId = data.get("voucherId").asLong();

        DistributionLog saved = distributionLogRepository.findById(logId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DistributionStatus.SENT);
        assertThat(saved.getChannel()).isEqualTo("EMAIL");
        assertThat(saved.getTargetAddress()).isEqualTo(CUSTOMER_EMAIL);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mail = captor.getValue();
        assertThat(mail.getFrom()).isEqualTo("no-reply@test.local");
        assertThat(mail.getTo()).containsExactly(CUSTOMER_EMAIL);
        assertThat(mail.getText()).contains(voucherCode(voucherId));
    }

    @Test
    @DisplayName("channel viết thường vẫn được chấp nhận và lưu dạng chữ hoa")
    void createDistributionChannelIsCaseInsensitive() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId(), "email", CUSTOMER_EMAIL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.channel").value("EMAIL"));
    }

    @Test
    @DisplayName("Mail server lỗi -> vẫn 201 nhưng status FAILED và log FAILED được lưu")
    void createDistributionMailServerDownReturnsFailedLog() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        JsonNode data = createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);

        assertThat(data.get("status").asText()).isEqualTo("FAILED");
        DistributionLog saved = distributionLogRepository.findById(data.get("id").asLong()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DistributionStatus.FAILED);
    }

    @Test
    @DisplayName("BULK: log FAILED không chiếm quota/voucher -> gửi lại được")
    void createDistributionBulkRetryAfterFailureSucceeds() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(1, 1), 1);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);

        org.mockito.Mockito.reset(mailSender);

        postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", CUSTOMER_EMAIL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    @DisplayName("GENERIC + WEBHOOK: không áp dụng quota, webhook nhận đúng payload -> 201, SENT")
    void createDistributionGenericWebhookIgnoresQuota() throws Exception {
        Ctx ctx = prepare(BatchType.GENERIC, allocations(1, 1), 1);

        JsonNode first = createAndReadData(ctx, "WEBHOOK", webhookUrl("/partner-a"));
        JsonNode second = createAndReadData(ctx, "WEBHOOK", webhookUrl("/partner-b"));

        assertThat(first.get("status").asText()).isEqualTo("SENT");
        assertThat(second.get("status").asText()).isEqualTo("SENT");
        assertThat(first.get("channel").asText()).isEqualTo("WEBHOOK");

        assertThat(webhookBodies).hasSize(2);
        assertThat(webhookBodies.get(0))
                .contains("voucher.distributed")
                .contains(voucherCode(first.get("voucherId").asLong()));
    }

    @Test
    @DisplayName("GENERIC: tổng lượt phân phối vượt maxUsageCount của batch -> 409")
    void createDistributionExceedsMaxUsageReturnsConflict() throws Exception {
        Ctx ctx = prepare(
                BatchType.GENERIC,
                allocations(5, 5),
                1,
                1,
                1
        );

        createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);

        postDistribution(
                ctx.batchId(),
                ctx.campaignId(),
                "WEBHOOK",
                webhookUrl("/partner")
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(distributionLogRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Webhook trả 500 -> 201, status FAILED")
    void createDistributionWebhookFailsReturnsFailedLog() throws Exception {
        Ctx ctx = prepare(BatchType.GENERIC, allocations(5, 5), 1);
        webhookResponseStatus.set(500);

        postDistribution(ctx.batchId(), ctx.campaignId(), "WEBHOOK", webhookUrl("/down"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    @DisplayName("GENERIC: gửi lại mã cho cùng địa chỉ trên cùng kênh -> 409")
    void createDistributionGenericDuplicateTargetReturnsConflict() throws Exception {
        Ctx ctx = prepare(BatchType.GENERIC, allocations(5, 5), 1);
        createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);

        postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", CUSTOMER_EMAIL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(distributionLogRepository.count()).isEqualTo(1);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("BULK: kênh đã dùng hết quota -> 409")
    void createDistributionBulkQuotaExceededReturnsConflict() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(1, 1), 2);
        createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);

        postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", "other@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(distributionLogRepository.count()).isEqualTo(1);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("BULK: hết voucher trong batch -> 409")
    void createDistributionBulkNoVoucherLeftReturnsConflict() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(1000, 1000), 1);
        long total = countVouchers(ctx.batchId());

        for (long i = 0; i < total; i++) {
            createAndReadData(ctx, "EMAIL", "user" + i + "@example.com");
        }

        postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", "last@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(distributionLogRepository.count()).isEqualTo(total);
    }

    @Test
    @DisplayName("Kênh không nằm trong allocations của batch -> 400")
    void createDistributionChannelNotEnabledForBatchReturnsBadRequest() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, emailOnly(5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId(), "WEBHOOK", webhookUrl("/hook"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(distributionLogRepository.count()).isZero();
        assertThat(webhookBodies).isEmpty();
    }

    @Test
    @DisplayName("Batch không thuộc campaign -> 404")
    void createDistributionBatchNotInCampaignReturnsNotFound() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId() + 9999, "EMAIL", CUSTOMER_EMAIL)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertThat(distributionLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("Email sai định dạng -> 400")
    void createDistributionInvalidEmailReturnsBadRequest() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", "not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(distributionLogRepository.count()).isZero();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Webhook URL không phải http(s) -> 400")
    void createDistributionInvalidWebhookUrlReturnsBadRequest() throws Exception {
        Ctx ctx = prepare(BatchType.GENERIC, allocations(5, 5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId(), "WEBHOOK", "ftp://example.com/hook")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(distributionLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("channel không hợp lệ (SMS) -> 400")
    void createDistributionUnsupportedChannelReturnsBadRequest() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId(), "SMS", "0901234567")
                .andExpect(status().isBadRequest());

        assertThat(distributionLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("Thiếu targetAddress -> 400")
    void createDistributionBlankTargetReturnsBadRequest() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);

        postDistribution(ctx.batchId(), ctx.campaignId(), "EMAIL", "")
                .andExpect(status().isBadRequest());

        assertThat(distributionLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /distributions: lọc theo voucher_id, campaign_id, status -> 200")
    void getAllLogsWithFilters() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);
        JsonNode created = createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);
        Long voucherId = created.get("voucherId").asLong();

        mockMvc.perform(get(BASE_URL)
                        .with(auth())
                        .param("voucher_id", voucherId.toString())
                        .param("campaign_id", ctx.campaignId().toString())
                        .param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get distribution logs successfully!"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(created.get("id").asLong()))
                .andExpect(jsonPath("$.data[0].voucherId").value(voucherId))
                .andExpect(jsonPath("$.data[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));

        mockMvc.perform(get(BASE_URL)
                        .with(auth())
                        .param("campaign_id", ctx.campaignId().toString())
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /distributions không filter -> trả tất cả, mới nhất trước")
    void getAllLogsWithoutFiltersReturnsNewestFirst() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 2);
        createAndReadData(ctx, "EMAIL", "first@example.com");
        JsonNode second = createAndReadData(ctx, "EMAIL", "second@example.com");

        mockMvc.perform(get(BASE_URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(second.get("id").asLong()))
                .andExpect(jsonPath("$.data[0].targetAddress").value("second@example.com"));
    }

    // ---------- GET /distributions/logs/{id} ----------

    @Test
    @DisplayName("GET /distributions/logs/{id}: tồn tại -> 200")
    void getLogByIdSuccess() throws Exception {
        Ctx ctx = prepare(BatchType.BULK, allocations(5, 5), 1);
        JsonNode created = createAndReadData(ctx, "EMAIL", CUSTOMER_EMAIL);
        Long logId = created.get("id").asLong();

        mockMvc.perform(get(BASE_URL + "/logs/" + logId).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Get distribution log successfully!"))
                .andExpect(jsonPath("$.data.id").value(logId))
                .andExpect(jsonPath("$.data.targetAddress").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    @DisplayName("GET /distributions/logs/{id}: không tồn tại -> 404")
    void getLogByIdNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/logs/999999").with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}