package org.solmix.runtime.transaction.support;

public class TransactionListenerAdaptor implements TransactionListener {

	@Override
	public void suspend() {}

	@Override
	public void resume() {}

	@Override
	public void flush() {}

	@Override
	public void beforeCommit(boolean readOnly) {}

	@Override
	public void beforeCompletion() {}

	@Override
	public void afterCommit() {}

	@Override
	public void afterCompletion(int status) {}

}
