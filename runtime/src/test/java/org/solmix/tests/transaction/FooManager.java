package org.solmix.tests.transaction;

import org.solmix.runtime.transaction.annotation.Transaction;

@Transaction
public class FooManager implements IFooManager {

	@Transaction
	@Override
	public void update(String id, String name) {
		// TODO Auto-generated method stub

	}

}
