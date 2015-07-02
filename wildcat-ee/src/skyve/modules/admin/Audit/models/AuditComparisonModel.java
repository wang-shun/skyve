package modules.admin.Audit.models;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import modules.admin.domain.Audit;
import modules.admin.domain.Audit.Operation;

import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Reference;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.metadata.view.model.ComparisonComposite;
import org.skyve.metadata.view.model.ComparisonComposite.Mutation;
import org.skyve.metadata.view.model.ComparisonModel;
import org.skyve.metadata.view.model.ComparisonProperty;
import org.skyve.persistence.Persistence;
import org.skyve.util.Binder;
import org.skyve.util.Binder.TargetMetaData;
import org.skyve.wildcat.bind.BindUtil;
import org.skyve.wildcat.metadata.model.document.field.Enumeration;
import org.skyve.wildcat.metadata.repository.AbstractRepository;
import org.skyve.wildcat.metadata.view.widget.bound.input.TextField;
import org.skyve.wildcat.util.JSONUtil;

public class AuditComparisonModel extends ComparisonModel<Audit, Audit> {
	private static final long serialVersionUID = 5964879680504956032L;

	@Override
	public ComparisonComposite getComparisonComposite(Audit me) throws Exception {
		Audit sourceVersion = me.getSourceVersion();
		Audit comparisonVersion = me.getComparisonVersion();
		
		Persistence p = CORE.getPersistence();
		User u = p.getUser();
		Customer c = u.getCustomer();
		Module am = c.getModule(sourceVersion.getAuditModuleName());
		Document ad = am.getDocument(c, sourceVersion.getAuditDocumentName());
		boolean deleted = Operation.delete.equals(sourceVersion.getOperation());

		final Map<String, ComparisonComposite> bindingToNodes = new LinkedHashMap<>();
		
		// Visit the source audit record
		@SuppressWarnings("unchecked")
		Map<String, Object> source = (Map<String, Object>) JSONUtil.unmarshall(u, sourceVersion.getAudit());
		for (String binding : source.keySet()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> sourceValues = (Map<String, Object>) source.get(binding);

			if (binding.isEmpty()) {
				bindingToNodes.put(binding, createNode(c, null, ad, sourceValues, deleted));
			}
			else {
				TargetMetaData target = null;
				try {
					target = Binder.getMetaDataForBinding(c, am, ad, binding);
					Reference reference = (Reference) target.getAttribute();
					Document referenceDocument = am.getDocument(c, reference.getDocumentName());
					bindingToNodes.put(binding, createNode(c, reference, referenceDocument, sourceValues, deleted));
				}
				catch (MetaDataException e) {
					bindingToNodes.put(binding, createNode(c, null, null, sourceValues, deleted));
				}
			}
		}
		
		// Visit the comparison audit record, if there is one
		if (comparisonVersion != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> compare = (Map<String, Object>) JSONUtil.unmarshall(u, comparisonVersion.getAudit());
			for (String binding : compare.keySet()) {
				ComparisonComposite node = bindingToNodes.get(binding);
				@SuppressWarnings("unchecked")
				Map<String, Object> compareValues = (Map<String, Object>) compare.get(binding);

				if (binding.isEmpty()) {
					if (node == null) {
						bindingToNodes.put(binding, createNode(c, null, ad, compareValues, true));
					}
					else {
						updateNode(node, c, compareValues);
					}
				}
				else {
					if (node == null) {
						TargetMetaData target = null;
						try {
							target = Binder.getMetaDataForBinding(c, am, ad, binding);
							Reference reference = (Reference) target.getAttribute();
							Document referenceDocument = am.getDocument(c, reference.getDocumentName());
							bindingToNodes.put(binding, createNode(c, reference, referenceDocument, compareValues, true));
						}
						catch (MetaDataException e) {
							bindingToNodes.put(binding, createNode(c, null, null, compareValues, true));
						}
					}
					else {
						updateNode(node, c, compareValues);
					}
				}
			}
		}

		// Link it all together
		ComparisonComposite result = null;
		for (String binding : bindingToNodes.keySet()) {
			ComparisonComposite node = bindingToNodes.get(binding);
			if ("".equals(binding)) {
				result = node;
			}
			else {
				int lastDotIndex = binding.lastIndexOf('.');
				if (lastDotIndex > 0) {
					String parentBinding = binding.substring(0, lastDotIndex);
					bindingToNodes.get(parentBinding).getChildren().add(node);
				}
				else {
					if (result != null) {
						result.getChildren().add(node);
					}
				}
			}
		}
		
		return result;
	}

	private static ComparisonComposite createNode(Customer c,
													Reference owningReference,
													Document referenceDocument,
													Map<String, Object> values,
													boolean deleted)
	throws Exception {
		ComparisonComposite result = new ComparisonComposite();
		result.setBizId((String) values.remove(Bean.DOCUMENT_ID));
		String description = (String) values.remove(Bean.BIZ_KEY);
		if (description == null) {
			description = (referenceDocument == null) ? "" : referenceDocument.getSingularAlias();
		}
		result.setBusinessKeyDescription(description);

		if (owningReference == null) {
			result.setReferenceName(null);
			result.setRelationshipDescription((referenceDocument == null) ? "" : referenceDocument.getSingularAlias());
		}
		else {
			result.setReferenceName(owningReference.getName());
			result.setRelationshipDescription(owningReference.getDisplayName());
		}
		result.setMutation(deleted ? Mutation.deleted:  Mutation.added);
		result.setDocument(referenceDocument);
		addProperties(c, result, values, deleted);

		return result;
	}

	private static void addProperties(Customer c,
										ComparisonComposite node,
										Map<String, Object> values,
										boolean deleted)
	throws Exception {
		Document nodeDocument = node.getDocument();
		List<ComparisonProperty> properties = node.getProperties();
		
		for (String name : values.keySet()) {
			Object value = values.get(name);

			ComparisonProperty property = new ComparisonProperty();
			property.setName(name);

			// Coerce the value to the attributes type if the attribute still exists
			Attribute attribute = nodeDocument.getAttribute(name);
			if (attribute == null) { // attribute DNE
				property.setTitle(name);
				property.setWidget(new TextField());
			}
			else { // attribute exists
				property.setTitle(attribute.getDisplayName());
				property.setWidget(attribute.getDefaultInputWidget());

				Class<?> type = null;
				if (attribute instanceof Enumeration) {
					type = AbstractRepository.get().getEnum((Enumeration) attribute);
				}
				else {
					type = attribute.getAttributeType().getImplementingType();
				}

				if (value instanceof String) {
					value = BindUtil.fromString(c, null, type, (String) value, true);
				}
				else {
					value = BindUtil.convert(type, value);
				}
			}

			property.setNewValue(deleted ? null : value);
			property.setOldValue(deleted ? value : null);

			properties.add(property);
		}
	}

	private static void updateNode(ComparisonComposite node,
									Customer c,
									Map<String, Object> values)
	throws Exception {
		if (values != null) {
			boolean nodeDirty = false;
			
			List<ComparisonProperty> properties = node.getProperties();
			for (ComparisonProperty property : properties) {
				Object value = values.remove(property.getName());
				Attribute attribute = node.getDocument().getAttribute(property.getName());
				if (attribute != null) {
					Class<?> type = null;
					if (attribute instanceof Enumeration) {
						type = AbstractRepository.get().getEnum((Enumeration) attribute);
					}
					else {
						type = attribute.getAttributeType().getImplementingType();
					}

					if (value instanceof String) {
						value = BindUtil.fromString(c, null, type, (String) value, true);
					}
					else {
						value = BindUtil.convert(type, value);
					}
				}

				property.setOldValue(value);
				if ((! nodeDirty) && property.isDirty()) {
					nodeDirty = true;
				}
			}

			values.remove(Bean.DOCUMENT_ID);
			values.remove(Bean.BIZ_KEY);
			
			// Process any extra old properties not present in the new version
			for (String name : values.keySet()) {
				ComparisonProperty property = new ComparisonProperty();
				property.setName(name);
				property.setTitle(name);
				property.setOldValue(values.get(name));
				properties.add(property);
				nodeDirty = true;
			}
			node.setMutation(nodeDirty ? Mutation.updated : Mutation.unchanged);
		}
	}
}