package org.openmrs.module.htmlformentry.db.hibernate;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.openmrs.Form;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.db.HtmlFormEntryDAO;
import org.openmrs.module.htmlformentry.element.PersonStub;

/**
 * Hibernate implementation of the Data Access Object
 */
public class HibernateHtmlFormEntryDAO implements HtmlFormEntryDAO {

	private static Log log = LogFactory.getLog(HibernateHtmlFormEntryDAO.class);
	
    private DbSessionFactory sessionFactory;
    
    public void setSessionFactory(DbSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    @Override
    public HtmlForm getHtmlForm(Integer id) {
        return (HtmlForm) sessionFactory.getCurrentSession().get(HtmlForm.class, id);
    }
    
    @Override
    public HtmlForm getHtmlFormByUuid(String uuid)  {
        Query q = sessionFactory.getCurrentSession().createQuery("from HtmlForm f where f.uuid = :uuid");
        return (HtmlForm) q.setString("uuid", uuid).uniqueResult();
    }
    
    @Override
    public HtmlForm saveHtmlForm(HtmlForm htmlForm) {
        sessionFactory.getCurrentSession().saveOrUpdate(htmlForm);
        return htmlForm;
    }
    
    @Override
    public void deleteHtmlForm(HtmlForm htmlForm) {
        sessionFactory.getCurrentSession().delete(htmlForm);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HtmlForm> getAllHtmlForms() {
    	Query query = sessionFactory.getCurrentSession().createQuery("from HtmlForm order by form.name asc");
    	return (List<HtmlForm>) query.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public HtmlForm getHtmlFormByForm(Form form) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(HtmlForm.class);
        crit.add(Restrictions.eq("form", form));
        crit.addOrder(Order.desc("dateCreated"));
        List<HtmlForm> list = (List<HtmlForm>) crit.list();
        if (list.size() >= 1)
            return list.get(0);
        else
            return null;
    }

	@Override
    public boolean needsNameAndDescriptionMigration() {
		Query query = sessionFactory.getCurrentSession().createQuery("select count(*) from HtmlForm where deprecatedName is not null or deprecatedDescription is not null");
		return ((Number) query.uniqueResult()).intValue() > 0;
    }
	
	@Override
    @SuppressWarnings("unchecked")
	public List<PersonStub> getUsersAsPersonStubs(String roleName){
	    String query = " select  u.person_id as id, pn.given_name as givenName, pn.family_name as familyName, pn.middle_name as middleName, pn.family_name2 as familyName2 from users u, person_name pn, user_role ur where u.retired = 0 and u.person_id = pn.person_id and pn.voided = 0 and u.user_id = ur.user_id  ";
	    if (roleName != null)
	        query += " and ur.role = '" + roleName + "' ";
	     query += " order by familyName ";
	    return (List<PersonStub>) sessionFactory.getCurrentSession().createSQLQuery(query)
	    .addScalar("id")
	    .addScalar("givenName")
	    .addScalar("familyName")
	    .addScalar("middleName")
	    .addScalar("familyName2")
	    .setResultTransformer(Transformers.aliasToBean(PersonStub.class)).list();
	}

	 @Override
    public OpenmrsObject getItemByUuid(Class<? extends OpenmrsObject> type, String uuid) {
		try {
			Criteria criteria = sessionFactory.getCurrentSession().createCriteria(type);
			criteria.add(Expression.eq("uuid", uuid));
			OpenmrsObject result = (OpenmrsObject) criteria.uniqueResult();
			return result;
		}
		catch(Exception e) {
    		log.error("Error fetching item by uuid:" + e);
    		return null;
    	}
	 }

	
	 @Override
    public OpenmrsObject getItemById(Class<? extends OpenmrsObject> type, Integer id) {
    	 try {
	    	 String idProperty = sessionFactory.getHibernateSessionFactory().getClassMetadata(type).getIdentifierPropertyName();
		 	 Criteria criteria = sessionFactory.getCurrentSession().createCriteria(type);
		 	 criteria.add(Expression.eq(idProperty, id));
		 	 OpenmrsObject result = (OpenmrsObject) criteria.uniqueResult();
		 	 return result;
    	 }
    	 catch(Exception e) {
     		log.error("Error fetching item by id:" + e);
     		return null;
     	}
	 }

    @Override
    public OpenmrsObject getItemByName(Class<? extends OpenmrsMetadata> type, String name) {
    	// we use a try/catch here to handle oddities like "Role" which don't have a directly-referenceable name property
    	try {
    		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(type);
    		criteria.add(Expression.eq("name", name));
    		OpenmrsObject result = (OpenmrsObject) criteria.uniqueResult();
    		return result;
    	}
    	catch(Exception e) {
    		log.error("Error fetching item by name:" + e);
    		return null;
    	}
    }
    
    @Override
    @SuppressWarnings("unchecked")
	public List<Integer> getPersonIdHavingAttributes(String attribute, String attributeValue) {
	    String query =  "select distinct(pa.person_id) from person_attribute pa, person_attribute_type pat where pa.person_attribute_type_id = pat.person_attribute_type_id and pat.name='" + attribute + "'";
		if(attributeValue != null)
		{
			query = query + " and value='" + attributeValue + "'";
		}
	    return (List<Integer>)sessionFactory.getCurrentSession().createSQLQuery(query).list();
    }
}
