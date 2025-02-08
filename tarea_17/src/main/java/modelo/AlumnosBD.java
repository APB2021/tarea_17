package modelo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pool.PoolConexiones;

public class AlumnosBD implements AlumnosDAO {

	private final static Scanner sc = new Scanner(System.in);

	private static final Logger loggerGeneral = LogManager.getRootLogger();
	private static final Logger loggerExcepciones = LogManager.getLogger("exceptions");

	@Override
	public boolean insertarAlumno(Connection conexionBD, Alumno alumno) throws SQLException {
		// Primero, obtener el numeroGrupo del grupo del alumno
		int numeroGrupo = obtenerNumeroGrupo(alumno.getGrupo().getNombreGrupo());

		if (numeroGrupo == -1) {
			loggerExcepciones.error("Error: El grupo '{}' no existe en la base de datos.",
					alumno.getGrupo().getNombreGrupo());
			return false;
		}

		String sql = "INSERT INTO alumnos (nombre, apellidos, genero, fechaNacimiento, ciclo, curso, numeroGrupo) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
			sentencia.setString(1, alumno.getNombre());
			sentencia.setString(2, alumno.getApellidos());
			sentencia.setString(3, String.valueOf(alumno.getGenero())); // Aseguramos que esté en mayúsculas
			sentencia.setDate(4, new java.sql.Date(alumno.getFechaNacimiento().getTime()));
			sentencia.setString(5, alumno.getCiclo());
			sentencia.setString(6, alumno.getCurso());
			sentencia.setInt(7, numeroGrupo); // Usar el numeroGrupo obtenido

			int filasAfectadas = sentencia.executeUpdate();

			if (filasAfectadas > 0) {
				loggerGeneral.info("Alumno '{}' '{}' insertado correctamente.", alumno.getNombre(),
						alumno.getApellidos());
				return true;
			} else {
				loggerExcepciones.error("No se pudo insertar el alumno '{}' '{}'.", alumno.getNombre(),
						alumno.getApellidos());
				return false;
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al insertar el alumno '{}' '{}': {}", alumno.getNombre(),
					alumno.getApellidos(), e.getMessage(), e);
			throw e; // Re-lanzamos la excepción para que la gestione el llamador
		}
	}

	/**
	 * Solicita al usuario los datos necesarios para crear un objeto Alumno.
	 * 
	 * @return Un objeto Alumno con los datos ingresados por el usuario.
	 */
	@Override
	public Alumno solicitarDatosAlumno() {
		String nombre, apellidos, ciclo, curso, nombreGrupo;
		char respuestaGenero;
		Date fechaNacimiento = null;

		try {
			System.out.println("Introduce el nombre del alumno:");
			nombre = sc.nextLine().toUpperCase().trim();

			System.out.println("Introduce los apellidos del alumno:");
			apellidos = sc.nextLine().toUpperCase().trim();

			// Validar género
			do {
				System.out.println("Introduce el género del alumno (M/F):");
				respuestaGenero = sc.nextLine().toUpperCase().charAt(0); // Convertir a mayúscula
				if (respuestaGenero != 'M' && respuestaGenero != 'F') {
					loggerGeneral.info("Entrada no válida para género: {}", respuestaGenero);
					System.out.println("Respuesta no válida. Introduce 'M' o 'F'.");
				}
			} while (respuestaGenero != 'M' && respuestaGenero != 'F');

			// Validar fecha de nacimiento
			do {
				System.out.println("Introduce la fecha de nacimiento (dd-MM-aaaa):");
				String fechaInput = sc.nextLine();
				try {
					SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
					formatoFecha.setLenient(false); // Validación estricta
					fechaNacimiento = formatoFecha.parse(fechaInput);
				} catch (ParseException e) {
					loggerExcepciones.error("Formato de fecha inválido ingresado: {}", e.getMessage());
					System.out.println("Formato de fecha inválido. Intenta de nuevo.");
				}
			} while (fechaNacimiento == null);

			System.out.println("Introduce el ciclo del alumno:");
			ciclo = sc.nextLine().trim().toUpperCase();

			System.out.println("Introduce el curso del alumno:");
			curso = sc.nextLine().trim().toUpperCase();

			// Validar nombre del grupo
			do {
				System.out.println("Introduce el nombre del grupo del alumno:");
				nombreGrupo = sc.nextLine().toUpperCase(); // Convertir a mayúsculas
				if (!validarNombreGrupo(nombreGrupo)) {
					loggerGeneral.info("Entrada no válida para grupo: {}", nombreGrupo);
					System.out.println("El nombre del grupo no es válido. Intenta de nuevo.");
				}
			} while (!validarNombreGrupo(nombreGrupo));

			// Crear el objeto Grupo
			Grupo grupo = new Grupo(nombreGrupo);

			// Crear y devolver el objeto Alumno
			Alumno alumno = new Alumno(nombre, apellidos, respuestaGenero, fechaNacimiento, ciclo, curso, grupo);
			loggerGeneral.info("Datos del alumno solicitados correctamente: {}", alumno);
			return alumno;

		} catch (Exception e) {
			loggerExcepciones.error("Error inesperado al solicitar datos del alumno: {}", e.getMessage(), e);
			throw new RuntimeException("Error al solicitar datos del alumno.", e);
		}
	}

	/**
	 * Recupera el número del grupo a partir de su nombre.
	 * 
	 * @param nombreGrupo El nombre del grupo.
	 * @return El numeroGrupo correspondiente o -1 si no existe.
	 */
	private int obtenerNumeroGrupo(String nombreGrupo) {
		String sql = "SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?";
		try (Connection conexionBD = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {

			sentencia.setString(1, nombreGrupo);
			loggerGeneral.info("Consultando numeroGrupo para el grupo: {}", nombreGrupo);

			try (ResultSet resultado = sentencia.executeQuery()) {
				if (resultado.next()) {
					int numeroGrupo = resultado.getInt("numeroGrupo");
					loggerGeneral.info("Grupo encontrado: {} con numeroGrupo: {}", nombreGrupo, numeroGrupo);
					return numeroGrupo;
				} else {
					loggerGeneral.info("No se encontró el grupo con nombre: {}", nombreGrupo);
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al obtener el numeroGrupo para el grupo {}: {}", nombreGrupo, e.getMessage(),
					e);
		}

		return -1; // Si no se encuentra el grupo, devolver -1
	}

	/**
	 * Valida si un nombre de grupo existe en la base de datos.
	 * 
	 * @param nombreGrupo El nombre del grupo a validar.
	 * @return true si el grupo existe, false en caso contrario.
	 */
	public boolean validarNombreGrupo(String nombreGrupo) {
		// Rendimiento mejorado: Uso de SELECT 1 reduce la carga en la base de datos.
		String sql = "SELECT 1 FROM grupos WHERE nombreGrupo = ?";
		try (Connection conexionBD = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {

			sentencia.setString(1, nombreGrupo);
			loggerGeneral.info("Validando existencia del grupo: {}", nombreGrupo);

			try (ResultSet resultado = sentencia.executeQuery()) {
				if (resultado.next()) {
					loggerGeneral.info("El grupo '{}' existe en la base de datos.", nombreGrupo);
					return true;
				} else {
					loggerGeneral.info("El grupo '{}' no existe en la base de datos.", nombreGrupo);
					return false;
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al validar el grupo '{}': {}", nombreGrupo, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Muestra todos los alumnos registrados.
	 * 
	 * @param conexionBD     La conexión a la base de datos.
	 * @param mostrarDetalle Indica si se debe mostrar toda la información (true) o
	 *                       solo NIA y nombre (false).
	 * @return true si se muestra la lista correctamente, false en caso contrario.
	 */
	public boolean mostrarTodosLosAlumnos(Connection conexionBD, boolean mostrarTodaLaInformacion) {
		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero, a.fechaNacimiento,
				           a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    LEFT JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    ORDER BY a.nia
				""";

		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql);
				ResultSet resultado = sentencia.executeQuery()) {

			if (!resultado.isBeforeFirst()) {
				System.out.println("No hay alumnos registrados.");
				loggerGeneral.info("La consulta de alumnos no devolvió resultados.");
				return false;
			}

			if (mostrarTodaLaInformacion) {
				System.out.println("Lista completa de alumnos registrados:");
			} else {
				System.out.println("Lista de alumnos (NIA y Nombre):");
			}
			loggerGeneral.info("Mostrando la lista de alumnos registrados.");

			// Scanner scanner = new Scanner(System.in); // Scanner para recoger el NIA del
			// usuario.
			boolean esSeleccion = !mostrarTodaLaInformacion; // Determinar si se pide al usuario seleccionar un NIA.
			int niaSeleccionado = -1;

			while (resultado.next()) {
				int nia = resultado.getInt("nia");
				String nombre = resultado.getString("nombre");

				if (mostrarTodaLaInformacion) {
					// Mostrar toda la información
					String apellidos = resultado.getString("apellidos");
					String genero = resultado.getString("genero");
					Date fechaNacimiento = resultado.getDate("fechaNacimiento");
					String ciclo = resultado.getString("ciclo");
					String curso = resultado.getString("curso");
					String nombreGrupo = resultado.getString("nombreGrupo");

					System.out.printf("""
							NIA: %d
							Nombre: %s
							Apellidos: %s
							Género: %s
							Fecha de nacimiento: %s
							Ciclo: %s
							Curso: %s
							Grupo: %s
							-------------------------
							""", nia, nombre, apellidos, genero, fechaNacimiento, ciclo, curso,
							nombreGrupo == null ? "Sin grupo" : nombreGrupo);
				} else {
					// Mostrar solo NIA y nombre
					System.out.printf("NIA: %d, Nombre: %s%n", nia, nombre);
				}
			}

			// Si el objetivo es que el usuario elija un NIA
			if (esSeleccion) {
				System.out.println("\nIntroduce el NIA del alumno que deseas visualizar:");
				while (true) {
					try {
						niaSeleccionado = Integer.parseInt(sc.nextLine().trim());
						break;
					} catch (NumberFormatException e) {
						System.out.println("El NIA debe ser un número. Inténtalo de nuevo:");
					}
				}

				// Llamar al método para mostrar todos los datos del alumno seleccionado
				if (!mostrarAlumnoPorNIA(conexionBD, niaSeleccionado)) {
					System.out.println("El NIA seleccionado no corresponde a ningún alumno.");
				}
			}

			return true;
		} catch (SQLException e) {
			loggerExcepciones.error("Error al recuperar la lista de alumnos: {}", e.getMessage(), e);
			System.out.println("Se produjo un error al recuperar los alumnos. Revisa los logs para más detalles.");
			return false;
		}
	}

	/**
	 * Guarda todos los alumnos en un fichero de texto. La información incluye sus
	 * datos y el grupo al que pertenecen. Los alumnos se ordenan de forma
	 * ascendente por su NIA.
	 * 
	 * @param conexionBD Conexión a la base de datos MySQL.
	 */
	@Override
	public void guardarAlumnosEnFicheroTexto(Connection conexionBD) {
		String nombreFichero = "alumnos.txt";
		File fichero = new File(nombreFichero);

		// Verificar si el archivo existe y pedir confirmación para sobreescribirlo
		if (fichero.exists()) {
			System.out.print("El fichero ya existe. ¿Desea sobreescribirlo? (S/N): ");
			char respuesta = sc.nextLine().toUpperCase().charAt(0);
			if (respuesta != 'S') {
				System.out.println("Operación cancelada. El fichero no se sobrescribirá.");
				loggerGeneral.info("El usuario decidió no sobrescribir el fichero '{}'.", nombreFichero);
				return;
			}
		}

		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero,
				           a.fechaNacimiento, a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    ORDER BY a.nia ASC
				""";

		// Intentar escribir en el fichero
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichero));
				PreparedStatement sentencia = conexionBD.prepareStatement(sql);
				ResultSet resultado = sentencia.executeQuery()) {

			// Escribir encabezados en el fichero
			writer.write("NIA,Nombre,Apellidos,Género,Fecha Nacimiento,Ciclo,Curso,Nombre del Grupo");
			writer.newLine();

			if (!resultado.isBeforeFirst()) {
				System.out.println("No hay alumnos registrados para guardar en el fichero.");
				loggerGeneral.info("No se encontraron alumnos en la base de datos para guardar en el fichero.");
				return;
			}

			// Escribir los datos de los alumnos en el fichero
			while (resultado.next()) {
				int nia = resultado.getInt("nia");
				String nombre = resultado.getString("nombre");
				String apellidos = resultado.getString("apellidos");
				String genero = resultado.getString("genero");
				String fechaNacimiento = resultado.getString("fechaNacimiento");
				String ciclo = resultado.getString("ciclo");
				String curso = resultado.getString("curso");
				String nombreGrupo = resultado.getString("nombreGrupo");

				// Escribir cada fila de datos
				writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s", nia, nombre, apellidos, genero, fechaNacimiento,
						ciclo, curso, nombreGrupo));
				writer.newLine();
			}

			System.out.println("Datos de los alumnos guardados correctamente en el fichero 'alumnos.txt'.");
			loggerGeneral.info("Los datos de los alumnos se guardaron correctamente en el fichero '{}'.",
					nombreFichero);

		} catch (SQLException e) {
			loggerExcepciones.error("Error al ejecutar la consulta SQL: {}", e.getMessage(), e);
			System.out.println("Se produjo un error al recuperar los datos. Revisa los logs para más detalles.");
		} catch (IOException e) {
			loggerExcepciones.error("Error al escribir en el fichero '{}': {}", nombreFichero, e.getMessage(), e);
			System.out.println("Se produjo un error al escribir en el fichero. Revisa los logs para más detalles.");
		}
	}

	/**
	 * Lee los alumnos desde el fichero de texto 'alumnos.txt' y los inserta en la
	 * base de datos. El formato del fichero debe ser:
	 * NIA,Nombre,Apellidos,Género,Fecha Nacimiento,Ciclo,Curso,Nombre del Grupo
	 *
	 * @param conexionBD Conexión a la base de datos MySQL.
	 * @return true si todos los alumnos fueron insertados correctamente, false si
	 *         ocurrió algún error.
	 */
	@Override
	public boolean leerAlumnosDeFicheroTexto(Connection conexionBD) {
		String fichero = "alumnos.txt";
		int lineasInsertadas = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(fichero))) {
			String linea;

			// Ignorar la primera línea (cabecera)
			br.readLine();

			while ((linea = br.readLine()) != null) {
				loggerGeneral.info("Leyendo línea: {}", linea);

				// Separamos los campos por coma
				String[] datos = linea.split(",");

				// Verificamos que la línea tenga 8 campos
				if (datos.length == 8) {
					try {
						String nombre = datos[1]; // Nombre
						String apellidos = datos[2]; // Apellidos
						char genero = datos[3].charAt(0); // Género (primer carácter)
						String fechaNacimiento = datos[4]; // Fecha Nacimiento
						String ciclo = datos[5]; // Ciclo
						String curso = datos[6]; // Curso
						String grupo = datos[7]; // Nombre del Grupo

						// Conversión de la fecha
						SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
						Date fechaUtil = formatoFecha.parse(fechaNacimiento);
						loggerGeneral.info("Fecha convertida: {}", fechaUtil);

						// Obtener el número del grupo por nombre
						int numeroGrupo = obtenerNumeroGrupoPorNombre(conexionBD, grupo);

						if (numeroGrupo != -1) {
							Grupo grupoObj = new Grupo(numeroGrupo, grupo);
							Alumno alumno = new Alumno();
							alumno.setNombre(nombre);
							alumno.setApellidos(apellidos);
							alumno.setGenero(genero);
							alumno.setFechaNacimiento(fechaUtil);
							alumno.setCiclo(ciclo);
							alumno.setCurso(curso);
							alumno.setGrupo(grupoObj);

							// Insertar el alumno en la base de datos
							insertarAlumno(conexionBD, alumno);
							lineasInsertadas++;
							loggerGeneral.info("Alumno insertado: {} {}", nombre, apellidos);
						} else {
							loggerGeneral.warn("El grupo '{}' no existe en la base de datos. Alumno ignorado.", grupo);
						}
					} catch (ParseException e) {
						loggerExcepciones.error("Error al convertir la fecha: {}", datos[4], e);
						System.out.println("Error al convertir la fecha: " + datos[4]);
					}
				} else {
					loggerGeneral.warn("Línea inválida en el fichero (número de campos incorrecto): {}", linea);
				}
			}

			if (lineasInsertadas > 0) {
				System.out.println("Alumnos leídos e insertados correctamente desde el fichero 'alumnos.txt'.");
				loggerGeneral.info("Alumnos leídos e insertados correctamente.");
				return true; // Todos los alumnos fueron insertados correctamente
			} else {
				System.out.println("No se insertaron alumnos.");
				loggerGeneral.info("No se insertaron alumnos.");
				return false;
			}
		} catch (IOException e) {
			loggerExcepciones.error("Ocurrió un error al leer el archivo '{}': {}", fichero, e.getMessage(), e);
			System.out.println("Ocurrió un error al leer el archivo: " + e.getMessage());
			return false;
		} catch (SQLException e) {
			loggerExcepciones.error("Error en la base de datos al insertar alumno: {}", e.getMessage(), e);
			System.out.println("Error en la base de datos al insertar alumno.");
			return false;
		}
	}

	/**
	 * Obtiene el número del grupo a partir del nombre del grupo.
	 *
	 * @param conexionBD  Conexión a la base de datos.
	 * @param nombreGrupo Nombre del grupo.
	 * @return El número del grupo, o -1 si no se encuentra el grupo.
	 */
	private int obtenerNumeroGrupoPorNombre(Connection conexionBD, String nombreGrupo) {
		String sql = "SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?";
		try (PreparedStatement stmt = conexionBD.prepareStatement(sql)) {
			stmt.setString(1, nombreGrupo);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					int numeroGrupo = rs.getInt("numeroGrupo");
					loggerGeneral.info("Grupo encontrado: {}", numeroGrupo);
					return numeroGrupo;
				} else {
					loggerGeneral.warn("No se encontró el grupo: {}", nombreGrupo);
					return -1; // Si no se encuentra el grupo
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al obtener el número de grupo: {}", e.getMessage(), e);
			System.out.println("Error al obtener el número de grupo: " + e.getMessage());
			return -1; // Devolver -1 en caso de error
		}
	}

	// TAREA 16:

	/**
	 * Ejecuta una operación genérica en la base de datos basada en un NIA.
	 * 
	 * @param conexionBD          La conexión a la base de datos.
	 * @param sql                 La consulta SQL a ejecutar.
	 * @param configuracionParams Función para configurar los parámetros del
	 *                            PreparedStatement.
	 * @return true si la operación afecta filas en la base de datos, false en caso
	 *         contrario.
	 */
	public boolean ejecutarOperacionConNIA(Connection conexionBD, String sql,
			Consumer<PreparedStatement> configuracionParams) {
		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
			// Configurar los parámetros usando la función pasada como argumento
			configuracionParams.accept(sentencia);

			// Ejecutar la consulta
			int filasAfectadas = sentencia.executeUpdate();

			if (filasAfectadas > 0) {
				loggerGeneral.info("Operación ejecutada correctamente. SQL: {}", sql);
				return true;
			} else {
				loggerGeneral.warn("Operación no afectó filas. SQL: {}", sql);
				return false;
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al ejecutar la operación SQL '{}': {}", sql, e.getMessage(), e);
			System.out.println("Error en la operación: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Modifica el nombre de un alumno en la base de datos basado en su NIA.
	 * 
	 * @param conexion    conexión a la base de datos.
	 * @param nia         NIA del alumno.
	 * @param nuevoNombre nuevo nombre del alumno.
	 * @return true si la modificación fue exitosa; false en caso contrario.
	 */
	@Override
	public boolean modificarNombreAlumnoPorNIA(Connection conexion, int nia, String nuevoNombre) {
		String sql = "UPDATE alumnos SET nombre = ? WHERE nia = ?";

		return ejecutarOperacionConNIA(conexion, sql, sentencia -> {
			try {
				sentencia.setString(1, nuevoNombre);
				sentencia.setInt(2, nia);
			} catch (SQLException e) {
				throw new RuntimeException("Error al configurar los parámetros", e);
			}
		});
	}

	/**
	 * Elimina un alumno de la base de datos a partir de su NIA.
	 * 
	 * @param conexionBD la conexión a la base de datos.
	 * @param nia        el NIA del alumno a eliminar.
	 * @return true si el alumno fue eliminado correctamente, false en caso
	 *         contrario.
	 */
	@Override
	public boolean eliminarAlumnoPorNIA(Connection conexionBD, int nia) {
		String sql = "DELETE FROM alumnos WHERE nia = ?";

		return ejecutarOperacionConNIA(conexionBD, sql, sentencia -> {
			try {
				sentencia.setInt(1, nia);
			} catch (SQLException e) {
				throw new RuntimeException("Error al configurar los parámetros", e);
			}
		});
	}

	/**
	 * Muestra toda la información de un alumno basándose en su NIA.
	 * 
	 * @param conexionBD La conexión a la base de datos.
	 * @param nia        El NIA del alumno a mostrar.
	 * @return true si el alumno fue encontrado y mostrado; false en caso contrario.
	 */
	@Override
	public boolean mostrarAlumnoPorNIA(Connection conexionBD, int nia) {
		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero, a.fechaNacimiento,
				           a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    LEFT JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    WHERE a.nia = ?
				""";

		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
			sentencia.setInt(1, nia);

			try (ResultSet resultado = sentencia.executeQuery()) {
				if (!resultado.isBeforeFirst()) {
					System.out.println("No se encontró un alumno con el NIA proporcionado.");
					loggerGeneral.warn("No se encontró un alumno con NIA {}.", nia);
					return false;
				}

				// Mostrar datos del alumno
				while (resultado.next()) {
					System.out.printf("""
							NIA: %d
							Nombre: %s
							Apellidos: %s
							Género: %s
							Fecha de nacimiento: %s
							Ciclo: %s
							Curso: %s
							Grupo: %s
							-------------------------
							""", resultado.getInt("nia"), resultado.getString("nombre"),
							resultado.getString("apellidos"), resultado.getString("genero"),
							resultado.getDate("fechaNacimiento"), resultado.getString("ciclo"),
							resultado.getString("curso"), resultado.getString("nombreGrupo"));
				}
				loggerGeneral.info("Información del alumno con NIA {} mostrada correctamente.", nia);
				return true;
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al consultar información del alumno con NIA {}: {}", nia, e.getMessage(), e);
			System.out.println("Se produjo un error al mostrar la información del alumno. Revisa los logs.");
			return false;
		}
	}

	@Override
	public boolean eliminarAlumnosPorApellidos(Connection conexionBD, String apellidos) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarAlumnosEnFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean leerAlumnosDeFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Inserta un nuevo grupo en la base de datos.
	 *
	 * @param conexionBD La conexión a la base de datos.
	 * @param grupo      El objeto Grupo que se desea insertar.
	 * @return true si la inserción fue exitosa, false en caso contrario.
	 */
	@Override
	public boolean insertarGrupo(Connection conexionBD, Grupo grupo) {
		String sql = "INSERT INTO grupos (nombreGrupo) VALUES (?)";

		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
			// Convertir el nombre del grupo a mayúsculas antes de insertar
			String nombreGrupo = grupo.getNombreGrupo().toUpperCase();
			sentencia.setString(1, nombreGrupo);

			int filasAfectadas = sentencia.executeUpdate();

			if (filasAfectadas > 0) {
				loggerGeneral.info("Grupo '{}' insertado exitosamente", nombreGrupo);
				return true;
			} else {
				loggerGeneral.warn("No se pudo insertar el grupo '{}'", nombreGrupo);
				return false;
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al insertar el grupo '{}': {}", grupo.getNombreGrupo(), e.getMessage(), e);
			System.out.println("Error al insertar el grupo: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Elimina a todos los alumnos de un grupo específico.
	 * 
	 * @param conexionBD  la conexión activa a la base de datos.
	 * @param nombreGrupo el nombre del grupo cuyos alumnos serán eliminados.
	 * @return true si se eliminaron correctamente, false si ocurrió un error.
	 */
	@Override
	public boolean eliminarAlumnosPorGrupo(Connection conexionBD, String nombreGrupo) {
		// Consulta para verificar si el grupo tiene alumnos antes de intentar
		// eliminarlos
		String comprobarAlumnosSql = "SELECT COUNT(*) FROM alumnos WHERE numeroGrupo = (SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?)";

		try (PreparedStatement comprobarAlumnosSentencia = conexionBD.prepareStatement(comprobarAlumnosSql)) {
			comprobarAlumnosSentencia.setString(1, nombreGrupo);

			ResultSet resultado = comprobarAlumnosSentencia.executeQuery();
			if (resultado.next()) {
				int cantidadAlumnos = resultado.getInt(1);

				if (cantidadAlumnos == 0) {
					loggerGeneral.info("No se encontraron alumnos en el grupo '{}'", nombreGrupo);
					return false; // Si no hay alumnos, no eliminamos nada
				}
			}

			// Procedemos a eliminar los alumnos si existen
			String sql = "DELETE FROM alumnos WHERE numeroGrupo = (SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?)";
			try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
				sentencia.setString(1, nombreGrupo);

				int filasAfectadas = sentencia.executeUpdate();
				if (filasAfectadas > 0) {
					loggerGeneral.info("Alumnos del grupo '{}' eliminados exitosamente", nombreGrupo);
					return true;
				} else {
					loggerGeneral.warn("No se eliminaron alumnos del grupo '{}'", nombreGrupo);
					return false;
				}
			} catch (SQLException e) {
				loggerExcepciones.error("Error al eliminar alumnos del grupo '{}': {}", nombreGrupo, e.getMessage(), e);
				System.out.println("Error al eliminar alumnos del grupo: " + e.getMessage());
				return false;
			}

		} catch (SQLException e) {
			loggerExcepciones.error("Error al verificar si el grupo '{}' tiene alumnos: {}", nombreGrupo,
					e.getMessage(), e);
			System.out.println("Error al verificar si el grupo tiene alumnos: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void guardarGruposEnFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean leerGruposDeFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Muestra todos los grupos disponibles en la base de datos.
	 * 
	 * @param conexionBD la conexión activa a la base de datos.
	 * @return true si se muestran los grupos correctamente, false si no hay grupos
	 *         o hay un error.
	 */
	public static boolean mostrarTodosLosGrupos(Connection conexionBD) {
		String sql = "SELECT nombreGrupo FROM grupos";
		try (Statement sentencia = conexionBD.createStatement(); ResultSet resultado = sentencia.executeQuery(sql)) {
			boolean hayGrupos = false;
			while (resultado.next()) {
				hayGrupos = true;
				System.out.println("- " + resultado.getString("nombreGrupo"));
			}
			if (hayGrupos) {
				loggerGeneral.info("Grupos mostrados exitosamente desde la base de datos.");
				return true;
			} else {
				loggerGeneral.warn("No se encontraron grupos en la base de datos.");
				return false;
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al mostrar los grupos: {}", e.getMessage(), e);
			System.out.println("Error al mostrar los grupos: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Guarda todos los grupos y sus alumnos en un archivo XML llamado 'grupos.xml'.
	 * Si el archivo ya existe, solicita confirmación al usuario antes de
	 * sobrescribirlo.
	 * 
	 * @param conexionBD La conexión activa a la base de datos MySQL.
	 * @return true si el archivo se guarda correctamente, false si ocurre un error.
	 */
	public static boolean guardarGruposEnXML(Connection conexionBD) {
		String nombreArchivo = "grupos.xml";

		File archivoXML = new File(nombreArchivo);
		if (archivoXML.exists()) {
			System.out.print("El archivo " + nombreArchivo + " ya existe. ¿Deseas sobrescribirlo? (S/N): ");
			String respuesta = sc.nextLine();

			if (!respuesta.equalsIgnoreCase("s")) {
				loggerGeneral.warn("No se ha sobrescrito el archivo XML porque el usuario no lo permitió.");
				System.out.println("El archivo no se ha sobrescrito.");
				return false;
			}
		}

		DocumentBuilderFactory documentoFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentoBuilder = documentoFactory.newDocumentBuilder();
			Document documentoXML = documentoBuilder.newDocument();

			Element raizElement = documentoXML.createElement("grupos");
			documentoXML.appendChild(raizElement);

			String consultaGrupos = "SELECT * FROM grupos";
			try (PreparedStatement stmtGrupos = conexionBD.prepareStatement(consultaGrupos)) {
				ResultSet rsGrupos = stmtGrupos.executeQuery();

				while (rsGrupos.next()) {
					int numeroGrupo = rsGrupos.getInt("numeroGrupo");
					String nombreGrupo = rsGrupos.getString("nombreGrupo");

					Element grupoElement = documentoXML.createElement("grupo");
					grupoElement.setAttribute("numeroGrupo", String.valueOf(numeroGrupo));
					grupoElement.setAttribute("nombreGrupo", nombreGrupo);
					raizElement.appendChild(grupoElement);

					String consultaAlumnos = "SELECT * FROM alumnos WHERE numeroGrupo = ?";
					try (PreparedStatement stmtAlumnos = conexionBD.prepareStatement(consultaAlumnos)) {
						stmtAlumnos.setInt(1, numeroGrupo);
						ResultSet rsAlumnos = stmtAlumnos.executeQuery();

						while (rsAlumnos.next()) {
							String nia = rsAlumnos.getString("nia");
							String nombreAlumno = rsAlumnos.getString("nombre");
							String apellidosAlumno = rsAlumnos.getString("apellidos");
							String genero = rsAlumnos.getString("genero");
							String fechaNacimiento = rsAlumnos.getString("fechaNacimiento");
							String curso = rsAlumnos.getString("curso");

							Element alumnoElement = documentoXML.createElement("alumno");
							alumnoElement.setAttribute("nia", nia);
							alumnoElement.setAttribute("nombre", nombreAlumno);
							alumnoElement.setAttribute("apellidos", apellidosAlumno);
							alumnoElement.setAttribute("genero", genero);
							alumnoElement.setAttribute("fechaNacimiento", fechaNacimiento);
							alumnoElement.setAttribute("curso", curso);

							grupoElement.appendChild(alumnoElement);
						}
					}
				}

				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");

				DOMSource source = new DOMSource(documentoXML);
				StreamResult result = new StreamResult(new File(nombreArchivo));
				transformer.transform(source, result);

				loggerGeneral.info("El archivo XML se ha guardado correctamente en {}", nombreArchivo);
				System.out.println("El archivo XML se ha guardado correctamente.");
				return true;
			} catch (SQLException e) {
				loggerExcepciones.error("Error al consultar los grupos o los alumnos: {}", e.getMessage(), e);
				System.out.println("Error al consultar los grupos o los alumnos: " + e.getMessage());
				return false;
			}
		} catch (ParserConfigurationException e) {
			loggerExcepciones.error("Error al crear el documento XML: {}", e.getMessage(), e);
			System.out.println("Error al generar el archivo XML: " + e.getMessage());
			return false;
		} catch (TransformerException e) {
			loggerExcepciones.error("Error al transformar el documento XML: {}", e.getMessage(), e);
			System.out.println("Error al transformar el archivo XML: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Lee un archivo XML que contiene información sobre grupos y alumnos, y guarda
	 * los datos en las tablas correspondientes de la base de datos.
	 *
	 * @param rutaArchivo Ruta del archivo XML a procesar.
	 * @param conexionBD  Conexión activa a la base de datos.
	 * @return true si los datos fueron procesados e insertados correctamente, false
	 *         en caso de error.
	 */
	public static boolean leerYGuardarGruposXML(String rutaArchivo, Connection conexionBD) {
		// Validar si el archivo XML existe en la ruta especificada
		File archivoXML = new File(rutaArchivo);
		if (!archivoXML.exists()) {
			loggerExcepciones.error("El archivo XML no existe: {}", rutaArchivo);
			System.err.println("El archivo XML no existe: " + rutaArchivo);
			return false;
		}

		try {
			DocumentBuilderFactory fabricaDocumentos = DocumentBuilderFactory.newInstance();
			DocumentBuilder constructorDocumentos = fabricaDocumentos.newDocumentBuilder();
			Document documentoXML = constructorDocumentos.parse(archivoXML);
			documentoXML.getDocumentElement().normalize();

			NodeList listaGrupos = documentoXML.getElementsByTagName("grupo");

			// Consulta SQL para insertar grupos en la tabla 'grupos'
			String sqlInsertarGrupo = "INSERT INTO grupos (nombreGrupo) VALUES (?)";
			PreparedStatement consultaInsertarGrupo = conexionBD.prepareStatement(sqlInsertarGrupo,
					Statement.RETURN_GENERATED_KEYS);

			// Consulta SQL para insertar alumnos en la tabla 'alumnos'
			String sqlInsertarAlumno = "INSERT INTO alumnos (nombre, apellidos, genero, fechaNacimiento, curso, numeroGrupo) "
					+ "VALUES (?, ?, ?, ?, ?, ?)";
			PreparedStatement consultaInsertarAlumno = conexionBD.prepareStatement(sqlInsertarAlumno);

			for (int i = 0; i < listaGrupos.getLength(); i++) {
				Node nodoGrupo = listaGrupos.item(i);

				if (nodoGrupo.getNodeType() == Node.ELEMENT_NODE) {
					Element elementoGrupo = (Element) nodoGrupo;

					String nombreGrupo = elementoGrupo.getAttribute("nombreGrupo");
					if (nombreGrupo != null && !nombreGrupo.trim().isEmpty()) {
						// Insertar el grupo en la base de datos
						consultaInsertarGrupo.setString(1, nombreGrupo);
						consultaInsertarGrupo.executeUpdate();

						ResultSet clavesGeneradas = consultaInsertarGrupo.getGeneratedKeys();
						int numeroGrupo = 0;
						if (clavesGeneradas.next()) {
							numeroGrupo = clavesGeneradas.getInt(1);
						}

						NodeList listaAlumnos = elementoGrupo.getElementsByTagName("alumno");
						for (int j = 0; j < listaAlumnos.getLength(); j++) {
							Node nodoAlumno = listaAlumnos.item(j);

							if (nodoAlumno.getNodeType() == Node.ELEMENT_NODE) {
								Element elementoAlumno = (Element) nodoAlumno;

								String nombreAlumno = elementoAlumno.getAttribute("nombre");
								String apellidosAlumno = elementoAlumno.getAttribute("apellidos");
								String generoAlumno = elementoAlumno.getAttribute("genero");
								String fechaNacimientoAlumno = elementoAlumno.getAttribute("fechaNacimiento");
								String cursoAlumno = elementoAlumno.getAttribute("curso");

								consultaInsertarAlumno.setString(1, nombreAlumno);
								consultaInsertarAlumno.setString(2, apellidosAlumno);
								consultaInsertarAlumno.setString(3, generoAlumno);
								consultaInsertarAlumno.setDate(4, java.sql.Date.valueOf(fechaNacimientoAlumno));
								consultaInsertarAlumno.setString(5, cursoAlumno);
								consultaInsertarAlumno.setInt(6, numeroGrupo);

								consultaInsertarAlumno.executeUpdate();

								loggerGeneral.info("Alumno insertado: {} {}", nombreAlumno, apellidosAlumno);
							}
						}
					} else {
						loggerExcepciones.warn("Advertencia: Nombre del grupo vacío en el XML.");
					}
				}
			}

			loggerGeneral.info("Datos cargados correctamente desde el archivo XML.");
			System.out.println("Datos cargados correctamente desde el archivo XML.");
			return true;
		} catch (Exception e) {
			loggerExcepciones.error("Error al procesar el archivo XML o insertar los datos: {}", e.getMessage(), e);
			System.err.println("Error al procesar el archivo XML o insertar los datos: " + e.getMessage());
			return false;
		}
	}

	// Tarea 16:

	/**
	 * Muestra todos los alumnos del grupo seleccionado por el usuario.
	 * 
	 * @param conexionBD La conexión activa a la base de datos.
	 */
	public void mostrarAlumnosPorGrupo(Connection conexionBD) {
		// Mostrar todos los grupos
		if (!mostrarTodosLosGrupos(conexionBD)) {
			System.out.println("No hay grupos disponibles para mostrar.");
			return;
		}

		System.out.println("Introduce el nombre del grupo del que quieres ver los alumnos:");
		String nombreGrupo = sc.nextLine();

		// Obtener el número del grupo
		int numeroGrupo = obtenerNumeroGrupoPorNombre(conexionBD, nombreGrupo);
		if (numeroGrupo == -1) {
			System.out.println("El grupo especificado no existe. Inténtalo de nuevo.");
			return;
		}

		// Consulta para obtener alumnos del grupo
		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero, a.fechaNacimiento,
				           a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    WHERE g.numeroGrupo = ?
				    ORDER BY a.nia
				""";

		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
			sentencia.setInt(1, numeroGrupo);
			try (ResultSet resultado = sentencia.executeQuery()) {
				if (!resultado.isBeforeFirst()) {
					System.out.println("No hay alumnos registrados en este grupo.");
					return;
				}

				System.out.println("Alumnos del grupo '" + nombreGrupo + "':");
				while (resultado.next()) {
					int nia = resultado.getInt("nia");
					String nombre = resultado.getString("nombre");
					String apellidos = resultado.getString("apellidos");
					String genero = resultado.getString("genero");
					Date fechaNacimiento = resultado.getDate("fechaNacimiento");
					String ciclo = resultado.getString("ciclo");
					String curso = resultado.getString("curso");
					String grupo = resultado.getString("nombreGrupo");

					// Mostrar los datos del alumno
					System.out.printf("""
							NIA: %d
							Nombre: %s
							Apellidos: %s
							Género: %s
							Fecha de nacimiento: %s
							Ciclo: %s
							Curso: %s
							Grupo: %s
							-------------------------\n
							""", nia, nombre, apellidos, genero, fechaNacimiento, ciclo, curso, grupo);
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al mostrar los alumnos del grupo '{}': {}", nombreGrupo, e.getMessage(), e);
			System.out.println("Se produjo un error al intentar mostrar los alumnos. Revisa los logs.");
		}
	}

	/**
	 * Cambia el grupo de un alumno seleccionado por el usuario.
	 * 
	 * @param conexionBD la conexión activa a la base de datos.
	 * @return true si el cambio se realizó correctamente, false en caso de error.
	 */
	public boolean cambiarGrupoAlumno(Connection conexionBD) {

		// Mostrar alumnos para que el usuario elija
		System.out.println("Lista de alumnos disponibles para cambiar de grupo:");
		if (!mostrarTodosLosAlumnos(conexionBD, false)) {
			System.out.println("No hay alumnos disponibles o ocurrió un error.");
			return false;
		}

		// Solicitar NIA del alumno
		System.out.println("\nIntroduce el NIA del alumno al que deseas cambiar de grupo:");
		int niaSeleccionado;
		while (true) {
			try {
				niaSeleccionado = Integer.parseInt(sc.nextLine().trim());
				break;
			} catch (NumberFormatException e) {
				System.out.println("El NIA debe ser un número. Inténtalo de nuevo:");
			}
		}

		// Mostrar grupos disponibles
		System.out.println("\nLista de grupos disponibles:");
		if (!mostrarTodosLosGrupos(conexionBD)) {
			System.out.println("No hay grupos disponibles o ocurrió un error.");
			return false;
		}

		// Solicitar el nuevo grupo
		System.out.println("\nIntroduce el nombre del grupo al que deseas cambiar al alumno:");
		String nuevoGrupo = sc.nextLine().trim();

		// Realizar la actualización en la base de datos
		String sql = """
				    UPDATE alumnos
				    SET numeroGrupo = (SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?)
				    WHERE nia = ?
				""";

		try (PreparedStatement sentencia = conexionBD.prepareStatement(sql)) {
			sentencia.setString(1, nuevoGrupo);
			sentencia.setInt(2, niaSeleccionado);

			int filasAfectadas = sentencia.executeUpdate();
			if (filasAfectadas > 0) {
				System.out.println("El grupo del alumno ha sido cambiado exitosamente.");
				loggerGeneral.info("Grupo del alumno con NIA {} cambiado al grupo '{}'.", niaSeleccionado, nuevoGrupo);
				return true;
			} else {
				System.out.println(
						"No se pudo cambiar el grupo del alumno. Verifica que el NIA y el grupo sean correctos.");
				loggerGeneral.warn("No se encontró el alumno con NIA {} o el grupo '{}'.", niaSeleccionado, nuevoGrupo);
				return false;
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al cambiar el grupo del alumno: {}", e.getMessage(), e);
			System.out.println("Se produjo un error al cambiar el grupo. Revisa los logs para más detalles.");
			return false;
		}
	}

	/**
	 * Guarda un grupo específico con toda su información (incluyendo los alumnos)
	 * en un archivo XML. Solicita al usuario el nombre del grupo.
	 * 
	 * @param conexionBD La conexión activa a la base de datos MySQL.
	 * @return true si el archivo se guarda correctamente, false si ocurre un error.
	 */
	public boolean guardarGrupoEspecificoEnXML(Connection conexionBD) {
	    System.out.print("Introduce el nombre del grupo que deseas guardar en fichero XML: ");
	    String nombreGrupo = sc.nextLine().toUpperCase();

	    // Validar si el grupo existe
	    if (!validarNombreGrupo(nombreGrupo)) {
	        System.out.println("El grupo '" + nombreGrupo + "' no existe en la base de datos.");
	        loggerGeneral.warn("El grupo '{}' no existe en la base de datos.", nombreGrupo);
	        return false;
	    }

	    // Obtener el número del grupo a partir del nombre
	    int numeroGrupo = obtenerNumeroGrupoPorNombre(conexionBD, nombreGrupo);
	    if (numeroGrupo == -1) {
	        System.out.println("Error al obtener el número del grupo para: " + nombreGrupo);
	        loggerGeneral.error("No se pudo obtener el número del grupo para '{}'.", nombreGrupo);
	        return false;
	    }

	    String nombreArchivo = "grupo_" + nombreGrupo + ".xml";

	    File archivoXML = new File(nombreArchivo);
	    if (archivoXML.exists()) {
	        System.out.print("El archivo " + nombreArchivo + " ya existe. ¿Deseas sobrescribirlo? (S/N): ");
	        String respuesta = sc.nextLine();

	        if (!respuesta.equalsIgnoreCase("s")) {
	            loggerGeneral.warn("No se ha sobrescrito el archivo XML porque el usuario no lo permitió.");
	            System.out.println("El archivo no se ha sobrescrito.");
	            return false;
	        }
	    }

	    DocumentBuilderFactory documentoFactory = DocumentBuilderFactory.newInstance();
	    try {
	        DocumentBuilder documentoBuilder = documentoFactory.newDocumentBuilder();
	        Document documentoXML = documentoBuilder.newDocument();

	        Element raizElement = documentoXML.createElement("grupo");
	        documentoXML.appendChild(raizElement);

	        String consultaGrupo = "SELECT * FROM grupos WHERE numeroGrupo = ?";
	        try (PreparedStatement stmtGrupo = conexionBD.prepareStatement(consultaGrupo)) {
	            stmtGrupo.setInt(1, numeroGrupo);
	            ResultSet rsGrupo = stmtGrupo.executeQuery();

	            if (rsGrupo.next()) {
	                String nombreGrupoBD = rsGrupo.getString("nombreGrupo");

	                raizElement.setAttribute("numeroGrupo", String.valueOf(numeroGrupo));
	                raizElement.setAttribute("nombreGrupo", nombreGrupoBD);

	                // Consultar los alumnos del grupo
	                String consultaAlumnos = "SELECT * FROM alumnos WHERE numeroGrupo = ?";
	                try (PreparedStatement stmtAlumnos = conexionBD.prepareStatement(consultaAlumnos)) {
	                    stmtAlumnos.setInt(1, numeroGrupo);
	                    ResultSet rsAlumnos = stmtAlumnos.executeQuery();

	                    while (rsAlumnos.next()) {
	                        String nia = rsAlumnos.getString("nia");
	                        String nombreAlumno = rsAlumnos.getString("nombre");
	                        String apellidosAlumno = rsAlumnos.getString("apellidos");
	                        String genero = rsAlumnos.getString("genero");
	                        String fechaNacimiento = rsAlumnos.getString("fechaNacimiento");
	                        String curso = rsAlumnos.getString("curso");

	                        Element alumnoElement = documentoXML.createElement("alumno");
	                        alumnoElement.setAttribute("nia", nia);
	                        alumnoElement.setAttribute("nombre", nombreAlumno);
	                        alumnoElement.setAttribute("apellidos", apellidosAlumno);
	                        alumnoElement.setAttribute("genero", genero);
	                        alumnoElement.setAttribute("fechaNacimiento", fechaNacimiento);
	                        alumnoElement.setAttribute("curso", curso);

	                        raizElement.appendChild(alumnoElement);
	                    }
	                }
	            } else {
	                System.out.println("No se encontró el grupo con el número " + numeroGrupo + ".");
	                loggerGeneral.warn("El grupo con número {} no existe en la base de datos.", numeroGrupo);
	                return false;
	            }

	            // Transformar el documento XML a archivo
	            TransformerFactory transformerFactory = TransformerFactory.newInstance();
	            Transformer transformer = transformerFactory.newTransformer();
	            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	            DOMSource source = new DOMSource(documentoXML);
	            StreamResult result = new StreamResult(new File(nombreArchivo));
	            transformer.transform(source, result);

	            loggerGeneral.info("El archivo XML del grupo {} se ha guardado correctamente en {}", numeroGrupo,
	                    nombreArchivo);
	            System.out.println("El archivo XML del grupo " + nombreGrupo + " se ha guardado correctamente.");
	            return true;
	        } catch (SQLException e) {
	            loggerExcepciones.error("Error al consultar el grupo o los alumnos: {}", e.getMessage(), e);
	            System.out.println("Error al consultar el grupo o los alumnos: " + e.getMessage());
	            return false;
	        }
	    } catch (ParserConfigurationException e) {
	        loggerExcepciones.error("Error al crear el documento XML: {}", e.getMessage(), e);
	        System.out.println("Error al generar el archivo XML: " + e.getMessage());
	        return false;
	    } catch (TransformerException e) {
	        loggerExcepciones.error("Error al transformar el documento XML: {}", e.getMessage(), e);
	        System.out.println("Error al transformar el archivo XML: " + e.getMessage());
	        return false;
	    }
	}

}
