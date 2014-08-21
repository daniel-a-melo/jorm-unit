#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class Customer {
	
	@Id
	@SequenceGenerator(name = "seq_gen_customer", sequenceName = "seq_customer", allocationSize=1)
	@GeneratedValue(strategy = SEQUENCE, generator = "seq_gen_customer")
	private Long id;
    private String name;

	public Customer() {
	}
	
	public Customer(String name) {
		this();
		setName(name);
	}

	//region getters and setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	//endregion

}
