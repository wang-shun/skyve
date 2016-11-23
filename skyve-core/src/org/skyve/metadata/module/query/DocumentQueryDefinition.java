package org.skyve.metadata.module.query;

import java.util.List;

import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.module.Module;
import org.skyve.persistence.DocumentQuery;
import org.skyve.persistence.DocumentQuery.AggregateFunction;

/**
 * 
 */
public interface DocumentQueryDefinition extends QueryDefinition {
	/**
	 * 
	 * @return
	 */
	public String getDocumentName();
	
	/**
	 * 
	 * @param customer
	 * @return
	 */
	public Module getDocumentModule(Customer customer);
	
	/**
	 * 
	 * @return
	 */
	public String getFromClause();
	
	/**
	 * 
	 * @return
	 */
	public String getFilterClause();
	
	/**
	 * 
	 * @return
	 */
	public List<QueryColumn> getColumns();
	
	/**
	 * 
	 * @param summaryType
	 * @param tagId
	 * @return
	 */
	public DocumentQuery constructDocumentQuery(AggregateFunction summaryType, String tagId);
}
