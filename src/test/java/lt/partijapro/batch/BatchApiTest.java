package lt.partijapro.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static lt.partijapro.batch.BatchController.TENANT_HEADER;
import static lt.partijapro.demo.DemoDataLoader.LOBIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// full flow through the API: input -> calculation -> saved result, plus error cases
@SpringBootTest
@AutoConfigureMockMvc
class BatchApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BatchRepository batchRepository;

    @Test
    void createsAndCalculatesBatch() throws Exception {
        String body = """
                {"name": "Testinė dėžė", "purchasePrice": 180, "shippingCost": 20, "items": [
                  {"category": "striukė", "condition": "LABAI_GERA", "manualCost": 50},
                  {"category": "džinsai", "condition": "LABAI_GERA", "quantity": 3},
                  {"category": "marškinėliai", "condition": "LABAI_GERA", "quantity": 9}
                ]}""";

        mockMvc.perform(post("/api/batches").header(TENANT_HEADER, LOBIS)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCost").value(200.0))
                .andExpect(jsonPath("$.items", hasSize(13)))
                .andExpect(jsonPath("$.items[0].cost").value(50.0))
                .andExpect(jsonPath("$.items[1].cost").value(20.0))
                .andExpect(jsonPath("$.items[1].recommendedPrice").value(40.0))
                .andExpect(jsonPath("$.items[1].minPrice").value(24.0))
                .andExpect(jsonPath("$.items[12].cost").value(10.0));
    }

    @Test
    void rejectsManualCostsAboveTotalAndSavesNothing() throws Exception {
        long batchesBefore = batchRepository.count();
        String body = """
                {"name": "Klaidinga dėžė", "purchasePrice": 200, "shippingCost": 0, "items": [
                  {"category": "striukė", "condition": "LABAI_GERA", "manualCost": 120},
                  {"category": "striukė", "condition": "LABAI_GERA", "manualCost": 90}
                ]}""";

        mockMvc.perform(post("/api/batches").header(TENANT_HEADER, LOBIS)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error")
                        .value("Rankinės savikainos (210,00 €) viršija partijos savikainą (200,00 €)"));

        assertThat(batchRepository.count()).isEqualTo(batchesBefore);
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        String body = """
                {"name": "Dėžė", "purchasePrice": 10, "shippingCost": 0, "items": [
                  {"category": "batai", "condition": "GERA"}
                ]}""";

        mockMvc.perform(post("/api/batches").header(TENANT_HEADER, LOBIS)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Nežinoma kategorija: batai"));
    }

    @Test
    void repricingListShowsJeansAfterTwentyEightDays() throws Exception {
        Long batchId = demoBatchId();

        mockMvc.perform(get("/api/batches/{id}/repricing", batchId).header(TENANT_HEADER, LOBIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].category", everyItem(is("džinsai"))))
                .andExpect(jsonPath("$[*].newPrice", everyItem(is(32.0))));

        String in42Days = LocalDate.now().plusDays(42).toString();
        mockMvc.perform(get("/api/batches/{id}/repricing", batchId).header(TENANT_HEADER, LOBIS)
                        .param("date", in42Days))
                .andExpect(jsonPath("$[?(@.category == 'džinsai')].newPrice", everyItem(is(24.0))))
                .andExpect(jsonPath("$[?(@.category == 'džinsai')].needsReview", everyItem(is(true))));
    }

    private Long demoBatchId() {
        return batchRepository.findAllByTenantIdOrderByIdDesc(LOBIS).stream()
                .filter(batch -> batch.getName().equals("Outlet dėžė #1"))
                .findFirst().orElseThrow().getId();
    }
}
