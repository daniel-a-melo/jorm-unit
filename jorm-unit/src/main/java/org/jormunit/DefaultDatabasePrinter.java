package org.jormunit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.jormunit.database.DatabaseInstrospector;

import dnl.utils.text.table.TextTable;

/**
 * <p>Runs a query to the database and prints all data to console.</p> 
 * 
 * <p>The SQL to obtain all database table names is obtained via an implementation of {@link DatabaseInstrospector}</p>
 *
 */
public class DefaultDatabasePrinter {
	
	private Connection connection;
	private DatabaseInstrospector introspector;
	
	public DefaultDatabasePrinter(Connection connection, DatabaseInstrospector introspector) {
		setConnection(connection);
		setInstrospector(introspector);
	}
	
	private List<String> fetchAllTableNames() throws SQLException {
		
		ColumnListHandler<String> handler = new ColumnListHandler<String>();
		QueryRunner runner = new QueryRunner();
		return runner.query(getConnection(), getIntrospector().getAllTablesSqlQuery(), handler);
		
	}
	
	
	public void printAllTables() throws SQLException {
		List<String> tableNames = fetchAllTableNames();
		System.out.println(" ");
		for (String name : tableNames) {
			printTable(name);
		}
		System.out.println(" ");
	}
	
	public void printTable(String tableName) throws SQLException  {
		
		MapListHandler handler = new MapListHandler();
		QueryRunner runner = new QueryRunner();
		List<Map<String, Object>> rows = runner.query(getConnection(), "SELECT * FROM " + tableName, handler);
		
		if (rows.size() == 0) {
			printEmptyTable(tableName);
			return;
		}

		//Data is organized on a 2-dimensional array in order to print it in table format by TextTable
		String[] columnNames = rows.get(0).keySet().toArray(new String[rows.get(0).keySet().size()]);
		String[][] data = new String[rows.size()][columnNames.length];
		
		for (int i = 0; i < rows.size(); i++) {
			
			Map<String, Object> currentRow = rows.get(i);
			
			for (int j = 0; j < columnNames.length; j++) {
				data[i][j] = printValue(currentRow.get(columnNames[j]));
			}
			
		}
		
		//Printing table header
		System.out.println(tableName + " [" + rows.size() + (rows.size() == 1 ? " row" : " rows") + "]");		
		TextTable table = new TextTable(columnNames, data);
		table.printTable();
		System.out.println(" ");
		
	}
	
	protected String printValue(Object data) {
		if (data == null) {
			return "[NULL]";
		} else {
			return data.toString();
		}
	}
	
	
	private void printEmptyTable(String tableName) {
		System.out.println(tableName + " is empty");
		System.out.println(" ");
	}
	
	//region getters and setters
	
	public Connection getConnection() {
		return connection;
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	public DatabaseInstrospector getIntrospector() {
		return introspector;
	}

	public void setInstrospector(DatabaseInstrospector introspector) {
		this.introspector = introspector;
	}
	
	//endregion
 

}
