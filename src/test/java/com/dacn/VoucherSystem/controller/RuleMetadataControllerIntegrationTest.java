package com.dacn.VoucherSystem.controller;

import com.dacn.VoucherSystem.BaseIntegrationTest;
import com.dacn.VoucherSystem.enums.ConditionType;
import com.dacn.VoucherSystem.enums.RewardType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RuleMetadataControllerIntegrationTest extends BaseIntegrationTest {

    private static final String URL = "/rule/metadata";

    @Test
    void metadataIsPublicAndReturnsSchemaVersion() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schemaVersion").value(1));
    }

    @Test
    void conditionTypesCoverEveryConditionEnumValue() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionTypes[*].type",
                        containsInAnyOrder(names(ConditionType.values()))));
    }

    @Test
    void rewardTypesCoverEveryRewardEnumValue() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardTypes[*].type",
                        containsInAnyOrder(names(RewardType.values()))));
    }

    @Test
    void everyEntryHasLabelDescriptionAndConfigSchema() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionTypes[*].label", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data.conditionTypes[*].description", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data.conditionTypes[*].configSchema", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data.rewardTypes[*].label", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data.rewardTypes[*].description", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data.rewardTypes[*].configSchema", everyItem(notNullValue())));
    }

    @Test
    void percentageRewardSchemaIsPresent() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardTypes[?(@.type=='PERCENTAGE')].configSchema").isNotEmpty());
    }

    // ---------- helpers ----------

    private static <E extends Enum<E>> String[] names(E[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }
}