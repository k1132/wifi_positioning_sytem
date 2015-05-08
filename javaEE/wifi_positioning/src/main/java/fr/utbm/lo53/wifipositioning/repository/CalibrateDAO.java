package fr.utbm.lo53.wifipositioning.repository;

import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.utbm.lo53.wifipositioning.model.Measurement;
import fr.utbm.lo53.wifipositioning.model.Position;
import fr.utbm.lo53.wifipositioning.util.HibernateUtil;

/**
 * @Repository précise que c'est une classe de DAO, de requêtage de bdd.
 * @Transactionnal précise (sûrement) que la classe effectue des transactions
 *                 entre l'appli et la bdd (genre session.beginTransaction()
 *                 sûrement
 */
public class CalibrateDAO
{
	/** Logger of the class */
	private final static Logger		s_logger		= LoggerFactory.getLogger(CalibrateDAO.class);

	private static CalibrateDAO		s_calibrateDAO	= new CalibrateDAO();
	private final SessionFactory	m_sessionFactory;

	private CalibrateDAO()
	{
		m_sessionFactory = HibernateUtil.getSessionfactory();
	}

	public synchronized static CalibrateDAO getInstance()
	{
		return s_calibrateDAO;
	}

	public synchronized void insertSample(
			final Position _position)
	{
		s_logger.debug("CalibrateDAO inserting new Measurement-Position...");
		Session session = m_sessionFactory.getCurrentSession();
		try
		{
			session.beginTransaction();

			/* Query to see if the position already exists */
			Query hqlQuery = session.createQuery("FROM Position where x = :x and y = :y");
			hqlQuery.setParameter("x", _position.getX());
			hqlQuery.setParameter("y", _position.getY());

			@SuppressWarnings("unchecked")
			List<Object> resultList = hqlQuery.list();

			/*
			 * If it doesn't exist, we save the new position and its associates
			 * measurement.
			 */
			if (resultList.isEmpty())
			{
				s_logger.debug("The coordinates (x,y) have not been found in the database.");
				session.save(_position);

				/*
				 * The position already exists, so we set the measurement's new
				 * position and save it inside the database.
				 */
			} else
			{
				s_logger.debug("The coordinates (x,y) have been found in the database.");
				Iterator<Measurement> measurementIterator = _position.getMeasurements().iterator();
				while (measurementIterator.hasNext())
				{
					Measurement m = measurementIterator.next();
					m.setPosition((Position) resultList.get(0));
					session.save(m);
				}
			}

			session.getTransaction().commit();

			s_logger.debug("Insertion in the database successful.");
		} catch (HibernateException he)
		{
			he.printStackTrace();
			if (session.getTransaction() != null)
			{
				try
				{
					session.getTransaction().rollback();
				} catch (HibernateException he2)
				{
					he2.printStackTrace();
				}
			}
		} finally
		{
			if ((session != null) && session.isOpen())
			{
				session.close();
			}
		}
	}
}