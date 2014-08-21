package org.jormunit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jormunit.database.AnsiDatabaseInstrospector;
import org.jormunit.database.DatabaseInstrospector;

/**
 * Setup the {@link javax.persistence.PersistenceUnit} name to be used by all tests in the test class and
 * configures a {@link DatabaseInstrospector} and {@link DatabaseConnectionProvider} if necessary 
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestConfiguration {
	/**
	 * {@link javax.persistence.PersistenceUnit} name 
	 */
	String persistenceUnitName();
	/**
	 * The {@link DatabaseInstrospector} to be used on all tests 
	 */
	Class<? extends DatabaseInstrospector> databaseInstrospector() default AnsiDatabaseInstrospector.class;
	/**
	 * The {@link DatabaseConnectionProvider} to be used on all tests
	 */
	Class<? extends DatabaseConnectionProvider> connectionProvider() default JPAConnectionProvider.class;
}
