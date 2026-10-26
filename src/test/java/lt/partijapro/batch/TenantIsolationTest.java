package lt.partijapro.batch;

import lt.partijapro.web.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static lt.partijapro.batch.BatchController.TENANT_HEADER;
import static lt.partijapro.demo.DemoDataLoader.LOBIS;
import static lt.partijapro.demo.DemoDataLoader.SPINTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// tenant isolation, uses demo tenants "lobis" and "spinta".
// service is also called directly, so it is not just the UI hiding stuff
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BatchService batchService;

    @Autowired
    private BatchRepository batchRepository;

    private Long lobisBatchId;

    @BeforeEach
    void findLobisBatch() {
        lobisBatchId = batchRepository.findAllByTenantIdOrderByIdDesc(LOBIS).stream()
                .filter(batch -> batch.getName().equals("Outlet dėžė #1"))
                .findFirst().orElseThrow().getId();
    }

    @Test
    void serviceDoesNotReturnOtherTenantsBatch() {
        assertThat(batchService.get(LOBIS, lobisBatchId).name()).isEqualTo("Outlet dėžė #1");

        assertThatThrownBy(() -> batchService.get(SPINTA, lobisBatchId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void apiDoesNotReturnOtherTenantsBatch() throws Exception {
        mockMvc.perform(get("/api/batches/{id}", lobisBatchId).header(TENANT_HEADER, LOBIS))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/batches/{id}", lobisBatchId).header(TENANT_HEADER, SPINTA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Partija " + lobisBatchId + " nerasta"));

        mockMvc.perform(get("/api/batches/{id}/repricing", lobisBatchId).header(TENANT_HEADER, SPINTA))
                .andExpect(status().isNotFound());
    }

    @Test
    void listContainsOnlyOwnBatches() throws Exception {
        mockMvc.perform(get("/api/batches").header(TENANT_HEADER, LOBIS))
                .andExpect(jsonPath("$[*].name", hasItem("Outlet dėžė #1")));

        mockMvc.perform(get("/api/batches").header(TENANT_HEADER, SPINTA))
                .andExpect(jsonPath("$[*].name", hasItem("Siunta iš Vokietijos")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Outlet dėžė #1"))));
    }

    @Test
    void otherTenantCannotDeleteBatch() throws Exception {
        mockMvc.perform(delete("/api/batches/{id}", lobisBatchId).header(TENANT_HEADER, SPINTA))
                .andExpect(status().isNotFound());

        assertThat(batchRepository.findByIdAndTenantId(lobisBatchId, LOBIS)).isPresent();
    }

    @Test
    void missingOrUnknownTenantIsRejected() throws Exception {
        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/batches").header(TENANT_HEADER, "isilauzelis"))
                .andExpect(status().isForbidden());
    }
}
