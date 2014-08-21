package org.jormunit.dataset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Loads the DBUnit's {@link org.dbunit.dataset.IDataSet} from the XML file specified into the database.
 * All tables referenced in the XML file will have all rows deleted before loading the data</p>
 * 
 * <p>The file must be in the class path</p>
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface BeforeDataSet {
	/**
	 * XML file to be loaded. The file must be in the class path  
	 */
	String fileName();	
}
