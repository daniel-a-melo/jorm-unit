package org.jormunit;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.jormunit.database.AnsiDatabaseInstrospector;
import org.jormunit.database.DatabaseInstrospector;
import org.jormunit.dataset.AfterDataSet;
import org.jormunit.dataset.BeforeDataSet;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * <p>Runs tests against JPA API out of application servers.</p>
 * 
 * <p>A {@link EntityManagerFactory} will be created once before all tests, reused for each test and destroyed after all tests are executed.
 * The {@link PersistenceUnit} name to be used is informed via the annotation {@link TestConfiguration} at class level. If this annotation 
 * is not present then it will try to create a persistence unit named "default-pu".</p>
 * 
 * <p>if any test use the annotations {@link PrintDatabase}, {@link BeforeDataSet} or {@link AfterDataSet} then it might be necessary to provide
 * implementations of the interfaces {@link DatabaseInstrospector} and {@link DatabaseConnectionProvider}. These implementations are also informed 
 * via the class level annotation {@link TestConfiguration}.</p>
 * 
 * <p>If these implementation are not informed the defaults are {@link AnsiDatabaseInstrospector} and {@link JPAConnectionProvider} respectively.</p>
 * 
 * <p>The {@link JPAConnectionProvider} implementation may be omitted if the test class overrides {{@link #getCurrentDatabaseConnection()}</p>
 * 
 * <p>The method {{@link #getEntityManager()} provides an {@link EntityManager} to be used on all tests.</p> 
 *
 */
public abstract class JPATest {
	
	private static final String DEFAULT_PERSISTENCE_UNIT_NAME = "default-pu";
	private static final DatabaseInstrospector DEFAULT_DATABASE_INSTROSPECTOR = new AnsiDatabaseInstrospector();
	private static final DatabaseConnectionProvider DEFAULT_CONNECTION_PROVIDER = new JPAConnectionProvider(); 
	
	private static EntityManagerFactory entityManagerFactory;
	private IDatabaseConnection dbUnitconnection;
	private DefaultDatabasePrinter printer;
	TestConfiguration testConfiguration;
	
	protected static EntityManager entityManager;
	
	private class DBUnitJPARule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					initTest(); //Creates the entity manager factory
					processBeforeDataSet(base, description); //Loads a XML file with data into data base
					executeTest(base, description); //Execute test
					processPrintDatabase(base, description); //Print the current database state
					processAfterDataSet(base, description); //Loads a XML file with data and compares it with current database state
				}
			};
		}
		
		private void processBeforeDataSet(final Statement base, final Description description) throws Exception {
			
			BeforeDataSet beforeAnnotation = description.getAnnotation(BeforeDataSet.class);
			if (beforeAnnotation != null) {
				try {
					setUp();
					beginTransactionAndInitDbUnitConnection();
					IDataSet beforeDataSet = loadDataSet(beforeAnnotation.fileName());
					executeDatabaseCleanInsert(beforeDataSet);
					commitTransaction();
				} catch (Exception e) {
					rollbackTransaction();
					throw e;
				} finally {
					tearDown();
				}
			}
		}
		
		private void executeTest(final Statement base, final Description description) throws Throwable {
			
			setUp();
			Transactional transactionAnnotation = description.getAnnotation(Transactional.class);
			
			try  {
				
				if (transactionAnnotation != null) {
					beginTransaction();
				}
				
				base.evaluate();
				
				if (transactionAnnotation != null) {
					commitTransaction();
				}
				
			} catch (Exception e) {
				
				if (transactionAnnotation != null) {
					rollbackTransaction();
				}
				throw e;
			} finally {
				tearDown();
			}
			
		}
		
		private void processAfterDataSet(final Statement base, final Description description) throws Exception {
			
			AfterDataSet afterAnnotation = description.getAnnotation(AfterDataSet.class);
			if (afterAnnotation != null) {
				try {
					setUp();
					beginTransactionAndInitDbUnitConnection();
					compareDbToDataSet(afterAnnotation.fileName());
					commitTransaction();
				} catch(Exception e) {
					rollbackTransaction();
					throw e;
				} finally {
					tearDown();
				}
			}
		}
		
		private void processPrintDatabase(final Statement base, final Description description) throws Exception {
			
			PrintDatabase printAnnotation = description.getAnnotation(PrintDatabase.class);
			if (printAnnotation != null) {
				try {
					setUp();
					beginTransactionAndInitDbUnitConnection();
					printer = buildDatabasePrinter();
					printer.printAllTables();
					commitTransaction();
				} catch (Exception e) {
					rollbackTransaction();
					throw e;
				} finally {
					tearDown();
				}
			}
			
		}
		
		private IDataSet loadDataSet(String filename) throws DataSetException {
			FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
			flatXmlDataSetBuilder.setColumnSensing(true);
			InputStream fileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
			ReplacementDataSet dataSet = new ReplacementDataSet(flatXmlDataSetBuilder.build(fileStream));
			dataSet.addReplacementObject("[NULL]", null);
			return dataSet;
			
		}
		
		private void executeDatabaseCleanInsert(IDataSet dataset) throws Exception {
			DatabaseOperation.CLEAN_INSERT.execute(getDbUnitconnection(), dataset);
		}
		
		private void compareDbToDataSet(String filename) throws Exception
		{
			
			IDataSet afterActualDataSet = getDbUnitconnection().createDataSet(); 
			IDataSet afterExpectedDataSet = loadDataSet(filename);
			
			
			ITableIterator iter = afterActualDataSet.iterator();
			while (iter.next()) {
				ITable actualTable = iter.getTable();
				ITable expectedTable = afterExpectedDataSet.getTable(actualTable.getTableMetaData().getTableName());
				Column[] expectedColumns = expectedTable.getTableMetaData().getColumns();
				//Expected table will be sorted by the columns present on expected file
				ITable sortedExpectedTable = new SortedTable(expectedTable, expectedColumns); 

				//Actual table will be sorted by the columns present on *expected* file.
				//It will be also filtered to only include tables present on expected file
				ITable filteredActualTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedColumns);
				ITable sortedFilteredActualTable = new SortedTable(filteredActualTable, expectedColumns);
				
			    Assertion.assertEquals(sortedExpectedTable, sortedFilteredActualTable); 
			}
		}
		
	}
	
	
	@Rule
	public DBUnitJPARule dbUnitJPARule = new DBUnitJPARule();
	
	/*
	 * This method cannot be annotated as @BeforeClass, as JUnit demands such methods to be static. 
	 * Therefore it would not be possible to introspect for configuration annotation
	 */
	private void initTest() throws DatabaseUnitException {
		
		setTestConfiguration(this.getClass().getAnnotation(TestConfiguration.class));
			
		if (entityManager == null) {
			
			String persistenceUnitName = null;
			
			if (getTestConfiguration() == null) {
				persistenceUnitName = DEFAULT_PERSISTENCE_UNIT_NAME;
			} else {
				persistenceUnitName = getTestConfiguration().persistenceUnitName();
			}
			
			entityManagerFactory = createEntityManagerFactory(persistenceUnitName);
		}
	}
	
	protected EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
		return Persistence.createEntityManagerFactory(persistenceUnitName);
	}
	
	@AfterClass
	public static void closeEntityManagerFactory() {
		entityManagerFactory.close();
		
	}

	public void setUp() throws Exception {
		entityManager = entityManagerFactory.createEntityManager();
	}
	
	protected void printDatabase() throws Exception {
		initDbUnitConnection();
		printer = buildDatabasePrinter();
		printer.printAllTables();
	}
	
	private void beginTransactionAndInitDbUnitConnection() {
		
		beginTransaction();
		initDbUnitConnection();

	}
	
	private void initDbUnitConnection() {
		
		try {
			dbUnitconnection = new DatabaseConnection(getCurrentDatabaseConnection());
		} catch (DatabaseUnitException e) {
			throw new JormUnitException("Failed to retrieve the database connection", e);
		}
		
		DefaultDataTypeFactory dbUnitDataTypeFactory = buildDbUnitTypeFactory();
		if (dbUnitDataTypeFactory != null) {
			dbUnitconnection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, dbUnitDataTypeFactory);
		}
		
	}
	
	/*
	 * This method can only be invoked within a transaction for EclipseLink. 
	 * To maintain consistency across JPA providers, JPATest always invoke this method within the context of a transaction 
	 */
	protected Connection getCurrentDatabaseConnection() {
		DatabaseConnectionProvider connectionProvider = buildDatabaseConnectionProvider();
		return connectionProvider.getCurrentConnection(getEntityManager());
	}
	
	//TODO: Support various databases
	protected DefaultDataTypeFactory buildDbUnitTypeFactory() {
		return new HsqldbDataTypeFactory();
	}
	
	private Object buildInstanceFor(Class<?> type, Object defaultInstance) {
		
		Object instance = null;
		
		try {
			instance = type.newInstance();
		} catch (InstantiationException ie) { 
			ie.printStackTrace();
		} catch (IllegalAccessException iae) {
			iae.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		if (instance == null) {
			System.out.println(String.format("Unable to create an instance of %s. Please ensure it has a constructor method with no arguments. Using an instance of %s instead.", type.getName(), defaultInstance.getClass().getName()));
			instance = defaultInstance;
		}

		return instance;
		
	}
	
	protected DefaultDatabasePrinter buildDatabasePrinter() throws SQLException {
		
		DatabaseInstrospector dbInstrospector = null; 
		
		if (getTestConfiguration() != null) {
			dbInstrospector = (DatabaseInstrospector) buildInstanceFor(getTestConfiguration().databaseInstrospector(), DEFAULT_DATABASE_INSTROSPECTOR);
		} else {
			dbInstrospector = DEFAULT_DATABASE_INSTROSPECTOR;
		}
		
		return new DefaultDatabasePrinter(getDbUnitconnection().getConnection(), dbInstrospector);
	}
	
	protected DatabaseConnectionProvider buildDatabaseConnectionProvider() {
		
		DatabaseConnectionProvider connectionProvider = null; 
		
		if (getTestConfiguration() != null) {
			connectionProvider = (DatabaseConnectionProvider) buildInstanceFor(getTestConfiguration().connectionProvider(), DEFAULT_CONNECTION_PROVIDER);
		} else {
			connectionProvider = DEFAULT_CONNECTION_PROVIDER;
		}
		
		return connectionProvider;
	}

	public void tearDown() {
		getEntityManager().close();
	}
	
	protected void beginTransaction() {
		getEntityManager().getTransaction().begin();
	}

	protected void commitTransaction() {
		getEntityManager().getTransaction().commit();
	}
	
	protected void rollbackTransaction() {
		if (getEntityManager().getTransaction().isActive()) {
			getEntityManager().getTransaction().rollback();
		}
	}
	
	
	//region getters and setters

	public static EntityManager getEntityManager() {
		return entityManager;
	}

	public static void setEntityManager(EntityManager entityManager) {
		JPATest.entityManager = entityManager;
	}

	public IDatabaseConnection getDbUnitconnection() {
		return dbUnitconnection;
	}

	public TestConfiguration getTestConfiguration() {
		return testConfiguration;
	}

	public void setTestConfiguration(TestConfiguration testConfiguration) {
		this.testConfiguration = testConfiguration;
	}

	//endregion
	
	

}
