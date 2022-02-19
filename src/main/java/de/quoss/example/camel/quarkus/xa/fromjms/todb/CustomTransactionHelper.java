package de.quoss.example.camel.quarkus.xa.fromjms.todb;

import org.jboss.logging.Logger;
import org.jboss.narayana.jta.jms.TransactionHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.jms.JMSException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

@ApplicationScoped
public class CustomTransactionHelper implements TransactionHelper {

    private static final Logger LOGGER = Logger.getLogger(CustomTransactionHelper.class);

    private final TransactionManager transactionManager;

    public CustomTransactionHelper(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public boolean isTransactionAvailable() throws JMSException {
        final String methodName = "isTransactionAvailable()";
        LOGGER.tracef("%s start", methodName);
        boolean result = false;
        try {
            Transaction txn = transactionManager.getTransaction();
            int status = transactionManager.getStatus();
            LOGGER.debugf("%s [txn=%s,status=%s]", methodName, txn, status);
            result = status != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            throw getJmsException(e.getMessage(), e);
        }
        LOGGER.tracef("%s end [result=%s]", methodName, result);
        return result;
    }

    @Override
    public void registerSynchronization(final Synchronization synchronization) throws JMSException {
        try {
            getTransaction().registerSynchronization(synchronization);
        } catch (IllegalStateException | RollbackException | SystemException e) {
            throw getJmsException(e.getMessage(), e);
        }
    }

    @Override
    public void registerXAResource(XAResource xaResource) throws JMSException {
        try {
            if (!getTransaction().enlistResource(xaResource)) {
                throw getJmsException("Error enlisting resource.", null);
            }
        } catch (RollbackException | IllegalStateException | SystemException e) {
            throw getJmsException(e.getMessage(), e);
        }
    }

    @Override
    public void deregisterXAResource(XAResource xaResource) throws JMSException {
        try {
            if (!getTransaction().delistResource(xaResource, XAResource.TMSUCCESS)) {
                throw getJmsException("Error delisting resource.", null);
            }
        } catch (IllegalStateException | SystemException e) {
            throw getJmsException(e.getMessage(), e);
        }
    }

    private Transaction getTransaction() throws JMSException {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw getJmsException(e.getMessage(), e);
        }
    }

    private JMSException getJmsException(final String message, final Exception cause) {
        JMSException jmsException = new JMSException(message);
        jmsException.setLinkedException(cause);
        return jmsException;
    }

}
