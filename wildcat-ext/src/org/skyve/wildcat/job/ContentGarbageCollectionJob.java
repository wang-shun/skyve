package org.skyve.wildcat.job;

import java.util.Set;
import java.util.TreeSet;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.skyve.CORE;
import org.skyve.EXT;
import org.skyve.domain.Bean;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.repository.Repository;
import org.skyve.persistence.Persistence;
import org.skyve.persistence.SQL;
import org.skyve.util.Util;
import org.skyve.wildcat.content.ContentManager;
import org.skyve.wildcat.content.SearchResult;
import org.skyve.wildcat.util.UtilImpl;

/**
 * This job removes orphaned uploads and any textually indexed data left from delete/truncate SQL statements issued.
 * @author sandsm01
 *
 */
public class ContentGarbageCollectionJob implements Job {
	private Set<String> orphanedContentIds = new TreeSet<>();
	
	@Override
	public void execute(JobExecutionContext context)
	throws JobExecutionException {
		try {
			Repository r = CORE.getRepository();
			Persistence p = CORE.getPersistence();
			try {
				try (ContentManager cm = EXT.newContentManager()) {
					for (SearchResult result : cm.all()) {
						Customer customer = r.getCustomer(result.getCustomerName());
						Module module = customer.getModule(result.getModuleName());
						Document document = module.getDocument(customer, result.getDocumentName());
						
						SQL query = null;
						StringBuilder sql = new StringBuilder(128);
						sql.append("select 1 from ").append(document.getPersistent().getPersistentIdentifier());
						sql.append(" where ").append(Bean.DOCUMENT_ID).append(" = :").append(Bean.DOCUMENT_ID);
						
						// check if we have a record
						String binding = result.getBinding();
						if (binding != null) { // attachment
							sql.append(" and ").append(binding).append(" = :").append(binding);

							query = p.newSQL(sql.toString());
							query.putParameter(Bean.DOCUMENT_ID, result.getBizId());
							query.putParameter(binding, result.getContentId());
						}
						else { // bean
							query = p.newSQL(sql.toString());
							query.putParameter(Bean.DOCUMENT_ID, result.getBizId());
						}
						
						if (p.retrieve(query).isEmpty()) {
							orphanedContentIds.add(result.getContentId());
						}
					}
					
					for (String contentId : orphanedContentIds) {
						UtilImpl.LOGGER.info("ContentGarbageCollectionJob: Remove content with ID " + contentId);
						cm.remove(contentId);
					}
				}
			}
			finally {
				p.commit(true);
			}
			Util.LOGGER.info("Successfully performed CMS garbage collection");
		}
		catch (Exception e) {
			throw new JobExecutionException("Error encountered whilst performing CMS garbage collection", e);
		}
	}
}