package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@QuarkusIntegrationTest
@QuarkusTestResource(ArtemisTestResource.class)
class MainRouteIT {

    private static final Logger LOGGER = Logger.getLogger(MainRouteIT.class);

    @Test
    void testClientIdBug() throws Exception {
        LOGGER.info("Running client-id bug integration test.");
        TimeUnit.SECONDS.sleep(30L);
        LOGGER.info("Client-id bug integration test run.");
    }

}
