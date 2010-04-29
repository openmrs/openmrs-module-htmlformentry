package org.openmrs.module.htmlformentry.db.hibernate;

import java.util.List;

import org.hibernate.Criteria;
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
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(HtmlForm.class);
        crit.addOrder(Order.asc("name"));
        return (List<HtmlForm>) crit.list();
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

}
