package modelo;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;


/**
 * Esta modelo utilizará Hibernate para acceder a los datos.
 * 
 * @author Alberto Polo
 *
 */
public class AlumnosHibernate implements AlumnosDAO {

	private static SessionFactory sessionFactory;

	static {
		try {
			// Cargar la configuración de Hibernate
			sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
			System.out.println("✅ Hibernate inicializado correctamente.");
		} catch (Throwable ex) {
			throw new ExceptionInInitializerError("❌ Error al inicializar Hibernate: " + ex);
		}
	}

	/**
	 * Obtiene una sesión de Hibernate.
	 *
	 * @return una nueva sesión
	 */
	private Session getSession() {
		return sessionFactory.openSession();
	}

	@Override
    public boolean insertarAlumno(Alumno alumno) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.persist(alumno);
            tx.commit();
            System.out.println("✅ Alumno insertado en Hibernate.");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion) {
        try (Session session = getSession()) {
            List<Alumno> alumnos = session.createQuery("FROM Alumno", Alumno.class).getResultList();
            for (Alumno alumno : alumnos) {
                System.out.println(alumno);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean modificarNombreAlumnoPorNIA(int nia, String nuevoNombre) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            Alumno alumno = session.get(Alumno.class, nia);
            if (alumno != null) {
                alumno.setNombre(nuevoNombre);
                session.merge(alumno);
                tx.commit();
                System.out.println("✅ Nombre actualizado en Hibernate.");
                return true;
            }
            return false;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminarAlumnoPorNIA(int nia) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            Alumno alumno = session.get(Alumno.class, nia);
            if (alumno != null) {
                session.remove(alumno);
                tx.commit();
                System.out.println("✅ Alumno eliminado en Hibernate.");
                return true;
            }
            return false;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

	@Override
	public Alumno solicitarDatosAlumno() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean mostrarAlumnoPorNIA(int nia) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean eliminarAlumnosPorApellidos(String apellidos) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarAlumnosEnFicheroTexto() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean leerAlumnosDeFicheroTexto() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarAlumnosEnFicheroJSON() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean leerAlumnosDeFicheroJSON() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean insertarGrupo(Grupo grupo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean eliminarAlumnosPorGrupo(String grupo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarGruposEnFicheroJSON() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean leerGruposDeFicheroJSON() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean guardarGruposEnXML() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean leerYGuardarGruposXML(String rutaArchivo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void mostrarAlumnosPorGrupo() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean cambiarGrupoAlumno() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean guardarGrupoEspecificoEnXML() {
		// TODO Auto-generated method stub
		return false;
	}
}
