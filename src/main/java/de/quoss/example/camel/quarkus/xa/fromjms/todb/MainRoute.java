package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.jta.TransactionManager;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.jboss.logging.Logger;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.Transaction;

public class MainRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(MainRoute.class);

    @Override
    public void configure() throws Exception {

        final String methodName = "configure()";
        LOGGER.tracef("%s start", methodName);

        ActiveMQXAConnectionFactory cf = new ActiveMQXAConnectionFactory();

        javax.transaction.TransactionManager tm = TransactionManager.transactionManager();

        ConnectionFactoryProxy cfp = new ConnectionFactoryProxy(cf, new CustomTransactionHelper(tm));

        // create spring platform transaction manager
        JtaTransactionManager jtm = new JtaTransactionManager(tm);

        // add transaction manager to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setTransactionManager(jtm);

        // add connection factory to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setConnectionFactory(cfp);

        from("jms:to-queue")
                .process(exchange -> {
                    // show transaction and status
                    Transaction txn = tm.getTransaction();
                    int status = txn.getStatus();
                    LOGGER.debugf("[txn=%s,status=%s]", txn, status);
                    // show object store configuration
                    ObjectStoreEnvironmentBean bean = arjPropertyManager.getObjectStoreEnvironmentBean();
                    String objectStoreDir = bean.getObjectStoreDir();
                    String localOSRoot = bean.getLocalOSRoot();
                    LOGGER.debugf("[localOSRoot=%s,objectStoreDir=%s]", localOSRoot, objectStoreDir);
                })
                .to("jdbc:default?resetAutoCommit=false");

        LOGGER.tracef("%s end", methodName);

    }

}
