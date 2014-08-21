package org.jormunit.database;

/**
 * Provides the ANSI SQL to obtain all table names of a database  
 * 
 */
public class AnsiDatabaseInstrospector implements DatabaseInstrospector {

	@Override
	public String getAllTablesSqlQuery() {
		return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'TABLE'";
	}

}
