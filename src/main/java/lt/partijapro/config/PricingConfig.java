package lt.partijapro.config;

import lt.partijapro.pricing.FixedIntervalMarkdownPolicy;
import lt.partijapro.pricing.PricingEngine;
import lt.partijapro.pricing.WeightedCostAllocation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// the only place where concrete strategies are picked.
// adding a new strategy in stage III = change only this, not PricingEngine
@Configuration
public class PricingConfig {

    @Bean
    public PricingEngine pricingEngine() {
        return new PricingEngine(new WeightedCostAllocation(), FixedIntervalMarkdownPolicy.defaults());
    }

    // separate bean so "today" can be faked in tests if needed
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
