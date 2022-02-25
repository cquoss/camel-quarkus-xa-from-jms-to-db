package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Session;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@QuarkusIntegrationTest
@QuarkusTestResource(ArtemisTestResource.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
class MainRouteIT {

    private static final Logger LOGGER = Logger.getLogger(MainRouteIT.class);

    private ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();

    private String jdbcUrl = ConfigProvider.getConfig().getConfigValue("quarkus.datasource.jdbc.url").getValue();

    @Test
    void testClientIdBug() throws Exception {
        LOGGER.info("Running client-id bug integration test.");
        // publish message on topic
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = context.createProducer();
            Destination topic = context.createTopic("foo");
            producer.send(topic, "insert into customer values ( 0, 'Barbara Quoss' )");
        }
        TimeUnit.SECONDS.sleep(30L);
        // assert record in table customer exists
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement s = c.createStatement()) {
            s.execute("select id from customer");
            Assertions.assertTrue(s.getResultSet().next());
        }

        LOGGER.info("Client-id bug integration test run.");
    }

    @AfterEach
    void tearDown() throws Exception {
        // truncate customer table
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement s = c.createStatement()) {
            s.execute("delete from customer");
        }
    }

}
