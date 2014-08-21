package org.jormunit.dataset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Loads the DBUnit's {@link org.dbunit.dataset.IDataSet} from the XML and compares it with the current database state</p>
 * 
 * <p>This data set is called "expected data set" whereas the current database state is called "actual data set".
 * Only the columns presented on the expected data set will be used on comparison. Rows ordering will not be taken in consideration</p>
 * 
 * <p>Columns that contain data that is not predictable such as database generated IDs or time stamps should not be included on expected data sets</p>
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AfterDataSet {
	
	/**
	 * XML file to be loaded. The file must be in the class path  
	 */
	String fileName();	
}