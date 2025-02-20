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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.xml.sax.SAXException;

import pool.PoolConexiones;

public class AlumnosBD implements AlumnosDAO {

	private final static Scanner sc = new Scanner(System.in);

	private static final Logger loggerGeneral = LogManager.getRootLogger();
	private static final Logger loggerExcepciones = LogManager.getLogger("exceptions");

	@Override
	public boolean insertarAlumno(Alumno alumno) {
		// Obtener el numeroGrupo del grupo del alumno
		int numeroGrupo = obtenerNumeroGrupo(alumno.getGrupo().getNombreGrupo());

		if (numeroGrupo == -1) {
			loggerExcepciones.error("Error: El grupo '{}' no existe en la base de datos.",
					alumno.getGrupo().getNombreGrupo());
			return false;
		}

		String sql = "INSERT INTO alumnos (nombre, apellidos, genero, fechaNacimiento, ciclo, curso, numeroGrupo) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

			sentencia.setString(1, alumno.getNombre());
			sentencia.setString(2, alumno.getApellidos());
			sentencia.setString(3, String.valueOf(alumno.getGenero())); // Convertir el char a String
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
			return false;
		}
	}

	/**
	 * Solicita al usuario los datos necesarios para crear un objeto Alumno.
	 * 
	 * @return Un objeto Alumno con los datos ingresados por el usuario.
	 */
	@Override
	public Alumno solicitarDatosAlumno() {
		try {
			System.out.println("Introduce el nombre del alumno:");
			String nombre = sc.nextLine().trim().toUpperCase();

			System.out.println("Introduce los apellidos del alumno:");
			String apellidos = sc.nextLine().trim().toUpperCase();

			// Validar género
			char respuestaGenero = solicitarGenero();

			// Validar fecha de nacimiento
			Date fechaNacimiento = solicitarFechaNacimiento();

			System.out.println("Introduce el ciclo del alumno:");
			String ciclo = sc.nextLine().trim().toUpperCase();

			System.out.println("Introduce el curso del alumno:");
			String curso = sc.nextLine().trim().toUpperCase();

			// Validar nombre del grupo
			String nombreGrupo = solicitarNombreGrupo();

			// Crear el objeto Grupo
			Grupo grupo = new Grupo(nombreGrupo);

			// Crear y devolver el objeto Alumno
			Alumno alumno = new Alumno(nombre, apellidos, respuestaGenero, fechaNacimiento, ciclo, curso, grupo);
			loggerGeneral.info("Datos del alumno solicitados correctamente: {}", alumno);
			return alumno;

		} catch (Exception e) {
			loggerExcepciones.error("Error inesperado al solicitar datos del alumno: {}", e.getMessage(), e);
			return null;
		}
	}

	private char solicitarGenero() {
		char genero;
		do {
			System.out.println("Introduce el género del alumno (M/F):");
			String input = sc.nextLine().trim().toUpperCase();
			if (input.length() == 1 && (input.charAt(0) == 'M' || input.charAt(0) == 'F')) {
				genero = input.charAt(0);
				break;
			} else {
				loggerGeneral.info("Entrada no válida para género: {}", input);
				System.out.println("Respuesta no válida. Introduce 'M' o 'F'.");
			}
		} while (true);
		return genero;
	}

	private Date solicitarFechaNacimiento() {
		Date fecha = null;
		SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
		formatoFecha.setLenient(false); // Validación estricta

		do {
			System.out.println("Introduce la fecha de nacimiento (dd-MM-aaaa):");
			String fechaInput = sc.nextLine().trim();
			try {
				fecha = formatoFecha.parse(fechaInput);
			} catch (ParseException e) {
				loggerExcepciones.error("Formato de fecha inválido ingresado: {}", fechaInput);
				System.out.println("Formato de fecha inválido. Intenta de nuevo.");
			}
		} while (fecha == null);

		return fecha;
	}

	private String solicitarNombreGrupo() {
		String nombreGrupo;
		do {
			System.out.println("Introduce el nombre del grupo del alumno:");
			nombreGrupo = sc.nextLine().trim().toUpperCase();
			if (!validarNombreGrupo(nombreGrupo)) {
				loggerGeneral.info("Entrada no válida para grupo: {}", nombreGrupo);
				System.out.println("El nombre del grupo no es válido. Intenta de nuevo.");
			}
		} while (!validarNombreGrupo(nombreGrupo));
		return nombreGrupo;
	}

	/**
	 * Recupera el número del grupo a partir de su nombre.
	 * 
	 * @param nombreGrupo El nombre del grupo.
	 * @return El numeroGrupo correspondiente o -1 si no existe.
	 */
	private int obtenerNumeroGrupo(String nombreGrupo) {
		String sql = "SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

			sentencia.setString(1, nombreGrupo);

			try (ResultSet resultado = sentencia.executeQuery()) {
				if (resultado.next()) {
					return resultado.getInt("numeroGrupo");
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al obtener numeroGrupo para el grupo '{}': {}", nombreGrupo, e.getMessage());
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
		String sql = "SELECT 1 FROM grupos WHERE nombreGrupo = ?";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

			sentencia.setString(1, nombreGrupo);

			try (ResultSet resultado = sentencia.executeQuery()) {
				return resultado.next(); // Retorna true si el grupo existe, false si no
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al validar el grupo '{}': {}", nombreGrupo, e.getMessage());
			return false;
		}
	}

	/**
	 * Muestra todos los alumnos registrados.
	 * 
	 * @param mostrarTodaLaInformacion Indica si se debe mostrar toda la información
	 *                                 (true) o solo NIA y nombre (false).
	 * @return true si se muestra la lista correctamente, false en caso contrario.
	 */
	@Override
	public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion) {
		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero, a.fechaNacimiento,
				           a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    LEFT JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    ORDER BY a.nia
				""";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql);
				ResultSet resultado = sentencia.executeQuery()) {

			if (!resultado.isBeforeFirst()) {
				System.out.println("No hay alumnos registrados.");
				return false;
			}

			if (mostrarTodaLaInformacion) {
				System.out.println("Lista completa de alumnos registrados:");
			} else {
				System.out.println("Lista de alumnos (NIA y Nombre):");
			}

			List<Integer> listaNias = new ArrayList<>();

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
					listaNias.add(nia);
				}
			}

			// Si estamos en modo "NIA y nombre", permitir al usuario seleccionar un NIA
			if (!mostrarTodaLaInformacion) {
				System.out.println("\nIntroduce el NIA del alumno que deseas visualizar (o 0 para salir):");
				while (true) {
					try {
						int niaSeleccionado = Integer.parseInt(sc.nextLine().trim());

						if (niaSeleccionado == 0) {
							System.out.println("Saliendo sin seleccionar un alumno.");
							return true;
						}

						if (listaNias.contains(niaSeleccionado)) {
							return mostrarAlumnoPorNIA(niaSeleccionado);
						} else {
							System.out.println("El NIA seleccionado no está en la lista. Inténtalo de nuevo.");
						}
					} catch (NumberFormatException e) {
						System.out.println("El NIA debe ser un número válido. Inténtalo de nuevo:");
					}
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
	 */
	@Override
	public void guardarAlumnosEnFicheroTexto() {
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
				    LEFT JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    ORDER BY a.nia ASC
				""";

		// Intentar escribir en el fichero
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichero));
				Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql);
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
				writer.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s", resultado.getInt("nia"),
						resultado.getString("nombre"), resultado.getString("apellidos"), resultado.getString("genero"),
						resultado.getString("fechaNacimiento"), resultado.getString("ciclo"),
						resultado.getString("curso"),
						resultado.getString("nombreGrupo") == null ? "Sin grupo" : resultado.getString("nombreGrupo")));
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
	 * @return true si todos los alumnos fueron insertados correctamente, false si
	 *         ocurrió algún error.
	 */
	@Override
	public boolean leerAlumnosDeFicheroTexto() {
		String fichero = "alumnos.txt";
		int lineasInsertadas = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(fichero))) {
			String linea;

			// Ignorar la primera línea (cabecera)
			br.readLine();

			while ((linea = br.readLine()) != null) {
				loggerGeneral.info("Leyendo línea: {}", linea);

				// Separar los campos por coma
				String[] datos = linea.split(",");

				// Verificar que la línea tenga 8 campos
				if (datos.length == 8) {
					try {
						String nombre = datos[1];
						String apellidos = datos[2];
						char genero = datos[3].charAt(0);
						String fechaNacimiento = datos[4];
						String ciclo = datos[5];
						String curso = datos[6];
						String grupo = datos[7];

						// Convertir la fecha
						SimpleDateFormat formatoFecha = new SimpleDateFormat("dd-MM-yyyy");
						Date fechaUtil = formatoFecha.parse(fechaNacimiento);
						loggerGeneral.info("Fecha convertida: {}", fechaUtil);

						// Obtener el número del grupo
						int numeroGrupo = obtenerNumeroGrupo(grupo);

						if (numeroGrupo != -1) {
							Grupo grupoObj = new Grupo(numeroGrupo, grupo);
							Alumno alumno = new Alumno(nombre, apellidos, genero, fechaUtil, ciclo, curso, grupoObj);

							// Insertar el alumno en la base de datos
							if (insertarAlumno(alumno)) {
								lineasInsertadas++;
								loggerGeneral.info("Alumno insertado: {} {}", nombre, apellidos);
							} else {
								loggerGeneral.warn("No se pudo insertar el alumno: {} {}", nombre, apellidos);
							}
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
				return true;
			} else {
				System.out.println("No se insertaron alumnos.");
				loggerGeneral.info("No se insertaron alumnos.");
				return false;
			}
		} catch (IOException e) {
			loggerExcepciones.error("Ocurrió un error al leer el archivo '{}': {}", fichero, e.getMessage(), e);
			System.out.println("Ocurrió un error al leer el archivo: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Obtiene el número del grupo a partir del nombre del grupo.
	 *
	 * @param nombreGrupo Nombre del grupo.
	 * @return El número del grupo, o -1 si no se encuentra el grupo.
	 */
	private int obtenerNumeroGrupoPorNombre(String nombreGrupo) {
		String sql = "SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement stmt = conexion.prepareStatement(sql)) {

			stmt.setString(1, nombreGrupo);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("numeroGrupo");
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al obtener el número de grupo '{}': {}", nombreGrupo, e.getMessage());
		}

		return -1; // Devolver -1 si no se encuentra el grupo o hay error
	}

	/**
	 * Ejecuta una operación genérica en la base de datos basada en un NIA.
	 * 
	 * @param sql                 La consulta SQL a ejecutar.
	 * @param configuracionParams Función para configurar los parámetros del
	 *                            PreparedStatement.
	 * @return true si la operación afecta filas en la base de datos, false en caso
	 *         contrario.
	 */
	public boolean ejecutarOperacionConNIA(String sql, Consumer<PreparedStatement> configuracionParams) {
		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

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
	 * @param nia         NIA del alumno.
	 * @param nuevoNombre Nuevo nombre del alumno.
	 * @return true si la modificación fue exitosa; false en caso contrario.
	 */
	@Override
	public boolean modificarNombreAlumnoPorNIA(int nia, String nuevoNombre) {
		String sql = "UPDATE alumnos SET nombre = ? WHERE nia = ?";

		return ejecutarOperacionConNIA(sql, sentencia -> {
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
	 * @param nia el NIA del alumno a eliminar.
	 * @return true si el alumno fue eliminado correctamente, false en caso
	 *         contrario.
	 */
	@Override
	public boolean eliminarAlumnoPorNIA(int nia) {
		String sql = "DELETE FROM alumnos WHERE nia = ?";

		return ejecutarOperacionConNIA(sql, sentencia -> {
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
	 * @param nia El NIA del alumno a mostrar.
	 * @return true si el alumno fue encontrado y mostrado; false en caso contrario.
	 */
	@Override
	public boolean mostrarAlumnoPorNIA(int nia) {
		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero, a.fechaNacimiento,
				           a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    LEFT JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    WHERE a.nia = ?
				""";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

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
							resultado.getString("curso"), resultado.getString("nombreGrupo") == null ? "Sin grupo"
									: resultado.getString("nombreGrupo"));
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

	/**
	 * Inserta un nuevo grupo en la base de datos.
	 *
	 * @param grupo El objeto Grupo que se desea insertar.
	 * @return true si la inserción fue exitosa, false en caso contrario.
	 */
	@Override
	public boolean insertarGrupo(Grupo grupo) {
		String sql = "INSERT INTO grupos (nombreGrupo) VALUES (?)";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

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
	 * @param nombreGrupo el nombre del grupo cuyos alumnos serán eliminados.
	 * @return true si se eliminaron correctamente, false si ocurrió un error.
	 */
	@Override
	public boolean eliminarAlumnosPorGrupo(String nombreGrupo) {
		String comprobarAlumnosSql = "SELECT COUNT(*) FROM alumnos WHERE numeroGrupo = (SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?)";
		String eliminarAlumnosSql = "DELETE FROM alumnos WHERE numeroGrupo = (SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?)";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement comprobarAlumnosSentencia = conexion.prepareStatement(comprobarAlumnosSql)) {

			comprobarAlumnosSentencia.setString(1, nombreGrupo);

			try (ResultSet resultado = comprobarAlumnosSentencia.executeQuery()) {
				if (resultado.next() && resultado.getInt(1) == 0) {
					loggerGeneral.info("No se encontraron alumnos en el grupo '{}'", nombreGrupo);
					return false;
				}
			}

			// Procedemos a eliminar los alumnos si existen
			try (PreparedStatement sentencia = conexion.prepareStatement(eliminarAlumnosSql)) {
				sentencia.setString(1, nombreGrupo);

				int filasAfectadas = sentencia.executeUpdate();
				if (filasAfectadas > 0) {
					loggerGeneral.info("Alumnos del grupo '{}' eliminados exitosamente", nombreGrupo);
					return true;
				} else {
					loggerGeneral.warn("No se eliminaron alumnos del grupo '{}'", nombreGrupo);
					return false;
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al eliminar alumnos del grupo '{}': {}", nombreGrupo, e.getMessage(), e);
			System.out.println("Error al eliminar alumnos del grupo: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Muestra todos los grupos disponibles en la base de datos.
	 * 
	 * @return true si se muestran los grupos correctamente, false si no hay grupos
	 *         o hay un error.
	 */

	@Override
	public boolean mostrarTodosLosGrupos() {
		String sql = "SELECT nombreGrupo FROM grupos";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql);
				ResultSet resultado = sentencia.executeQuery()) {

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
	 * @return true si el archivo se guarda correctamente, false si ocurre un error.
	 */

	public boolean guardarGruposEnXML() {
		String nombreArchivo = "grupos.xml";
		File archivoXML = new File(nombreArchivo);

		if (archivoXML.exists()) {
			System.out.print("El archivo " + nombreArchivo + " ya existe. ¿Deseas sobrescribirlo? (S/N): ");
			String respuesta = sc.nextLine().trim().toUpperCase();

			if (!respuesta.equals("S")) {
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

			String consultaGrupos = "SELECT numeroGrupo, nombreGrupo FROM grupos";
			String consultaAlumnos = "SELECT nia, nombre, apellidos, genero, fechaNacimiento, ciclo, curso FROM alumnos WHERE numeroGrupo = ?";

			try (Connection conexion = PoolConexiones.getConnection();
					PreparedStatement stmtGrupos = conexion.prepareStatement(consultaGrupos);
					ResultSet rsGrupos = stmtGrupos.executeQuery()) {

				while (rsGrupos.next()) {
					int numeroGrupo = rsGrupos.getInt("numeroGrupo");
					String nombreGrupo = rsGrupos.getString("nombreGrupo");

					Element grupoElement = documentoXML.createElement("grupo");
					grupoElement.setAttribute("numeroGrupo", String.valueOf(numeroGrupo));
					grupoElement.setAttribute("nombreGrupo", nombreGrupo);
					raizElement.appendChild(grupoElement);

					try (PreparedStatement stmtAlumnos = conexion.prepareStatement(consultaAlumnos)) {
						stmtAlumnos.setInt(1, numeroGrupo);
						try (ResultSet rsAlumnos = stmtAlumnos.executeQuery()) {
							while (rsAlumnos.next()) {
								Element alumnoElement = documentoXML.createElement("alumno");
								alumnoElement.setAttribute("nia", rsAlumnos.getString("nia"));
								alumnoElement.setAttribute("nombre", rsAlumnos.getString("nombre"));
								alumnoElement.setAttribute("apellidos", rsAlumnos.getString("apellidos"));
								alumnoElement.setAttribute("genero", rsAlumnos.getString("genero"));
								alumnoElement.setAttribute("fechaNacimiento", rsAlumnos.getString("fechaNacimiento"));
								alumnoElement.setAttribute("ciclo", rsAlumnos.getString("ciclo"));
								alumnoElement.setAttribute("curso", rsAlumnos.getString("curso"));

								grupoElement.appendChild(alumnoElement);
							}
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
			}
		} catch (ParserConfigurationException | TransformerException e) {
			loggerExcepciones.error("Error al generar el archivo XML: {}", e.getMessage(), e);
			System.out.println("Error al generar el archivo XML: " + e.getMessage());
		} catch (SQLException e) {
			loggerExcepciones.error("Error al consultar la base de datos: {}", e.getMessage(), e);
			System.out.println("Error al consultar la base de datos: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Lee un archivo XML que contiene información sobre grupos y alumnos, y guarda
	 * los datos en las tablas correspondientes de la base de datos.
	 *
	 * @param rutaArchivo Ruta del archivo XML a procesar.
	 * @return true si los datos fueron procesados e insertados correctamente, false
	 *         en caso de error.
	 */

	public boolean leerYGuardarGruposXML(String rutaArchivo) {
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

			String sqlVerificarGrupo = "SELECT numeroGrupo FROM grupos WHERE nombreGrupo = ?";
			String sqlInsertarGrupo = "INSERT INTO grupos (nombreGrupo) VALUES (?)";
			String sqlInsertarAlumno = """
					INSERT INTO alumnos (nombre, apellidos, genero, fechaNacimiento, ciclo, curso, numeroGrupo)
					VALUES (?, ?, ?, ?, ?, ?, ?)
					""";

			try (Connection conexion = PoolConexiones.getConnection();
					PreparedStatement consultaVerificarGrupo = conexion.prepareStatement(sqlVerificarGrupo);
					PreparedStatement consultaInsertarGrupo = conexion.prepareStatement(sqlInsertarGrupo,
							Statement.RETURN_GENERATED_KEYS);
					PreparedStatement consultaInsertarAlumno = conexion.prepareStatement(sqlInsertarAlumno)) {

				for (int i = 0; i < listaGrupos.getLength(); i++) {
					Node nodoGrupo = listaGrupos.item(i);

					if (nodoGrupo.getNodeType() == Node.ELEMENT_NODE) {
						Element elementoGrupo = (Element) nodoGrupo;
						String nombreGrupo = elementoGrupo.getAttribute("nombreGrupo").trim();

						if (!nombreGrupo.isEmpty()) {
							int numeroGrupo = -1;

							// Verificar si el grupo ya existe en la base de datos
							consultaVerificarGrupo.setString(1, nombreGrupo);
							try (ResultSet resultado = consultaVerificarGrupo.executeQuery()) {
								if (resultado.next()) {
									numeroGrupo = resultado.getInt("numeroGrupo"); // Obtener el número del grupo
																					// existente
								}
							}

							// Si el grupo no existe, lo insertamos
							if (numeroGrupo == -1) {
								consultaInsertarGrupo.setString(1, nombreGrupo);
								consultaInsertarGrupo.executeUpdate();

								try (ResultSet clavesGeneradas = consultaInsertarGrupo.getGeneratedKeys()) {
									if (clavesGeneradas.next()) {
										numeroGrupo = clavesGeneradas.getInt(1);
									}
								}
							}

							// Insertar alumnos del grupo
							NodeList listaAlumnos = elementoGrupo.getElementsByTagName("alumno");
							for (int j = 0; j < listaAlumnos.getLength(); j++) {
								Node nodoAlumno = listaAlumnos.item(j);

								if (nodoAlumno.getNodeType() == Node.ELEMENT_NODE) {
									Element elementoAlumno = (Element) nodoAlumno;

									consultaInsertarAlumno.setString(1, elementoAlumno.getAttribute("nombre"));
									consultaInsertarAlumno.setString(2, elementoAlumno.getAttribute("apellidos"));
									consultaInsertarAlumno.setString(3, elementoAlumno.getAttribute("genero"));
									consultaInsertarAlumno.setDate(4,
											java.sql.Date.valueOf(elementoAlumno.getAttribute("fechaNacimiento")));
									consultaInsertarAlumno.setString(5, elementoAlumno.getAttribute("ciclo"));
									consultaInsertarAlumno.setString(6, elementoAlumno.getAttribute("curso"));
									consultaInsertarAlumno.setInt(7, numeroGrupo);

									consultaInsertarAlumno.executeUpdate();

									loggerGeneral.info("Alumno insertado: {} {}", elementoAlumno.getAttribute("nombre"),
											elementoAlumno.getAttribute("apellidos"));
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
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			loggerExcepciones.error("Error al procesar el archivo XML: {}", e.getMessage(), e);
			System.err.println("Error al procesar el archivo XML: " + e.getMessage());
		} catch (SQLException e) {
			loggerExcepciones.error("Error al insertar datos en la base de datos: {}", e.getMessage(), e);
			System.err.println("Error al insertar datos en la base de datos: " + e.getMessage());
		}

		return false;
	}

	/**
	 * Muestra todos los alumnos del grupo seleccionado por el usuario.
	 */
	@Override
	public void mostrarAlumnosPorGrupo() {
		// Mostrar todos los grupos
		if (!mostrarTodosLosGrupos()) {
			System.out.println("No hay grupos disponibles para mostrar.");
			return;
		}

		System.out.println("Introduce el nombre del grupo del que quieres ver los alumnos:");
		String nombreGrupo = sc.nextLine().trim().toUpperCase();

		// Obtener el número del grupo
		int numeroGrupo = obtenerNumeroGrupoPorNombre(nombreGrupo);
		if (numeroGrupo == -1) {
			System.out.println("El grupo especificado no existe. Inténtalo de nuevo.");
			return;
		}

		String sql = """
				    SELECT a.nia, a.nombre, a.apellidos, a.genero, a.fechaNacimiento,
				           a.ciclo, a.curso, g.nombreGrupo
				    FROM alumnos a
				    JOIN grupos g ON a.numeroGrupo = g.numeroGrupo
				    WHERE g.numeroGrupo = ?
				    ORDER BY a.nia
				""";

		try (Connection conexion = PoolConexiones.getConnection();
				PreparedStatement sentencia = conexion.prepareStatement(sql)) {

			sentencia.setInt(1, numeroGrupo);

			try (ResultSet resultado = sentencia.executeQuery()) {
				if (!resultado.isBeforeFirst()) {
					System.out.println("No hay alumnos registrados en este grupo.");
					return;
				}

				System.out.println("Alumnos del grupo '" + nombreGrupo + "':");
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
							-------------------------\n
							""", resultado.getInt("nia"), resultado.getString("nombre"),
							resultado.getString("apellidos"), resultado.getString("genero"),
							resultado.getDate("fechaNacimiento"), resultado.getString("ciclo"),
							resultado.getString("curso"), resultado.getString("nombreGrupo"));
				}
			}
		} catch (SQLException e) {
			loggerExcepciones.error("Error al mostrar los alumnos del grupo '{}': {}", nombreGrupo, e.getMessage(), e);
			System.out.println("Se produjo un error al intentar mostrar los alumnos. Revisa los logs.");
		}
	}
	
	/**
	 * Muestra solo los NIA y nombres de los alumnos, sin interacción adicional.
	 * 
	 * @return true si hay alumnos, false si la lista está vacía.
	 */
	public boolean listarNiasYNombresAlumnos() {
	    String sql = "SELECT nia, nombre FROM alumnos ORDER BY nia";

	    try (Connection conexion = PoolConexiones.getConnection();
	         PreparedStatement sentencia = conexion.prepareStatement(sql);
	         ResultSet resultado = sentencia.executeQuery()) {

	        if (!resultado.isBeforeFirst()) {
	            System.out.println("❌ No hay alumnos registrados.");
	            return false;
	        }

	        System.out.println("Lista de alumnos disponibles para cambiar de grupo:");
	        while (resultado.next()) {
	            System.out.printf("NIA: %d, Nombre: %s%n", resultado.getInt("nia"), resultado.getString("nombre"));
	        }

	        return true;
	    } catch (SQLException e) {
	        System.out.println("❌ Error al recuperar la lista de alumnos: " + e.getMessage());
	        return false;
	    }
	}

	/**
	 * Cambia el grupo de un alumno seleccionado por el usuario.
	 * 
	 * @return true si el cambio se realizó correctamente, false en caso de error.
	 */
	@Override
	public boolean cambiarGrupoAlumno() {
	    // Mostrar lista de alumnos sin interacción extra
	    if (!listarNiasYNombresAlumnos()) {
	        System.out.println("❌ No hay alumnos disponibles.");
	        return false;
	    }

	    // Solicitar NIA del alumno
	    System.out.println("\nIntroduce el NIA del alumno al que deseas cambiar de grupo:");
	    int niaSeleccionado;
	    try {
	        niaSeleccionado = Integer.parseInt(sc.nextLine().trim());
	    } catch (NumberFormatException e) {
	        System.out.println("❌ El NIA debe ser un número válido.");
	        return false;
	    }

	    // Verificar si el NIA existe
	    String sqlExistencia = "SELECT numeroGrupo FROM alumnos WHERE nia = ?";
	    int grupoActual = -1;

	    try (Connection conexion = PoolConexiones.getConnection();
	         PreparedStatement consulta = conexion.prepareStatement(sqlExistencia)) {

	        consulta.setInt(1, niaSeleccionado);
	        try (ResultSet resultado = consulta.executeQuery()) {
	            if (resultado.next()) {
	                grupoActual = resultado.getInt("numeroGrupo");
	            } else {
	                System.out.println("❌ No se encontró ningún alumno con el NIA proporcionado.");
	                return false;
	            }
	        }
	    } catch (SQLException e) {
	        System.out.println("❌ Error al verificar el grupo actual del alumno: " + e.getMessage());
	        return false;
	    }

	    // Mostrar grupos disponibles
	    System.out.println("\nGrupos disponibles:");
	    if (!mostrarTodosLosGrupos()) {
	        System.out.println("❌ No hay grupos disponibles.");
	        return false;
	    }

	    // Solicitar el nuevo grupo
	    System.out.println("\nIntroduce el nombre del grupo al que deseas cambiar al alumno:");
	    String nuevoGrupo = sc.nextLine().trim().toUpperCase();

	    int numeroGrupo = obtenerNumeroGrupoPorNombre(nuevoGrupo);
	    if (numeroGrupo == -1) {
	        System.out.println("❌ El grupo especificado no existe.");
	        return false;
	    }

	    if (grupoActual == numeroGrupo) {
	        System.out.println("⚠️ El alumno ya pertenece al grupo '" + nuevoGrupo + "'.");
	        return false;
	    }

	    // Actualizar grupo
	    String sqlUpdate = "UPDATE alumnos SET numeroGrupo = ? WHERE nia = ?";
	    try (Connection conexion = PoolConexiones.getConnection();
	         PreparedStatement sentencia = conexion.prepareStatement(sqlUpdate)) {

	        sentencia.setInt(1, numeroGrupo);
	        sentencia.setInt(2, niaSeleccionado);

	        int filasAfectadas = sentencia.executeUpdate();
	        if (filasAfectadas > 0) {
	            System.out.println("✅ El grupo del alumno ha sido cambiado exitosamente.");
	            return true;
	        } else {
	            System.out.println("❌ No se pudo cambiar el grupo del alumno.");
	            return false;
	        }
	    } catch (SQLException e) {
	        System.out.println("❌ Error al cambiar el grupo del alumno: " + e.getMessage());
	        return false;
	    }
	}

	/**
	 * Guarda un grupo específico con toda su información (incluyendo los alumnos)
	 * en un archivo XML. Solicita al usuario el nombre del grupo.
	 * 
	 * @return true si el archivo se guarda correctamente, false si ocurre un error.
	 */
	@Override
	public boolean guardarGrupoEspecificoEnXML() {
		System.out.print("Introduce el nombre del grupo que deseas guardar en fichero XML: ");
		String nombreGrupo = sc.nextLine().trim().toUpperCase();

		// Validar si el grupo existe
		if (!validarNombreGrupo(nombreGrupo)) {
			System.out.println("El grupo '" + nombreGrupo + "' no existe en la base de datos.");
			loggerGeneral.warn("El grupo '{}' no existe en la base de datos.", nombreGrupo);
			return false;
		}

		// Obtener el número del grupo
		int numeroGrupo = obtenerNumeroGrupoPorNombre(nombreGrupo);
		if (numeroGrupo == -1) {
			System.out.println("Error al obtener el número del grupo para: " + nombreGrupo);
			loggerGeneral.error("No se pudo obtener el número del grupo para '{}'.", nombreGrupo);
			return false;
		}

		String nombreArchivo = "grupo_" + nombreGrupo + ".xml";
		File archivoXML = new File(nombreArchivo);

		if (archivoXML.exists()) {
			System.out.print("El archivo " + nombreArchivo + " ya existe. ¿Deseas sobrescribirlo? (S/N): ");
			String respuesta = sc.nextLine().trim().toUpperCase();

			if (!respuesta.equals("S")) {
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

			String consultaGrupo = "SELECT numeroGrupo, nombreGrupo FROM grupos WHERE numeroGrupo = ?";
			String consultaAlumnos = """
					SELECT nia, nombre, apellidos, genero, fechaNacimiento, curso
					FROM alumnos
					WHERE numeroGrupo = ?
					""";

			try (Connection conexion = PoolConexiones.getConnection();
					PreparedStatement stmtGrupo = conexion.prepareStatement(consultaGrupo);
					PreparedStatement stmtAlumnos = conexion.prepareStatement(consultaAlumnos)) {

				stmtGrupo.setInt(1, numeroGrupo);
				try (ResultSet rsGrupo = stmtGrupo.executeQuery()) {
					if (rsGrupo.next()) {
						raizElement.setAttribute("numeroGrupo", String.valueOf(numeroGrupo));
						raizElement.setAttribute("nombreGrupo", rsGrupo.getString("nombreGrupo"));
					} else {
						System.out.println("No se encontró el grupo con el número " + numeroGrupo + ".");
						loggerGeneral.warn("El grupo con número {} no existe en la base de datos.", numeroGrupo);
						return false;
					}
				}

				stmtAlumnos.setInt(1, numeroGrupo);
				try (ResultSet rsAlumnos = stmtAlumnos.executeQuery()) {
					while (rsAlumnos.next()) {
						Element alumnoElement = documentoXML.createElement("alumno");
						alumnoElement.setAttribute("nia", rsAlumnos.getString("nia"));
						alumnoElement.setAttribute("nombre", rsAlumnos.getString("nombre"));
						alumnoElement.setAttribute("apellidos", rsAlumnos.getString("apellidos"));
						alumnoElement.setAttribute("genero", rsAlumnos.getString("genero"));
						alumnoElement.setAttribute("fechaNacimiento", rsAlumnos.getString("fechaNacimiento"));
						alumnoElement.setAttribute("curso", rsAlumnos.getString("curso"));

						raizElement.appendChild(alumnoElement);
					}
				}

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
			}
		} catch (ParserConfigurationException | TransformerException e) {
			loggerExcepciones.error("Error al generar el archivo XML: {}", e.getMessage(), e);
			System.out.println("Error al generar el archivo XML: " + e.getMessage());
		}

		return false;
	}
	



	@Override
	public boolean eliminarAlumnosPorApellidos(String apellidos) {
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
	public void guardarGruposEnFicheroJSON() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean leerGruposDeFicheroJSON() {
		// TODO Auto-generated method stub
		return false;
	}

}
