package org.jormunit.database;

/**
 * Provides a SQL command to retrieve all table names of a database
 * 
 * If a DMBS implements ANSI INFORMATION_SCHEMA view it is possible to use {@link AnsiDatabaseInstrospector} 
 * 
 */
public interface DatabaseInstrospector {
	
	String getAllTablesSqlQuery();

}
