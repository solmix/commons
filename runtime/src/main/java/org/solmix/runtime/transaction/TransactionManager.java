package org.solmix.runtime.transaction;


public interface TransactionManager {

	void rollback() throws TransactionException;

    void commit() throws TransactionException;

    void bind(Object object, TransactionService transaction);

//    Map<Object, Transaction> getTransactions();

    TransactionService getTransaction(Object object);
}