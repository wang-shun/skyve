package org.skyve.wildcat.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.skyve.bizport.BizPortWorkbook;
import org.skyve.content.MimeType;
import org.skyve.domain.messages.SessionEndedException;
import org.skyve.metadata.controller.BizExportAction;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.wildcat.domain.messages.SecurityException;
import org.skyve.wildcat.metadata.customer.CustomerImpl;
import org.skyve.wildcat.metadata.repository.AbstractRepository;
import org.skyve.wildcat.persistence.AbstractPersistence;

public class BizportExportServlet extends HttpServlet {
	/**
	 * For Serialization
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		try (OutputStream out = response.getOutputStream()) {
			AbstractPersistence persistence = AbstractPersistence.get();
			try {
				try {
					persistence.begin();
					User user = WebUtil.processUserPrincipalForRequest(request, request.getUserPrincipal().getName(), true);
					if (user == null) {
						throw new SessionEndedException();
					}
					persistence.setUser(user);

					AbstractRepository repository = AbstractRepository.get();
					CustomerImpl customer = (CustomerImpl) user.getCustomer();
		
					String documentName = request.getParameter(AbstractWebContext.DOCUMENT_NAME);
					int dotIndex = documentName.indexOf('.');
					String moduleName = documentName.substring(0, dotIndex);
					documentName = documentName.substring(dotIndex + 1);
					Module module = customer.getModule(moduleName);
					Document document = module.getDocument(customer, documentName);

					String actionName = request.getParameter(AbstractWebContext.RESOURCE_FILE_NAME);
					if (! user.canExecuteAction(document, actionName)) {
						throw new SecurityException(actionName, user.getName());
					}

					BizExportAction bizPortAction = repository.getBizExportAction(customer, 
																					document, 
																					actionName);
					String contextKey = request.getParameter(AbstractWebContext.CONTEXT_NAME);
		        	AbstractWebContext context = WebUtil.getCachedConversation(contextKey, request, response);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					boolean vetoed = customer.interceptBeforeBizExportAction(document, actionName, context);
					BizPortWorkbook result = null;
					if (! vetoed) {
						result = bizPortAction.bizExport(context);
						customer.interceptAfterBizExportAction(document, actionName, result, context);
						if (result != null) {
							result.write(baos);
						}
					}
		            byte[] bytes = baos.toByteArray();
		            
		            if (result != null) {
						switch (result.getFormat()) {
						case xls:
							response.setContentType(MimeType.excel.toString());
							response.setCharacterEncoding(ServletConstants.UTF8);
							response.setHeader("Content-Disposition", "attachment; filename=\"bizport.xls\"");
							break;
						case xlsx:
							break;
						default:
						}
		            }
		
		            response.setContentLength(bytes.length);
		            
		    		// NEED TO KEEP THIS FOR IE TO SHOW PDFs ACTIVE-X temp files required
		    		response.setHeader("Cache-Control", "cache");
		            response.setHeader("Pragma", "cache");
		            response.addDateHeader("Expires", System.currentTimeMillis() + (60000)); // 1 minute
		
		            out.write(bytes);
		            out.flush();
				}
				catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}
			catch (Throwable t) {
				System.err.println("Problem generating the export - " + t.toString());
				t.printStackTrace();
				response.setContentType(MimeType.html.toString());
				response.setCharacterEncoding(ServletConstants.UTF8);
				out.write("<html><head/><body><h3>".getBytes(ServletConstants.UTF8));
				out.write("An error occured whilst processing your report.".getBytes(ServletConstants.UTF8));
				out.write("</body></html>".getBytes(ServletConstants.UTF8));
			}
			finally {
				persistence.commit(true);
			}
		}
	}
}
