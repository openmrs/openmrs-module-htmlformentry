package org.openmrs.module.htmlformentry.db.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Form;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.db.HtmlFormEntryDAO;

/**
 * Hibernate implementation of the Data Access Object
 */
public class HibernateHtmlFormEntryDAO implements HtmlFormEntryDAO {

    private SessionFactory sessionFactory;
    
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public HtmlForm getHtmlForm(Integer id) {
        return (HtmlForm) sessionFactory.getCurrentSession().get(HtmlForm.class, id);
    }
    
    public HtmlForm saveHtmlForm(HtmlForm htmlForm) {
        sessionFactory.getCurrentSession().saveOrUpdate(htmlForm);
        return htmlForm;
    }
    
    public void deleteHtmlForm(HtmlForm htmlForm) {
        sessionFactory.getCurrentSession().delete(htmlForm);
    }

    @SuppressWarnings("unchecked")
    public List<HtmlForm> getAllHtmlForms() {
    	Query query = sessionFactory.getCurrentSession().createQuery("from HtmlForm order by form.name asc");
    	return (List<HtmlForm>) query.list();
    }

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
	
	@SuppressWarnings("unchecked")
	public List<Object[]> getProviderStubs(String roleName){
	    
	    String query = " select distinct u.person_id, pn.given_name, pn.family_name from users u, person_name pn, user_role ur where u.retired = 0 and u.person_id = pn.person_id and pn.voided = 0 and u.user_id = ur.user_id  ";
	    if (roleName != null)
	        query += " and ur.role = '" + roleName + "' ";
	     query += " order by family_name ";
	    return sessionFactory.getCurrentSession().createSQLQuery(query)
	        .addScalar("person_id", Hibernate.INTEGER)
	        .addScalar("given_name", Hibernate.STRING)
	        .addScalar("family_name", Hibernate.STRING).list();
	}

}
