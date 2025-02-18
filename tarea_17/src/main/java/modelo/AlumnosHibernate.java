package modelo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

/**
 * Utilizará Hibernate para acceder a los datos.
 * 
 * @author Alberto Polo
 */
public class AlumnosHibernate implements AlumnosDAO {

	private final static Scanner sc = new Scanner(System.in);

	private static SessionFactory sessionFactory;

	static {
		try {
			// Cargar la configuración de Hibernate
			// Sin especificar el nombre, Hibernate buscará automáticamente
			// hibernate.cfg.xml en src/main/resources/.
			sessionFactory = new Configuration().configure().buildSessionFactory();
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
		Session session = null; // 🔹 Inicializamos `session` en null para asegurarnos de cerrarla en `finally`
		try {
			session = getSession();
			tx = session.beginTransaction();
			session.persist(alumno);
			tx.commit();
			System.out.println("✅ Alumno insertado en Hibernate.");
			return true;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return false;
		} finally {
			if (session != null)
				session.close(); // 🔹 Cerramos la sesión solo si fue abierta correctamente
		}
	}

	@Override
	public boolean insertarGrupo(Grupo grupo) {
		Transaction tx = null;
		try (Session session = getSession()) {
			tx = session.beginTransaction();
			session.persist(grupo); // Guardar el grupo en la base de datos
			tx.commit();
			System.out.println("✅ Grupo insertado correctamente en Hibernate.");
			return true;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			System.out.println("❌ Error al insertar el grupo: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion) {
		try (Session session = getSession()) {
			List<Alumno> alumnos = session.createQuery("FROM Alumno", Alumno.class).getResultList();

			if (alumnos.isEmpty()) {
				System.out.println("No hay alumnos registrados.");
				return false;
			}

			for (Alumno alumno : alumnos) {
				if (mostrarTodaLaInformacion) {
					System.out.printf("""
							-------------------------
							NIA: %d
							Nombre: %s
							Apellidos: %s
							Género: %s
							Fecha de nacimiento: %s
							Ciclo: %s
							Curso: %s
							Grupo: %s
							""", alumno.getNia(), alumno.getNombre(), alumno.getApellidos(), alumno.getGenero(),
							new SimpleDateFormat("dd-MM-yyyy").format(alumno.getFechaNacimiento()), alumno.getCiclo(),
							alumno.getCurso(),
							(alumno.getGrupo() != null ? alumno.getGrupo().getNombreGrupo() : "Sin grupo"));
				} else {
					System.out.printf("NIA: %d, Nombre: %s%n", alumno.getNia(), alumno.getNombre());
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void guardarAlumnosEnFicheroTexto() {
	    String nombreArchivo = "alumnos.txt";

	    try (Session session = getSession();
	         BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo))) {

	        // Obtener todos los alumnos
	        List<Alumno> alumnos = session.createQuery("FROM Alumno", Alumno.class).getResultList();

	        if (alumnos.isEmpty()) {
	            System.out.println("No hay alumnos para guardar en el archivo.");
	            return;
	        }

	        // Escribir la cabecera del archivo
	        writer.write("NIA,Nombre,Apellidos,Género,Fecha Nacimiento,Ciclo,Curso,Nombre del Grupo");
	        writer.newLine();

	        // Escribir cada alumno en el archivo
	        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
	        for (Alumno alumno : alumnos) {
	            String linea = String.format("%d,%s,%s,%s,%s,%s,%s,%s",
	                    alumno.getNia(),
	                    alumno.getNombre(),
	                    alumno.getApellidos(),
	                    alumno.getGenero(),
	                    formatoFecha.format(alumno.getFechaNacimiento()),
	                    alumno.getCiclo(),
	                    alumno.getCurso(),
	                    (alumno.getGrupo() != null ? alumno.getGrupo().getNombreGrupo() : "Sin grupo"));

	            writer.write(linea);
	            writer.newLine();
	        }

	        System.out.println("✅ Alumnos guardados correctamente en " + nombreArchivo);

	    } catch (IOException e) {
	        System.out.println("❌ Error al guardar los alumnos en el archivo: " + e.getMessage());
	    } catch (Exception e) {
	        System.out.println("❌ Error inesperado: " + e.getMessage());
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
			if (tx != null)
				tx.rollback();
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
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public Alumno solicitarDatosAlumno() {
		System.out.println("Introduce el nombre del alumno:");
		String nombre = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce los apellidos:");
		String apellidos = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce el género (M/F):");
		char genero = sc.nextLine().toUpperCase().charAt(0);

		System.out.println("Introduce la fecha de nacimiento (dd-MM-yyyy):");
		Date fechaNacimiento;
		try {
			SimpleDateFormat formato = new SimpleDateFormat("dd-MM-yyyy");
			fechaNacimiento = formato.parse(sc.nextLine());
		} catch (Exception e) {
			System.out.println("❌ Formato de fecha incorrecto.");
			return null;
		}

		System.out.println("Introduce el ciclo:");
		String ciclo = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce el curso:");
		String curso = sc.nextLine().toUpperCase().trim();

		System.out.println("Introduce el nombre del grupo:");
		String nombreGrupo = sc.nextLine().toUpperCase().trim();

		// 🔹 Obtener el grupo desde la BD
		Grupo grupo;
		try (Session session = getSession()) {
			grupo = session.createQuery("FROM Grupo WHERE nombreGrupo = :nombre", Grupo.class)
					.setParameter("nombre", nombreGrupo).uniqueResult();
		}

		if (grupo == null) {
			System.out.println("❌ El grupo no existe en la BD. Debes crearlo antes de asignarlo a un alumno.");
			return null;
		}

		return new Alumno(nombre, apellidos, genero, fechaNacimiento, ciclo, curso, grupo);
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
