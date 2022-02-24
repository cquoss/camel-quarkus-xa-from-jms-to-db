package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.util.ObjectHelper;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

@ApplicationScoped
public class MainRoute extends RouteBuilder {

    private final UserTransaction transaction;

    private final TransactionManager transactionManager;

    public MainRoute(final UserTransaction transaction, final TransactionManager transactionManager) {
        this.transaction = ObjectHelper.notNull(transaction, "User transaction");
        this.transactionManager = ObjectHelper.notNull(transactionManager, "Transaction manager");
    }

    @Override
    public void configure() throws Exception {

        // add transaction manager to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setTransactionManager(new JtaTransactionManager(transaction, transactionManager));

        // add connection factory to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setConnectionFactory(new ActiveMQXAConnectionFactory());

        from("jms:topic:foo?receiveTimeout=30000&clientId=client-0&durableSubscriptionName=subscription-0")
            .routeId("main-route-0")
            // TODO Figure out how to set the id of the from node. When putting the node id right after
            //   the from node i cannot set a route id any more (it _is_ the route id then). And when i put it
            //   behind the route id like this it overrides the route id.
            // .id("from-jms")
            .to("jdbc:default?resetAutoCommit=false")
            .id("to-jdbc-0");

    }

}
