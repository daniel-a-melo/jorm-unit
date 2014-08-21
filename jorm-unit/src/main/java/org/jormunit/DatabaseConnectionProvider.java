package org.jormunit;

import java.sql.Connection;

import javax.persistence.EntityManager;

/**
 * <p>Provides the {@link java.sql.Connection} given an {@link javax.persistence.EntityManager}</p>
 * 
 * <p>The API to "unwrap" the current database connection may be provider specific. If JPA 2.1 is being used, the {@link JPAConnectionProvider}
 * will use the default {@link javax.persistence.EntityManager #unwrap(Class)} unwrap method.</p> 
 *
 */
public interface DatabaseConnectionProvider {
	
	/**
	 * Obtains the current {@link java.sql.Connection} being wrapped by the {@link javax.persistence.EntityManager} passed as parameter
	 * @param em {@link javax.persistence.EntityManager} to obtain the current {@link java.sql.Connection}
	 * @return Unwrapped {@link java.sql.Connection}
	 */
	Connection getCurrentConnection(EntityManager em);

}
