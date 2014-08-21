package org.jormunit;

import java.sql.Connection;

import javax.persistence.EntityManager;

/**
 * Obtains the current {@link java.sql.Connection} using the {@link EntityManager #unwrap(Class)} method (as of JPA 2.1).
 *   
 */
public class JPAConnectionProvider implements DatabaseConnectionProvider {

	@Override
	public Connection getCurrentConnection(EntityManager em) {
		return em.unwrap(java.sql.Connection.class);
	}

}
