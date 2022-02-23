package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.jta.TransactionManager;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.jboss.logging.Logger;
import org.springframework.transaction.jta.JtaTransactionManager;

public class MainRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(MainRoute.class);

    @Override
    public void configure() throws Exception {

        final String methodName = "configure()";
        LOGGER.tracef("%s start", methodName);

        // we use all artemis broker defaults here (url, user, password)
        ActiveMQXAConnectionFactory cf = new ActiveMQXAConnectionFactory();
        // ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory();
        // FIXME Setting the client id on endpoint level does not work with the xa connection factory.
        //    Until then it can only be set on the connection factory. When using more than one
        //    jms durable subscription consumer one will have to set up separate xa connection factories.
        //    This should be fixed.
        // cf.setClientID("client-0");

        // fetch transaction manager from narayana
        javax.transaction.TransactionManager tm = TransactionManager.transactionManager();
        // dump narayana settings to debug log
        if (LOGGER.isDebugEnabled()) {
            dumpNarayanaSettings();
        }

        // create spring platform transaction manager
        JtaTransactionManager jtm = new JtaTransactionManager(tm);

        // add transaction manager to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setTransactionManager(jtm);

        // add connection factory to jms component
        ((JmsComponent) getCamelContext().getComponent("jms")).setConnectionFactory(cf);

        from("jms:topic:foo?receiveTimeout=30000&clientId=client-0&durableSubscriptionName=subscription-0")
            .routeId("main-route-0")
            // TODO Figure out how to set the id of the from node. When putting the node id right after
            //   the from node i cannot set a route id any more (it _is_ the route id then). And when i put it
            //   behind the route id like this it overrides the route id.
            // .id("from-jms")
            .to("jdbc:default?resetAutoCommit=false")
            .id("to-jdbc-0");

        LOGGER.tracef("%s end", methodName);

    }

    private void dumpNarayanaSettings() {

        final String methodName = "dumpNarayanaSetting()";

        // core settings
        LOGGER.debugf("%s --- Narayana core settings ---", methodName);
        CoreEnvironmentBean coreBean = arjPropertyManager.getCoreEnvironmentBean();
        LOGGER.debugf("%s [allowMultipleLastResources=%s]", methodName, coreBean.isAllowMultipleLastResources());
        // coordinator settings
        LOGGER.debugf("%s --- Narayana coordinator settings ---", methodName);
        CoordinatorEnvironmentBean coordinatorBean = arjPropertyManager.getCoordinatorEnvironmentBean();
        LOGGER.debugf("%s [checkedActionFactory=%s]", methodName, coordinatorBean.getCheckedActionFactory());
        LOGGER.debugf("%s [checkedActionFactoryClassName=%s]", methodName, coordinatorBean.getCheckedActionFactoryClassName());
        LOGGER.debugf("%s [defaultTimeOut=%s]", methodName, coordinatorBean.getDefaultTimeout());
        LOGGER.debugf("%s [dynamic1PC=%s]", methodName, coordinatorBean.getDynamic1PC());
        LOGGER.debugf("%s [maxTwoPhaseCommitThreads=%s]", methodName, coordinatorBean.getMaxTwoPhaseCommitThreads());
        LOGGER.debugf("%s [txReaperCancelFailWaitPeriod=%s]", methodName, coordinatorBean.getTxReaperCancelFailWaitPeriod());
        LOGGER.debugf("%s [txReaperCancelWaitPeriod=%s]", methodName, coordinatorBean.getTxReaperCancelWaitPeriod());
        LOGGER.debugf("%s [TxReaperMode=%s]", methodName, coordinatorBean.getTxReaperMode());
        // object store settings
        LOGGER.debugf("%s --- Narayana object store settings ---", methodName);
        ObjectStoreEnvironmentBean objectStoreBean = arjPropertyManager.getObjectStoreEnvironmentBean();
        LOGGER.debugf("%s [localOSRoot=%s]", methodName, objectStoreBean.getLocalOSRoot());
        LOGGER.debugf("%s [objectStoreDir=%s]", methodName, objectStoreBean.getObjectStoreDir());

    }

}
