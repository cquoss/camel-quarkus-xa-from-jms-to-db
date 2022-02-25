package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.util.ObjectHelper;
import org.jboss.logging.Logger;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

@ApplicationScoped
public class MainRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(MainRoute.class);

    private final UserTransaction transaction;

    private final TransactionManager transactionManager;

    public MainRoute(final UserTransaction transaction, final TransactionManager transactionManager) {
        this.transaction = ObjectHelper.notNull(transaction, "User transaction");
        this.transactionManager = ObjectHelper.notNull(transactionManager, "Transaction manager");
    }

    @Override
    public void configure() throws Exception {

        LOGGER.info("Configuring main route.");

        // add transaction manager to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setTransactionManager(new JtaTransactionManager(transaction, transactionManager));

        // add connection factory to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setConnectionFactory(new ActiveMQXAConnectionFactory());

        from("jms:topic:foo?clientId=client-0&durableSubscriptionName=subscription-0")
            .to("jdbc:default?resetAutoCommit=false");

        LOGGER.info("Main route configured.");

    }

}
