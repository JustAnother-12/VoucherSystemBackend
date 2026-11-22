package com.dacn.VoucherSystem.service.sender;

import com.dacn.VoucherSystem.exception.DistributionSendFailedException;
import com.dacn.VoucherSystem.exception.InvalidDistributionTargetException;
import com.dacn.VoucherSystem.service.interfaces.DistributionLogService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Send email from a no-reply address (cấu hình qua app.mail.no-reply).
 */
@Service("EMAIL")
public class EmailDistributionSender implements DistributionSender {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ObjectProvider<GmailApiClient> gmailApiClientProvider;
    private final String noReplyAddress;

    public EmailDistributionSender(
            ObjectProvider<GmailApiClient> gmailApiClientProvider,
            @Value("${app.mail.no-reply:no-reply@vouchersystem.local}") String noReplyAddress
    ) {
        this.gmailApiClientProvider = gmailApiClientProvider;
        this.noReplyAddress = noReplyAddress;
    }

    @Override
    public String channel() {
        return DistributionLogService.EMAIL;
    }

    @Override
    public void validateTarget(String targetAddress) {
        if (!EMAIL_PATTERN.matcher(targetAddress).matches()) {
            throw new InvalidDistributionTargetException(
                    "targetAddress is not a valid email address: " + targetAddress);
        }
    }

    @Override
    public void send(DistributionPayload payload) {
        GmailApiClient gmailApiClient = gmailApiClientProvider.getIfAvailable();
        if (gmailApiClient == null || !gmailApiClient.isEnabled()) {
            throw new DistributionSendFailedException("Gmail API is not configured");
        }

        try {
            gmailApiClient.send(
                    noReplyAddress,
                    payload.targetAddress(),
                    "Mã voucher của bạn - " + payload.campaignName(),
                    emailText(payload)
            );
        } catch (Exception ex) {
            throw new DistributionSendFailedException(
                    "Failed to send email to " + payload.targetAddress());
        }
    }

    private String emailText(DistributionPayload payload) {
        return """
                Hello,

                You have received a voucher code from the event. "%s".

                Code: %s

                This is an automated email; please do not reply to this email.
                """.formatted(payload.campaignName(), payload.voucherCode());
    }
}