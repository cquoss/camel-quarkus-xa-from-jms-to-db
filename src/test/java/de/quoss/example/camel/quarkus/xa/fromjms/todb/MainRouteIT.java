package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Session;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

@QuarkusIntegrationTest
@QuarkusTestResource(ArtemisTestResource.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
class MainRouteIT {

    private static final Logger LOGGER = Logger.getLogger(MainRouteIT.class);

    private static final ActiveMQConnectionFactory CONNECTION_FACTORY = new ActiveMQConnectionFactory();

    private static final String JDBC_URL = ConfigProvider.getConfig().getConfigValue("quarkus.datasource.jdbc.url").getValue();

    private static final String BROKER_URL = ConfigProvider.getConfig().getConfigValue("quarkus.artemis.url").getValue();

    @Test
    void testClientIdBug() throws Exception {
        LOGGER.info("Running client-id bug integration test.");
        final String topicName = "foo";
        // log queues on topic
        queryQueuesOnAddress(topicName);
        // publish message on topic
        try (JMSContext context = CONNECTION_FACTORY.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = context.createProducer();
            Destination topic = context.createTopic(topicName);
            producer.send(topic, "insert into customer values ( 0, 'Barbara Quoss' )");
        }
        TimeUnit.SECONDS.sleep(30L);
        // assert record in table customer exists
        try (Connection c = DriverManager.getConnection(JDBC_URL);
             Statement s = c.createStatement()) {
            s.execute("select id from customer");
            Assertions.assertTrue(s.getResultSet().next());
        }

        LOGGER.info("Client-id bug integration test run.");
    }

    private void queryQueuesOnAddress(final String name) throws Exception {
        ServerLocator locator = ActiveMQClient.createServerLocator(BROKER_URL);
        ClientSession session = locator.createSessionFactory().createSession();
        ClientSession.AddressQuery query = session.addressQuery(new SimpleString(name));
        List<SimpleString> queueNames = query.getQueueNames();
        for (SimpleString queueName : queueNames) {
            ClientSession.QueueQuery queueQuery = session.queueQuery(queueName);
            LOGGER.infof("address: %s - queue.autoCreated: %s", name, queueQuery.isAutoCreated());
            LOGGER.infof("address: %s - queue.consumerCount: %s", name, queueQuery.getConsumerCount());
            LOGGER.infof("address: %s - queue.durable: %s", name, queueQuery.isDurable());
            LOGGER.infof("address: %s - queue.name: %s", name, queueQuery.getName());
            LOGGER.infof("address: %s - queue.purgeOnNoConsumers: %s", name, queueQuery.isPurgeOnNoConsumers());
            LOGGER.infof("address: %s - queue.routingType: %s", name, queueQuery.getRoutingType());
            LOGGER.info(new String(new char[40]).replace("\0", "-"));
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // truncate customer table
        try (Connection c = DriverManager.getConnection(JDBC_URL);
             Statement s = c.createStatement()) {
            s.execute("delete from customer");
        }
    }

}
