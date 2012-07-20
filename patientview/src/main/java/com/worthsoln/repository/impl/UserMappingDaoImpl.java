package com.worthsoln.repository.impl;

import com.worthsoln.patientview.logon.UserMapping;
import com.worthsoln.patientview.model.User;
import com.worthsoln.repository.AbstractHibernateDAO;
import com.worthsoln.repository.UserMappingDao;

import java.util.List;

/**
 *
 */
public class UserMappingDaoImpl extends AbstractHibernateDAO<UserMapping> implements UserMappingDao {

    @Override
    public void deleteUserMappings(String username, String unitcode) {

        List<UserMapping> userMappings = getAll(username, unitcode);

        try {
            for (UserMapping userMapping : userMappings) {
                delete(userMapping);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<UserMapping> getAll(String username) {

//        List userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.username = ? ",
//                    new Object[]{username}, new Type[]{Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserMapping> getAllExcludeUnitcode(String username, String unitcode) {

//        List userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.username = ? and usermapping.unitcode != ?",
//                    new Object[]{username, unitcode}, new Type[]{Hibernate.STRING, Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserMapping> getAll(String username, String unitcode) {

//        List userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.username = ? and usermapping.unitcode = ?",
//                    new Object[]{username, unitcode}, new Type[]{Hibernate.STRING, Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserMapping> getAllForNhsNo(String nhsNo) {

//        List<UserMapping> userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.nhsno = ? ",
//                    new Object[]{nhsno}, new Type[]{Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUsersRealUnitcodeBestGuess(String username) {

//        List<UserMapping> userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.username = ? and usermapping.unitcode != ?",
//                    new Object[]{username, UnitUtils.PATIENT_ENTERS_UNITCODE}, new Type[]{Hibernate.STRING, Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }
//
//        if (userMappings.isEmpty()) {
//            return "";
//        } else {
//            return userMappings.get(0).getUnitcode();
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUsersRealNhsNoBestGuess(String username) {

//        List<UserMapping> userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.username = ? and usermapping.unitcode != ?",
//                    new Object[]{username, UnitUtils.PATIENT_ENTERS_UNITCODE}, new Type[]{Hibernate.STRING, Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }
//
//        if (userMappings.isEmpty()) {
//            return "";
//        } else {
//            return userMappings.get(0).getNhsno();
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserMapping getUserMappingPatientEntered(User user) {

//        List userMappings = new ArrayList();
//        try {
//            Session session = HibernateUtil.currentSession();
//            Transaction tx = session.beginTransaction();
//
//
//            userMappings = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                    " where usermapping.username = ? ",
//                    new Object[]{user.getUsername()}, new Type[]{Hibernate.STRING});
//            tx.commit();
//            HibernateUtil.closeSession();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//        }
//
//
//        UserMapping patientEntryUserMapping = null;
//        UserMapping anyOtherUserMapping = null;
//
//        for (Object obj : userMappings) {
//            UserMapping currentUserMapping = (UserMapping) obj;
//
//            if (UnitUtils.PATIENT_ENTERS_UNITCODE.equals(currentUserMapping.getUnitcode())) {
//                patientEntryUserMapping = currentUserMapping;
//            } else {
//                anyOtherUserMapping = currentUserMapping;
//            }
//        }
//
//        if (patientEntryUserMapping == null) {
//            if (anyOtherUserMapping != null) {
//                patientEntryUserMapping = anyOtherUserMapping;
//                patientEntryUserMapping = new UserMapping(anyOtherUserMapping.getUsername(), UnitUtils.PATIENT_ENTERS_UNITCODE, anyOtherUserMapping.getNhsno());
//                try {
//                    HibernateUtil.saveOrUpdateWithTransaction(patientEntryUserMapping);
//                } catch (HibernateException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserMapping> getUsersSiblings(String username, String unitcode) {

//        Session session = HibernateUtil.currentSession();
//        Transaction tx = session.beginTransaction();
//        List<UserMapping> duplicateUsers = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                " WHERE (usermapping.username = ? OR usermapping.username = ?) " +
//                " AND (usermapping.unitcode = ? OR usermapping.unitcode = ?) ",
//                new Object[]{username, username + "-GP", unitcode, "PATIENT"},
//                new Type[]{Hibernate.STRING, Hibernate.STRING, Hibernate.STRING, Hibernate.STRING});
//        tx.commit();
//        HibernateUtil.closeSession();

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserMapping> getDuplicateUsers(String nhsno, String username) {

//        Session session = HibernateUtil.currentSession();
//        Transaction tx = session.beginTransaction();
//        List duplicateUsers = session.find("from " + UserMapping.class.getName() + " as usermapping " +
//                " where usermapping.nhsno = ? AND usermapping.username <> ? AND usermapping.username not like ?",
//                new Object[]{nhsno, username, "%-GP"},
//                new Type[]{Hibernate.STRING, Hibernate.STRING, Hibernate.STRING});
//        tx.commit();
//        HibernateUtil.closeSession();

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}